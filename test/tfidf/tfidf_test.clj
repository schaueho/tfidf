(ns bmtagger.tfidf-test
  (:use midje.sweet)
  (:require [bmtagger.tfidf :as tfidf]))

(def text1 '("This" "is" "a" "silli" "english" "text" "test" "which" "is" "onli" "here" "for" "test" "pars"))
(def text2 '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test"))
(def text3 '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test"))
(def textcoll (list text1 text2 text3))

(facts "Computing tf-idf"
       (fact "Compute term-frequency"
             (frequencies text1) => (contains {"is" 2 "This" 1}))
       (fact "Compute normalized term-frequency for a text"
             (tfidf/tf text1) => (contains {"is" 1 "This" 1/2}))
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
       (fact "Compute the sequence of tfidf values for a collection"
             (tfidf/tfidf textcoll) => '((0.0 0.0 0.14384103622589034 0.0 0.34657359027997264 0.0 0.2876820724517807 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0) (0.0 0.0 0.14384103622589034 0.34657359027997264 0.0 0.34657359027997264 0.14384103622589034 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0) (0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.0))))



             ; '((0.0 0.14384103622589034 0.34657359027997264 0.2876820724517807 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.34657359027997264 0.34657359027997264 0.0) (0.0 0.14384103622589034 0.34657359027997264 0.34657359027997264 0.14384103622589034 0.0 0.0 0.0 0.0) (0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0))))

(def doc1 {:title "Doc1"
           :wordseq '("This" "is" "a" "silli" "english" "text" "test" "which" "is"
                      "onli" "here" "for" "test" "pars")})
(def doc2 {:title "Doc2"
           :wordseq '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test")})
(def doc3 {:title "Doc3"
           :wordseq '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test")})
(def doccoll (list doc1 doc2 doc3))

(facts "Compute the tf-idf matrix for a document collection"
       (let [tfidfr (tfidf/->TfIdf {} 0 {} 0 [])]
         (fact "We can add documents incrementally"
               (tfidf/add-doc tfidfr doc1) => (contains
                                               {:docs
                                                (contains
                                                 {"Doc1"
                                                  (contains {:pos 1
                                                             :tf (contains {"onli" 1/2, "which" 1/2, "pars" 1/2,
                                                                            "is" 1, "for" 1/2, "text" 1/2,
                                                                            "a" 1/2, "here" 1/2, "english" 1/2,
                                                                            "silli" 1/2, "This" 1/2, "test" 1})})})
                                               :maxdocid 1
                                               :terms (contains {"onli" {:pos 1, :docswithterm 1, :idf 0.0},
                                                                 "which" {:pos 2, :docswithterm 1, :idf 0.0},
                                                                 "pars" {:pos 3, :docswithterm 1, :idf 0.0},
                                                                 "is" {:pos 4, :docswithterm 1, :idf 0.0},
                                                                 "for" {:pos 5, :docswithterm 1, :idf 0.0},
                                                                 "text" {:pos 6, :docswithterm 1, :idf 0.0},
                                                                 "a" {:pos 7, :docswithterm 1, :idf 0.0},
                                                                 "here" {:pos 8, :docswithterm 1, :idf 0.0},
                                                                 "english" {:pos 9, :docswithterm 1, :idf 0.0},
                                                                 "silli" {:pos 10, :docswithterm 1, :idf 0.0},
                                                                 "This" {:pos 11, :docswithterm 1, :idf 0.0},
                                                                 "test" {:pos 12, :docswithterm 1, :idf 0.0}})
                                                :maxtermpos 12,
                                                :tfidfmat '((0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0))}))
         (fact "Compute tfidf for more documents"
               (-> tfidfr
                   (tfidf/add-doc doc1)
                   (tfidf/add-doc doc2)
                   (tfidf/add-doc doc3)) => (contains
                                               {:docs
                                                (contains
                                                 {"Doc1"
                                                  (contains {:pos 1
                                                             :tf (contains {"onli" 1/2, "which" 1/2, "pars" 1/2,
                                                                            "is" 1, "for" 1/2, "text" 1/2,
                                                                            "a" 1/2, "here" 1/2, "english" 1/2,
                                                                            "silli" 1/2, "This" 1/2, "test" 1})})})
                                               :maxdocid 3
                                               :terms (contains {"onli" {:pos 1, :docswithterm 3, :idf 0.0},
                                                                 "And" {:pos 15, :docswithterm 1,
                                                                        :idf 0.6931471805599453},
                                                                 "which" {:pos 2, :docswithterm 2,
                                                                          :idf 0.2876820724517807},
                                                                 "stupid" {:pos 13, :docswithterm 1,
                                                                           :idf 0.6931471805599453},
                                                                 "pars" {:pos 3, :docswithterm 1,
                                                                         :idf 0.6931471805599453},
                                                                 "Another" {:pos 14, :docswithterm 1,
                                                                            :idf 0.6931471805599453},
                                                                 "is" {:pos 4, :docswithterm 2,
                                                                       :idf 0.2876820724517807},
                                                                 "just" {:pos 16, :docswithterm 1,
                                                                         :idf 0.6931471805599453},
                                                                 "for" {:pos 5, :docswithterm 3, :idf 0.0},
                                                                 "text" {:pos 6, :docswithterm 3, :idf 0.0},
                                                                 "a" {:pos 7, :docswithterm 1,
                                                                      :idf 0.6931471805599453},
                                                                 "here" {:pos 8, :docswithterm 1,
                                                                         :idf 0.6931471805599453},
                                                                 "other" {:pos 17, :docswithterm 1,
                                                                          :idf 0.6931471805599453},
                                                                 "some" {:pos 18, :docswithterm 1,
                                                                         :idf 0.6931471805599453},
                                                                 "english" {:pos 9, :docswithterm 3, :idf 0.0},
                                                                 "silli" {:pos 10, :docswithterm 1,
                                                                          :idf 0.6931471805599453},
                                                                 "This" {:pos 11, :docswithterm 1,
                                                                         :idf 0.6931471805599453},
                                                                 "test" {:pos 12, :docswithterm 3, :idf 0.0}})
                                                :maxtermpos 18,
                                                :tfidfmat '((0.0 0.0 0.14384103622589034 0.0 0.34657359027997264 0.0 0.2876820724517807 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0)
                                                            (0.0 0.0 0.14384103622589034 0.34657359027997264 0.0 0.34657359027997264 0.14384103622589034 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0)
                                                            (0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.0))}))))
