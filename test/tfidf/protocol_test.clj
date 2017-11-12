(ns tfidf.protocol-test
  (:use midje.sweet)
  (:require [tfidf.tfidf :as tfidf]
            [tfidf.protocol :as protocol]))

(def doc1 {:title "Doc1"
           :wordseq '("This" "is" "a" "silli" "english" "text" "test" "which" "is"
                      "onli" "here" "for" "test" "pars")})
(def doc2 {:title "Doc2"
           :wordseq '("Another" "stupid" "english" "text" "test" "which" "is" "onli" "for" "test")})
(def doc3 {:title "Doc3"
           :wordseq '("And" "just" "some" "other" "english" "text" "test" "onli" "for" "test")})
(def doccoll (list doc1 doc2 doc3))

(facts "Compute the tf-idf matrix for a document collection"
       (let [tfidfr (protocol/->TfIdf {} 0 {} 0 [])]
         (fact "We can add documents incrementally"
               (protocol/add-doc tfidfr doc1) => (contains
                                               {:docs
                                                (contains
                                                 {"Doc1"
                                                  (contains {:pos 1
                                                             :tf (contains {"This" 0.7, "a" 0.7, "english" 0.7, "for" 0.7, "here" 0.7, "is" 1.0, "onli" 0.7, "pars" 0.7, "silli" 0.7, "test" 1.0, "text" 0.7, "which" 0.7})})})
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
                   (protocol/add-doc doc1)
                   (protocol/add-doc doc2)
                   (protocol/add-doc doc3)) => (contains
                                               {:docs
                                                (contains
                                                 {"Doc1"
                                                  (contains {:pos 1
                                                             :tf (contains {"This" 0.7, "a" 0.7, "english" 0.7, "for" 0.7, "here" 0.7, "is" 1.0, "onli" 0.7, "pars" 0.7, "silli" 0.7, "test" 1.0, "text" 0.7, "which" 0.7})})})
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
                                                :tfidfmat '((0.0 0.0 0.20137745071624646 0.0 0.48520302639196167 0.0 0.2876820724517807 0.0 0.0 0.0 0.48520302639196167 0.48520302639196167 0.0 0.0 0.0 0.48520302639196167 0.48520302639196167 0.0) (0.0 0.0 0.20137745071624646 0.48520302639196167 0.0 0.48520302639196167 0.20137745071624646 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0) (0.0 0.48520302639196167 0.0 0.0 0.0 0.0 0.0 0.48520302639196167 0.0 0.0 0.0 0.0 0.48520302639196167 0.48520302639196167 0.0 0.0 0.0 0.0))}))))
                                                ;((0.0 0.0 0.14384103622589034 0.0 0.34657359027997264 0.0 0.2876820724517807 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0)
                                                 ;           (0.0 0.0 0.14384103622589034 0.34657359027997264 0.0 0.34657359027997264 0.14384103622589034 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0)
                                                  ;          (0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.0 0.34657359027997264 0.0 0.0 0.0 0.0 0.34657359027997264 0.34657359027997264 0.0 0.0 0.0 0.0))}))))
