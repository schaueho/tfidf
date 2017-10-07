`tfidf-at-once-bench` measures `tfidf/tfidf` which expects a complete set of texts. `tfidef-incremental-bench` instead uses `TfIdfprotocol/add-doc` to add documents incrementally.

Let's start benchmarking the former with a collection of 100 texts that are at a maximum 1000 words long:

    tfidf.bench> (setup-testcolls :nrtexts 100)
    #'tfidf.bench/doccoll
    tfidf.bench> (count textcoll)
    100
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 20213.376417 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 1333.464334 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 1336.887014 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 1328.537407 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 1328.702489 msecs"
    nil

This is very strange: we see a quite time consuming first run, all laters are way faster. This looks as if there would be some memoization / caching going on, but that is not the case. It's probably a matter of lazy sequences.

    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 5372.5338 msecs"
    nil
    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 5351.701402 msecs"
    nil
    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 5418.182709 msecs"
    nil

This is as expected: the incremental approach is quite a bit slower, as it's re-computing the idf and tfidf matrices for each new document. Now let's increase the number of texts to understand how the approaches scale:

    tfidf.bench> (setup-testcolls :nrtexts 1000)
    #'tfidf.bench/doccoll
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 209650.637658 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 18521.180892 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 19045.841541 msecs"
    nil

Again, we see the strange slow run on initial computation. Otherwise, going for a magnitude of more texts results in (somewhat more than) a magnitude of slower computation.

    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 149994.626034 msecs"
    nil
    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 150218.209884 msecs"
    nil
    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 148443.458266 msecs"
    nil
    tfidf.bench> (tfidf-incremental-bench doccoll)
    incremental tfidf done.
    "Elapsed time: 147141.349427 msecs"
    nil

Again, the incremental approach is way slower. However, the time required is not only a magnitude slower, but roughly 3*10 times.

After adding the transducer version, let's compare the speed again.

    tfidf.bench> (setup-testcolls :nrtexts 100)
    #'tfidf.bench/doccoll
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 27472.455627 msecs"
    nil
    tfidf.bench> (tfidf-at-once-bench textcoll)
    tfidf done.
    "Elapsed time: 27230.122897 msecs"
    nil

For some reason, the time is much longer (probably code changes?).
This is the transducer time: 

    tfidf.bench> (tfidf-xf-bench textcoll)
    tfidf done.
    "Elapsed time: 63796.287953 msecs"
    nil
    tfidf.bench> (tfidf-xf-bench textcoll)
    tfidf done.
    "Elapsed time: 42080.224396 msecs"
    nil
    tfidf.bench> (tfidf-xf-bench textcoll)
    tfidf done.
    "Elapsed time: 41415.599763 msecs"
    nil

Again, we see a slower time on first run. Probably, we're seeing the (text) collection being build up. And, as expected, as the idf and tfidf values are re-computed for each new text, this is a lot slower, but still a lot faster than the protocol version.

Now, let's double the number of texts:

    tfidf.bench> (setup-testcolls :nrtexts 200)
    #'tfidf.bench/doccoll

Version with map:

	tfidf.bench> (tfidf-at-once-bench textcoll)
	tfidf done.
	"Elapsed time: 68793.530486 msecs"
	nil

	tfidf.bench> (tfidf-xf-bench textcoll)
	tfidf done.
	"Elapsed time: 126259.278491 msecs"
	nil

Take away: doubling the text number tripples the run time.
