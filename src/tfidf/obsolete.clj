(ns tfidf.obsolete
  (:require [tfidf.freq :refer [freq]]
            [tfidf.tfidf :refer [normalize-value]]))

(defn normalize-tfmap [freqs]
  "Returns a normalization sequence of a frequency map."
  ;; Brief algorithmic description:
  ;; Then we map the normalization over the sequence of [term frequency] entries.
  ;; First, we get the maximum frequency to build our `normalize-maxvalue` function.
  ;; This will be used on the frequency value only `(comp normalize-max val)`
  ;; juxt applies `key` and the normalization to a [term freq] pair, generating
  ;; a new list [key normalized], which will be picked up by map.
  (let [maxfreq (val (apply max-key val freqs))
        normalize-maxvalue (partial normalize-value maxfreq)
        normalize-termfreq (juxt key (comp normalize-maxvalue val))]
    (map normalize-termfreq freqs)))

(defn ^:obsolete tf-reduce
  "Returns a map of the normalized term frequencies for a sequence of words."
  ;; Note: currently implements an augmented term frequency,
  [wordseq]
  (let [tfreq (frequencies wordseq)
        maxfreq (val (apply max-key val tfreq))]
    (reduce (fn [resmap freq]
              (update resmap freq #(+ 0.4 (/ (* 0.6 %) maxfreq))))
            tfreq (keys tfreq))))
