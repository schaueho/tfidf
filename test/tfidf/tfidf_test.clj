(ns bmtagger.tfidf-test
  (:use midje.sweet)
  (:require [bmtagger.tfidf :as tfidf]))

(def text1 '("This" "is" "a" "silli" "english" "text" "test" "which" "is" "onli" "here" "for" "test" "pars"))
(def text2 '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test"))
(def text3 '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test"))
(def textcoll (list text1 text2 text3))

(facts "Computing tf-idf"
       (facts "Compute term-frequency"
              (frequencies text1) => (contains {"is" 2 "This" 1}))
       (facts "Compute normalized term-frequency for a text"
              (tfidf/tf text1) => (contains {"is" 1 "This" 1/2}))
       (facts "Compute inverted document frequencies for a collection"
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
                                                 "test" 0.0})))

