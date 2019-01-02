(ns tfidf.xf
  (:require [tfidf.freq :refer [freq]]
            [tfidf.tfidf :refer [normalize-value]]))

(defn normalize-tf-xf
  "Returns a normalization of frequencies (either a single map or a collection of maps). 
Returns a transducer when no collection is provided."
  ([]
   (fn [rf]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [newmax (val (apply max-key val input))
                normalize-maxvalue (partial normalize-value newmax)
                normalize-termfreq (juxt key (comp normalize-maxvalue val))]
            (rf result (into {} (map normalize-termfreq input))))))))
  ([freqs]
   (cond
     (map? freqs) (into {} (normalize-tf-xf) [freqs])
     (sequential? freqs) (into {} (normalize-tf-xf) freqs)
     :else (throw (ex-info "Don't know how to normalize non-sequential / non-map like type"
                           {:data freqs})))))

(def norm-tf-xf
  "Transducer that will return normalized frequencies."
  (comp (freq) (normalize-tf-xf)))

(defn tf
  "Returns a map of term frequencies for a sequence of words.
Keyword `normalize` defaults to true, returning an augemented term frequency."
  [wordseq & {:keys [normalize] :or {normalize true}}]
  (if normalize
    (into {} norm-tf-xf wordseq)
    (freq wordseq)))

(defn tf-from-docs-xf
  "Returns a map of terms with the number of documents a term appears in and a list of related tf-vector for each document, sorted according to the terms.
Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [termdoccount (ref (sorted-map))
           tfs (ref [])]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [newtdcount ; re-compute for each term how many documents contain it
                (reduce (fn [newtdcount term]
                          (update newtdcount term (fnil inc 0))) ; inc #term, even if missing (=0)
                        @termdoccount (keys input))
                termcount (count (keys newtdcount))              ; determine |terms|
                termzeromap (into (sorted-map)                   ; build up a sorted map
                                (zipmap (keys newtdcount)        ; of terms with vectors of
                                        (repeat termcount 0)))   ; length |terms| all set to 0
                currows (map (fn [tfdoc]                         ; re-map all existing tfs
                               (vals (merge termzeromap tfdoc))) ; so that they contains all terms
                             @tfs)                               ; with 0 or the old tf value
                newrow (vals (merge termzeromap input))
                currows (conj currows newrow)]
            (dosync
             (alter tfs conj input)
             (ref-set termdoccount newtdcount))
            (rf result {:terms newtdcount :tfs currows})))))))
  ([coll]
   (into {} (tf-from-docs-xf) (map tf coll))))

(defn idf-xf
  "Returns a map of the inverse document frequency for some documents. Expects the input to be a collection of sorted(!) maps of terms with number of documents the term appears in and a list of term frequencies.
Returns a transducer when called without a collection."
  ([]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result input]
        (let [doccount (count (:tfs input))
              terms (map (fn [[term docswithterm]]
                           [term {:doccount docswithterm
                                  :idf (Math/log10 (/ doccount docswithterm))}])
                         (:terms input))]
          (rf result {:terms (into (sorted-map) terms) :tfs (:tfs input)}))))))
  ([coll]
   (into {} (idf-xf) coll)))

(defn tfidf-xf
  "Returns a map of the terms, the tf and  tf-idf values for a sequence of texts (sequence of words),
given an input collection of sorted(!) maps of terms/doccount/idf and tf values.
  Returns a transducer if called without a collection."
  ([]
   (fn [rf]
     (fn
       ([] (rf))
       ([result] (rf result))
       ([result input]
        (let [tfidfs
              (map (fn [tfdoc]
                     ;; make use of the fact that the tf values are placed at exactly
                     ;; the same position as their corresponding term in the term vector
                     ;; by mapping over both tf and term vector in parallel
                     (map (fn [tfvalue [term {doccount :doccount idf :idf}]]
                            (* tfvalue idf))
                          tfdoc (:terms input)))
                   (:tfs input))]
          (rf result {:terms (:terms input) :tfs (:tfs input) :tfidfs tfidfs}))))))
  ([coll]
   (into {} (tfidf-xf) coll)))
