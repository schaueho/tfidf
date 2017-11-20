(ns tfidf.xf-test
  (:use midje.sweet)
  (:require [tfidf.tfidf :as tfidf]
            [tfidf.xf :as xf]))

(def text1 '("This" "is" "a" "silli" "english" "text" "test" "which" "is" "onli" "here" "for" "test" "pars"))
(def text2 '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test"))
(def text3 '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test"))
(def textcoll (list text1 text2 text3))

(facts "Normalize a frequency map"
       (fact "We can call the normalization with a normal map."
             (xf/normalize-tf-xf {:b 2, :a 3, :c 1}) => {:b 0.8, :a 1.0, :c 0.6})
       (fact "We can call the normalization with a sequence of maps, but it will only return the normalization of the last one."
             (xf/normalize-tf-xf [{:b 1}{:b 2, :a 3, :c 1}]) => {:b 0.8, :a 1.0, :c 0.6})
       (fact "Normalization can be used as a transducer"
             (into {} (xf/normalize-tf-xf) [{:b 1}{:b 2, :a 3, :c 1}]) => {:b 0.8, :a 1.0, :c 0.6}
             (into [] (xf/normalize-tf-xf) [{:b 1}{:b 2, :a 3, :c 1}]) =>
             [{:b 1.0} {:b 0.8, :a 1.0, :c 0.6}]))

(facts "Computing term frequencies"
       (fact "Compute term-frequency unnormalized"
             (xf/tf text1 :normalize false) => (frequencies text1))
       (fact "Compute normalized term-frequency for a text"
             (xf/tf text1) => (contains {"is" 1.0 "This" 0.7}))
       (fact "This is the same as in the old tf computation"
             (xf/tf text1) => (tfidf/tf text1))
       (fact "Compute normalized term-frequency for a text using the transducer"
             (into {} xf/norm-tf-xf text1) => (contains {"is" 1.0 "This" 0.7})))

(facts "Test transducer version to compile term frequencies from documents"
      (fact "We can convert a document collection to a set of rows of tf values"
            (xf/tf-from-docs-xf textcoll) =>
            {:terms {"And" 1, "Another" 1, "This" 1, "a" 1, "english" 3, "for" 3, "here" 1,
                     "is" 2, "just" 1, "onli" 3, "other" 1, "pars" 1, "silli" 1, "some" 1,
                     "stupid" 1, "test" 3, "text" 3, "which" 2},
             :tfs '((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
                    (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
                    (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))})
      (fact "This conversion gives the same data as the non-xf version"
            (let [olddata (tfidf/tf-from-docs textcoll)
                  newdata (xf/tf-from-docs-xf (map tfidf/tf textcoll))
                  oldtfs (dorun (map #(zipmap (first olddata) %) (second olddata)))
                  newtfs (dorun (map #(zipmap (:terms newdata) %) (:tfs newdata)))]
              (= oldtfs newtfs)) => true))

(facts "Test transducer version to compile idf documents"
       (let [testtfdata [{:terms (into (sorted-map) ; !sorted map expected!
                                  {"And" 1, "Another" 1, "This" 1, "a" 1, "english" 3, "for" 3,
                                  "here" 1, "is" 2, "just" 1, "onli" 3, "other" 1, "pars" 1,
                                  "silli" 1, "some" 1, "stupid" 1, "test" 3, "text" 3, "which" 2}),
                          :tfs '((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
                                 (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
                                 (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))}]
             expecteddata {:tfs (:tfs (first testtfdata))
                           :terms {"And" {:doccount 1, :idf 0.47712125471966244},
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
                                   "which" {:doccount 2, :idf 0.17609125905568124}}}]
         (fact "idf-xf applies to a map of terms/doccounts and tf values per doc"
               (xf/idf-xf testtfdata) => expecteddata)
         (fact :integration "Functional test of tf, tf-docs and idf"
               (into {} (comp (map xf/tf) (xf/tf-from-docs-xf) (xf/idf-xf)) textcoll)
               => expecteddata)))

(facts "Test transducer version to compile tfidf data"
       (let [testidfdata {:terms (into (sorted-map) ; !sorted map expected!
                                  {"And" {:doccount 1, :idf 0.47712125471966244},
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
                                   "which" {:doccount 2, :idf 0.17609125905568124}})
                          :tfs '((0.7 0 0 0 0.7 0.7 0 0 0.7 0.7 0.7 0 0 0.7 0 1.0 0.7 0)
                                 (0 0 0.7 0.7 0.7 0.7 0.7 1.0 0 0.7 0 0.7 0.7 0 0 1.0 0.7 0.7)
                                 (0 0.7 0 0 0.7 0.7 0 0.7 0 0.7 0 0 0 0 0.7 1.0 0.7 0.7))}
             expecteddata {:terms (:terms testidfdata)
                           :tfs (:tfs testidfdata)
                           :tfidfs '((0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.33398487830376367 0.0 0.0 0.33398487830376367 0.0 0.0 0.0 0.0)
(0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.33398487830376367 0.17609125905568124 0.0 0.0 0.0 0.33398487830376367 0.33398487830376367 0.0 0.0 0.0 0.0 0.12326388133897685)
(0.0 0.33398487830376367 0.0 0.0 0.0 0.0 0.0 0.12326388133897685 0.0 0.0 0.0 0.0 0.0 0.0 0.33398487830376367 0.0 0.0 0.12326388133897685))}]
         (fact "tfidf-xf applies to a map of terms/doccounts/idfs and tf values per doc"
               (xf/tfidf-xf [testidfdata]) => expecteddata)
         (fact :integration "Functional test of tf, tf-docs and idf"
               (into {} (comp (map xf/tf) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) textcoll)
               => expecteddata)))
