Let's try some simple benchmarking on the three different approaches. `tfidf.bench` contains
three methods:

- `tfidf-at-once-bench` expects a text collection and calls the one-pass tf-idf computation (`tfidf/tfidf`)
- `tfidf-incremental-bench` expects a document collection and calls the incremental tf-idf computation (`protocol/tfidf-from-docs`)
- `tfidf-xf-bench` expects a text collection and calls the transducer version (`xf/tfidf-xf`)


Calling `bench/setup-testcolls` we can generate two global vars `textcoll` and `doccoll` which will contain sequences of texts and documents respectively with the specified number of texts (default 200) and words per text (default 1000). Let's start with a small collection of 100 texts first.


    tfidf.bench> (setup-testcolls :nrtexts 100)
    #'tfidf.bench/doccoll


Let's start with the one-pass version, which internally uses `pmap`:

	tfidf.bench> (tfidf-at-once-bench textcoll)
	tfidf done.
	"Elapsed time: 122.702942 msecs"
	nil

This is pretty fast. Now, in comparison the incremental version:

	tfidf.bench> (tfidf-incremental-bench doccoll)
	incremental tfidf done.
	"Elapsed time: 8438.600434 msecs"
	nil

And here is the transducer version:

	tfidf.bench> (tfidf-xf-frequencies-bench textcoll)
	tfidf done.
	"Elapsed time: 17099.97106 msecs"
	nil

Wow, this is way slower than the one-time-pass or incremental version.

Now, let's use `pmap` instead of `map` everywhere within the `xf` code:

	tfidf.bench> (tfidf-xf-frequencies-pmap-bench textcoll)
	tfidf done.
	"Elapsed time: 17164.466022 msecs"
	nil

This doesn't buy us anything which is also telling for the additional coordination overhead.

Actually, as it turns out, this is because of a naive use of `into` on the transducer. If we actually use the transducer with `pipeline`, then the results look rather different:

	tfidf.bench> (tfidf-ppl-bench textcoll :f frequencies)
	tfidf done.
	"Elapsed time: 56.618998 msecs"
	nil

This is using a parallelization of 10 on the `pipeline` call.

We can dig a little bit into where the time is spent by timing the different parts:

	tfidf.bench> (tfidf-xf-parts-bench textcoll :f frequencies)
	f done.
	"Elapsed time: 51.929494 msecs"
	tf-from-docs done.
	"Elapsed time: 8809.338147 msecs"
	idf done.
	"Elapsed time: 16768.259933 msecs"
	tfidf done.
	"Elapsed time: 16831.959518 msecs"
	nil

So apparently building up the initial map of tf values for all texts is accounting for half of the spent time and building up the idf takes the second half. The tf-idf computation operating on the tf and iddf values is instead practically for free. Let's see how the different parts scale by doubling the number of texts:

	tfidf.bench> (setup-testcolls :nrtexts 200)
	#'tfidf.bench/doccoll
	tfidf.bench> (tfidf-xf-parts-bench textcoll :f frequencies)
	f done.
	"Elapsed time: 108.082195 msecs"
	tf-from-docs done.
	"Elapsed time: 27606.655964 msecs"
	idf done.
	"Elapsed time: 66512.28172 msecs"
	tfidf done.
	"Elapsed time: 66594.54985 msecs"
	nil

The `frequencies` number just double, as expected. However, the numbers for transforming these to a sequence of common position based tf values is taking more than three times as long and the idf computation roughly five times as long. Again, the tf-idf computation doesn't add much.

Finally, a comparison of the built-in `frequencies` with my `xf` version:

    user> (count (mapcat identity benchcoll))
    499819
	user> (def randomwl (mapcat identity benchcoll))
	#'user/randomwl
	user> (time (do (frequencies randomwl) "done"))
	"Elapsed time: 315.262751 msecs"
	"done"
	user> (time (do (tfidf.freq/freq randomwl) "done"))
	"Elapsed time: 1.104315515898E7 msecs"
	"done"

*Ouch*
