(ns tfidf.tfidf-test
  (:use midje.sweet)
  (:require [tfidf.tfidf :as tfidf]))

(def text1 '("This" "is" "a" "silli" "english" "text" "test" "which" "is" "onli" "here" "for" "test" "pars"))
(def text2 '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test"))
(def text3 '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test"))
(def textcoll (list text1 text2 text3))

(facts "Compute term frequencies"
       (fact "Compute term-frequency unnormalized"
             (tfidf/tf text1 :normalize false) => (frequencies text1))
       (fact "Compute normalized term-frequency for a text"
             (tfidf/tf text1) => (contains {"is" 1.0 "This" 0.7})))

(fact "Compute inverted document frequencies for a collection"
      (tfidf/idf textcoll) => (contains {"onli" 0.0,
                                         "And" 0.6931471805599453,
                                         "which" 0.2876820724517807,
                                         "stupid" 0.6931471805599453,
                                         "pars" 0.6931471805599453,
                                         "Another" 0.6931471805599453,
                                         "is" 0.2876820724517807,
                                         "just" 0.6931471805599453,
                                         "for" 0.0,
                                         "text" 0.0,
                                         "a" 0.6931471805599453,
                                         "here" 0.6931471805599453,
                                         "other" 0.6931471805599453,
                                         "some" 0.6931471805599453,
                                         "english" 0.0,
                                         "silli" 0.6931471805599453,
                                         "This" 0.6931471805599453,
                                         "test" 0.0}))

(facts "Computing the tf-idf values for a text collection"
       (fact "Compute the sequence of tfidf values for a collection"
             (tfidf/tfidf textcoll) =>
             [["onli" "And" "which" "stupid" "pars" "Another" "is" "just" "for" "text"
               "a" "here" "other" "some" "english" "silli" "This" "test"]
              '((0.0 0.0 0.12326388133897685 0.0 0.33398487830376367 0.0 0.17609125905568124
                 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0
                 0.33398487830376367 0.33398487830376367 0.0) 
                (0.0 0.0 0.12326388133897685 0.33398487830376367 0.0 0.33398487830376367
                 0.12326388133897685 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0)
                (0.0 0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.0 0.0 
                 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0 0.0))])

       (fact "Ensure sequence of terms matches"
             ;; Note: this example from Wikipedia gives different results
             ;; because we're using a different tf computation
             (let [text1 '("this" "is" "a" "sample" "a")
                   text2 '("this" "another" "is" "example" "another" "example" "example")]
               (tfidf/tfidf (list text1 text2)) =>
               [["this" "is" "a" "sample" "another" "example"]
                '((0.0 0.0 0.3010299956639812 0.21072099696478683 0.0 0.0)
                  (0.0 0.0 0.0 0.0 0.24082399653118497 0.3010299956639812))])))

(facts "Generate TF data for some docs"
       (fact "Convert a tfmap to a vector of tf values per term"
             (tfidf/tfmap-to-termvector {"foo" 1 "bar" 2 "baz" 3}
                                  ["abc" "foo" "def" "baz" "ghi" "bar"])
             => [0 1 0 3 0 2])
       (fact "We can convert a document collection to a set of rows of tf values"
             (tfidf/tf-from-docs [["foo" "bar" "baz" "bar" "foo"] ["abc" "abc" "abc" "def"]])
             => [["foo" "bar" "baz" "abc" "def"] ; vocabulary
                 [[1.0 1.0 0.7 0 0]              ; adapted vector for doc 1
                  [0 0 0 1.0 0.6]]]))            ; adapted vector for doc 2
