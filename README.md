# tfidf

This is a small Clojure library that provides code to compute [tf-idf](https://en.wikipedia.org/wiki/Tf%E2%80%93idf) values for texts. 

This library also provides code to compute term-frequencies and inverse document frequencies. Several alternative implementations are provided:

* straight-forward one-pass computation
* transducer-based versions of frequencies, idf and tf-idf
* a protocol-based one-document-at-a-time tf-idf version

## Usage overview

For the one-pass computation, simply call `tfidf` on a text collection. The following code assume `textcoll` holds a collection of texts, i.e. sequences of words.

	    user> (def text1 '("This" "is" "a" "silli" "english" "text" "test" "which" "is" "onli" "here" "for" "test" "pars"))
	    user> (def text2 '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test"))
	    user> (def text3 '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test"))
	    user> (def textcoll (list text1 text2 text3))

 Then the result of calling `tfidf` on this text collection is a tuple (sequence) of all terms and the tf-idf values per document, ordered per term vector.

	    user> (require '[tfidf.tfidf :as tfidf])
	    user> (tfidf/tfidf textcoll)
	    [["onli" "And" "which" "stupid" "pars" "Another" "is" "just" "for" "text" "a" "here" "other" "some" "english" "silli" "This" "test"]
	    '((0.0 0.0 0.12326388133897685 0.0 0.33398487830376367 0.0 0.17609125905568124 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0)
	      (0.0 0.0 0.12326388133897685 0.33398487830376367 0.0 0.33398487830376367 0.12326388133897685 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0)
	      (0.0 0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0 0.0))])

The transducer version to compute tf-idf values expects that a prior computation of tf and idf values, like so:

	    user> (require '[tfidf.xf :as xf])
	    user> (into {} (comp (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) (map xf/tf textcoll))
        {:terms {"And" {:doccount 1, :idf 0.47712125471966244},
		         "Another" {:doccount 1, :idf 0.47712125471966244},
				 "This" {:doccount 1, :idf 0.47712125471966244},
				 "a" {:doccount 1, :idf 0.47712125471966244},
				 "english" {:doccount 3, :idf 0.0},
				 "for" {:doccount 3, :idf 0.0},
				 "here" {:doccount 1, :idf 0.47712125471966244},
				 "is" {:doccount 2, :idf 0.17609125905568124},
				 "just" {:doccount 1, :idf 0.47712125471966244},
				 "onli" {:doccount 3, :idf 0.0},
				 "other" {:doccount 1, :idf 0.47712125471966244},
				 "pars" {:doccount 1, :idf 0.47712125471966244},
				 "silli" {:doccount 1, :idf 0.47712125471966244},
				 "some" {:doccount 1, :idf 0.47712125471966244},
				 "stupid" {:doccount 1, :idf 0.47712125471966244},
				 "test" {:doccount 3, :idf 0.0},
				 "text" {:doccount 3, :idf 0.0}, "which" {:doccount 2, :idf 0.17609125905568124}}, 
	      :tfs ((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
		        (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
		        (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7)),
		  :tfidfs ((0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.33398487830376367 0.0 0.0 0.33398487830376367 0.0 0.0 0.0 0.0) 
	               (0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.33398487830376367 0.17609125905568124 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0 0.0 0.12326388133897685) 
			  	   (0.0 0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.12326388133897685 0.0 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.0 0.12326388133897685))}

This returns a map of terms, including the document count and idf values, the term frequencies per document and the tf-idf values (ordered as per the `sorted-map` of terms). Note that the input to `idf-xf` and `tfidf-xf` already expects `sorted-map`s -- basically, each step expects more data as input from the previous one.

## Term frequencies

One fundamental step required to compute tf-idf values is computing term frequencies. With [frequencies](http://clojuredocs.org/clojure.core/frequencies) Clojure provides an out of box function for this.

This library provides a transducer version in `freq`, which works exactly like `frequencies` when called on a collection.

	    user> (require '[tfidf.freq :refer [freq]])
	    user> (let [testdata [:a :b :c :a :a :b]]
                   (= (freq testdata) (frequencies testdata)))
        true

But you can also get an idea of all the intermediate steps when using `conj` as the reducing function:

	    user> (let [testdata [:a :b :c :a :a :b]]
                 (into [] (freq) testdata))
  	    [{:a 1}
		 {:a 1, :b 1}
		 {:a 1, :b 1, :c 1}
		 {:a 2, :b 1, :c 1}
		 {:a 3, :b 1, :c 1}
		 {:a 3, :b 2, :c 1}]

In the context of tf-idf computation, the collection is usually a single document, i.e. it is a collection of words in the text. Term frequencies are usually [normalized](https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency_2). So the `tf` function computes the [augmented term frequency](http://nlp.stanford.edu/IR-book/html/htmledition/maximum-tf-normalization-1.html) when the `:normalize` keyword is set to `true` (the default). This works both for the normal and the transducer versions:

	    user> (let [testdata '("This" "is" "a" "silli" "english" "text" "test"
                               "which" "is" "onli" "here" "for" "test" "pars")]
                 (tfidf/tf testdata))
		{"onli" 0.7, "which" 0.7, "pars" 0.7, "is" 1.0, "for" 0.7, "text" 0.7,
         "a" 0.7, "here" 0.7, "english" 0.7, "silli" 0.7, "This" 0.7, "test" 1.0}
	    user> (let [testdata '("This" "is" "a" "silli" "english" "text" "test"
                               "which" "is" "onli" "here" "for" "test" "pars")]
                 (xf/tf testdata))
		{"onli" 0.7, "which" 0.7, "pars" 0.7, "is" 1.0, "for" 0.7, "text" 0.7,
         "a" 0.7, "here" 0.7, "english" 0.7, "silli" 0.7, "This" 0.7, "test" 1.0}
	    user> (let [testdata '("This" "is" "a" "silli" "english" "text" "test"
                               "which" "is" "onli" "here" "for" "test" "pars")]
                 (into {} xf/norm-tf-xf testdata))
		{"onli" 0.7, "which" 0.7, "pars" 0.7, "is" 1.0, "for" 0.7, "text" 0.7,
         "a" 0.7, "here" 0.7, "english" 0.7, "silli" 0.7, "This" 0.7, "test" 1.0}
	 
`tf` is just a convenience function allowing frequency computation with or without normalization (via the `:normalize` keyword). The transducer version (`xf/tf` or `norm-tf-xf`) simply calls either `freq` or the composition of the `freq` and `normalize-tf-xf` transducers. In other words:
	 
	    user> (let [testdata '("This" "is" "a" "silli" "english" "text" "test"
                               "which" "is" "onli" "here" "for" "test" "pars")]
                 (= (xf/tf testdata :normalize true)
                    (into {} (comp (freq) (xf/normalize-tf-xf)) testdata)))
	    true
	    user> (let [testdata '("This" "is" "a" "silli" "english" "text" "test"
                               "which" "is" "onli" "here" "for" "test" "pars")]
                 (= (xf/tf testdata :normalize false)
                    (into {} (freq) testdata)))
	    true

`tf-from-docs` and `tf-from-docs-xf` can be used to compute term frequencies on a collection of texts. The former will return a sequence of terms of all documents and a sequence of tf values per document.
	
	    user> (tfidf/tf-from-docs textcoll)
	    [["onli" "And" "which" "stupid" "pars" "Another" "is" "just" "for" "text" "a"
		  "here" "other" "some" "english" "silli" "This" "test"]
		 ([0.7 0 0.7 0 0.7 0 1.0 0 0.7 0.7 0.7 0.7 0 0 0.7 0.7 0.7 1.0]
		  [0.7 0 0.7 0.7 0 0.7 0.7 0 0.7 0.7 0 0 0 0 0.7 0 0 1.0]
		  [0.7 0.7 0 0 0 0 0 0.7 0.7 0.7 0 0 0.7 0.7 0.7 0 0 1.0])]

In the result the sequence of values per document is ordered per term, i.e. the first 0.7 value is for the term "onli" and so on.

The transducer version returns a map with `:terms` and `:tfs` term-frequencies per document. You can either call it with a list of documents (sequence of word sequences), in which case it will `map tf` internally over the sequence, or use it as a (stateful) transducer:

	    user> (xf/tf-from-docs-xf textcoll)
        {:terms {"And" 1, "Another" 1, "This" 1, "a" 1, "english" 3, "for" 3, "here" 1,
	     "is" 2, "just" 1, "onli" 3, "other" 1, "pars" 1, "silli" 1, "some" 1,
	     "stupid" 1, "test" 3, "text" 3, "which" 2},
		 :tfs ((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
		       (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
               (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))}
	    user> (into [] (xf/tf-from-docs-xf) (map xf/tf textcoll))
        [{:terms {"This" 1, "a" 1, "english" 1, "for" 1, "here" 1, "is" 1, "onli" 1, 
                  "pars" 1, "silli" 1, "test" 1, "text" 1, "which" 1},
	      :tfs ((0.7 0.7 0.7 0.7 0.7 1.0 0.7 0.7 0.7 1.0 0.7 0.7))}
	     {:terms {"Another" 1, "This" 1, "a" 1, "english" 2, "for" 2, "here" 1, "is" 2,
	              "onli" 2, "pars" 1, "silli" 1, "stupid" 1, "test" 2, "text" 2, "which" 2},
	      :tfs ((0.7 0 0 0.7 0.7 0 0.7 0.7 0 0 0.7 1.0 0.7 0.7) 
	            (0 0.7 0.7 0.7 0.7 0.7 1.0 0.7 0.7 0.7 0 1.0 0.7 0.7))}
	     {:terms {"And" 1, "Another" 1, "This" 1, "a" 1, "english" 3, "for" 3, "here" 1, 
                  "is" 2, "just" 1, "onli" 3, "other" 1, "pars" 1, "silli" 1, "some" 1, 
	              "stupid" 1, "test" 3, "text" 3, "which" 2},
	      :tfs ((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
	            (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
	            (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))}]

In this example I used `conj` as the reducing function, so we can see how the `tf-from-docs-xf` builds up the result sentence by sentence. The result is made up of maps of `:terms`, counting the number of documents a term has appeared in and `:tfs` values per seen document and we can see how the terms are changing with each new document. Again, the resulting sequences of `:tfs` values are ordered in accordance with the terms. Note that value for `:terms` is a [`sorted-map`](https://clojuredocs.org/clojure.core/sorted-map).

## Inverse document frequencies

`idf` and its transducer variant `idf-xf` compute the [inverse document frequency](https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Inverse_document_frequency) of terms in a collection of texts, which is a measure of the specifity of a term for a given document collection. The non-transducer version can be directly called on a text collection:

	    user> (tfidf/idf textcoll)
		{"onli" 0.0, "And" 0.6931471805599453, "which" 0.2876820724517807, "stupid" 0.6931471805599453,
		 "pars" 0.6931471805599453, "Another" 0.6931471805599453, "is" 0.2876820724517807, "just" 0.6931471805599453,
		 "for" 0.0, "text" 0.0, "a" 0.6931471805599453, "here" 0.6931471805599453, "other" 0.6931471805599453,
		 "some" 0.6931471805599453, "english" 0.0, "silli" 0.6931471805599453, "This" 0.6931471805599453, "test" 0.0}

The transducer version expects an input as produced from the term frequency transducer, i.e., a map of terms (a `sorted-map` with their respective occurence frequency) and a sequence of tfs value per document.
	
		user> (into {} (comp (xf/tf-from-docs-xf) (xf/idf-xf)) (map xf/tf textcoll))
		{:terms {"And" {:doccount 1, :idf 0.47712125471966244},
				 "Another" {:doccount 1, :idf 0.47712125471966244},
				 "This" {:doccount 1, :idf 0.47712125471966244},
				 "a" {:doccount 1, :idf 0.47712125471966244},
				 "english" {:doccount 3, :idf 0.0},
				 "for" {:doccount 3, :idf 0.0},
				 "here" {:doccount 1, :idf 0.47712125471966244},
				 "is" {:doccount 2, :idf 0.17609125905568124},
				 "just" {:doccount 1, :idf 0.47712125471966244},
				 "onli" {:doccount 3, :idf 0.0},
				 "other" {:doccount 1, :idf 0.47712125471966244},
				 "pars" {:doccount 1, :idf 0.47712125471966244},
				 "silli" {:doccount 1, :idf 0.47712125471966244},
				 "some" {:doccount 1, :idf 0.47712125471966244},
				 "stupid" {:doccount 1, :idf 0.47712125471966244},
				 "test" {:doccount 3, :idf 0.0},
				 "text" {:doccount 3, :idf 0.0},
				 "which" {:doccount 2, :idf 0.17609125905568124}},
		:tfs ((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
			  (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
			  (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))}

Note that `terms` is again a `sorted-map` of terms, the number of documents the term occured in and the idf value of the term. We can see here that "for", "english", "onli", "test" and "text" are not very specific: they appear in each sentence, so their idf values are zero.
