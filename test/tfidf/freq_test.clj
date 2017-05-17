(ns bmtagger.freq-test
  (:use midje.sweet)
  (:require [bmtagger.freq :as f :refer [freq]]))


(defn intermediate-frequencies [coll]
  "Call `frequencies` on each consecutive subsequence of coll, i.e. starting with the first element, then with first and the second element, then the first to the third and so on."
  (loop [[head & tail] coll
         seen []
         result []]
    (let [newseen (conj seen head)
          newfreq (conj result (frequencies newseen))]
      (if (seq? tail)
        (recur tail newseen newfreq)
        newfreq))))

(fact "Intermediate frequencies on some testdata"
      (intermediate-frequencies [:a :b :c :a :a :b])
      => [{:a 1} {:a 1, :b 1} {:a 1, :b 1, :c 1} {:a 2, :b 1, :c 1}
          {:a 3, :b 1, :c 1} {:a 3, :b 2, :c 1}])
  
(facts "freq is a stateful transducer working like `frequencies`"
       (let [testdata [:a :b :c :a :a :b]
             simple-freq (frequencies testdata)
             intermediate-freqs (intermediate-frequencies testdata)]
       (fact "Transducing with `conj` as a reduction function on a sequence returns a sequence of intermediate `frequencies` results"
             (transduce (freq) conj testdata) => intermediate-freqs)

       (fact "Transducing with `merge` as a reduction function on a sequence yiels a single `frequencies` map result"
             (transduce (freq) merge testdata) => simple-freq)

       (fact "Usage with `into []` also returns a sequence of intermediate `frequencies` results"
             (into [] (freq) testdata) => intermediate-freqs)

       (fact "Usage with `into {}` also returns simply a map of a `frequencies` map result"
             (into {} (freq) testdata) => simple-freq)

       (fact "Calling `freq` with a collection returns the same result as calling `frequencies`"
             (freq testdata) => simple-freq)))

