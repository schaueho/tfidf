(ns tfidf.tfidf
  (:require [tfidf.freq :refer [freq]]))

(defn normalize-value [maxfreq curfreq]
  "Augment a frequency value, cf. https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency_2"
  ;; cf. https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency_2 or
  ;; http://nlp.stanford.edu/IR-book/html/htmledition/maximum-tf-normalization-1.html
  (-> (* 0.6 curfreq)
      (/ maxfreq)
      (+ 0.4)))

(defn tf
  "Returns a map of term frequencies for a sequence of words.
Keyword `normalize` defaults to true, returning an augemented term frequency."
  [wordseq & {:keys [normalize] :or {normalize true}}]
  (let [tfreqs (frequencies wordseq)]
    (if-not normalize
      tfreqs
      (let [maxfreq (val (apply max-key val tfreqs))
            normalize-tf (map (fn [[term freq]]
                                   [term (normalize-value maxfreq freq)]))]
        (into {} normalize-tf tfreqs)))))

(defn tfmap-to-termvector [tf-row terms]
  "Convert tf-row into a vector of frequencies (potentially 0) for all terms in tf-row."
  (reduce (fn [tfvec term]
            (conj tfvec (get tf-row term 0)))
          [] terms))

(defn tf-from-docs [documents]
  "Returns a vector of all terms in documents and the related tf-vector for each document"
  (let [tf-rows (map tf documents)
        terms (vec (into #{} (flatten (map keys tf-rows))))]
    (vector terms
            (pmap #(tfmap-to-termvector % terms) tf-rows))))


(defn idf
  "Returns a map of the inverse document frequency for a sequence of texts (sequence of words)."
  [textseq]
  (let [alltfs (map tf textseq)
        termdoccount (reduce (fn [result tfmap]
                               (reduce (fn [resmap [term _]]
                                           (update resmap term (fnil inc 0)))
                                       result tfmap))
                             {} alltfs)
        doccount (count textseq)]
    (reduce (fn [resmap [term docswithterm]]
              (assoc resmap term
                     (Math/log (/ (+ doccount 1) ; apply smoothing!
                                  (+ docswithterm 1)))))
            {} termdoccount)))

(defn tfidf
  "Returns a sequence of the terms and the tf-idf values for a sequence of texts (sequence of words)."
  [textseq]
  (let [alltfs (pmap tf textseq)
        termdoccount (reduce (fn [result tfmap]
                               (reduce (fn [resmap [term _]]
                                           (update resmap term (fnil inc 0)))
                                       result tfmap))
                             {} alltfs)
        terms (keys termdoccount)
        doccount (count textseq)
        idf (reduce (fn [resmap [term docswithterm]]
              (assoc resmap term
                     (Math/log10 (/ doccount ; Note: no smoothing here!
                                    docswithterm))))
            {} termdoccount)
        matrix (pmap (fn [tfpdoc]
                      (map (fn [term]
                             (* (get tfpdoc term 0)
                                (get idf term 0)))
                           terms))
                    alltfs)]
    [terms matrix]))
