(ns bmtagger.tfidf)

(defn tf
  "Returns a map of the normalized term frequencies for a sequence of words."
  [wordseq]
  (let [tfreq (frequencies wordseq)
        maxfreq (val (apply max-key val tfreq))]
    (reduce (fn [resmap freq]
              (update resmap freq #(/ % maxfreq)))
            tfreq (keys tfreq))))

(defn idf
  "Returns a map of the inverse document frequency for a sequence of texts (sequence of words)."
  [textseq]
  (let [alltfs (map tf textseq)
        terms (reduce conj #{}
                      (flatten (map keys alltfs)))
        count-docs-with-term (fn [term]
                               (apply + (map #(if (get % term)
                                                1
                                                0)
                                             alltfs)))
        doccount (count textseq)]
    (reduce (fn [resmap term]
              (assoc resmap term
                     (Math/log (/ (+ doccount 1)
                                  (+ (count-docs-with-term term) 1)))))
            {} terms)))


