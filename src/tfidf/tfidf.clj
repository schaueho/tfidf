(ns bmtagger.tfidf
  (:require [bmtagger.freq :refer [freq]]))

(defn normalize-value [maxfreq curfreq]
  "Augment a frequency value, cf. https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency_2"
  ;; cf. https://en.wikipedia.org/wiki/Tf%E2%80%93idf#Term_frequency_2 or
  ;; http://nlp.stanford.edu/IR-book/html/htmledition/maximum-tf-normalization-1.html
  (-> (* 0.6 curfreq)
      (/ maxfreq)
      (+ 0.4)))

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

(defn normalize-tf-xf
  "Returns a normalization of frequencies (either a single map or a collection of maps). 
Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [nfm (atom {})
           maxfreq (atom 1)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [newmax (val (apply max-key val input))
                normalize-maxvalue (partial normalize-value newmax)
                normalize-termfreq (juxt key (comp normalize-maxvalue val))]
            (reset! maxfreq newmax)
            (swap! nfm merge (into {} (map normalize-termfreq input)))
            (rf result @nfm)))))))
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

(defn tf-map
  "Returns a map of term frequencies for a sequence of words.
Keyword `normalize` defaults to true, returning an augemented term frequency."
  [wordseq & {:keys [normalize] :or {normalize true}}]
  (let [tfreqs (frequencies wordseq)]
    (if-not normalize
      tfreqs
      (let [maxfreq (val (apply max-key val tfreqs))
            normalize-tf-xf (map (fn [[term freq]]
                                   [term (normalize-value maxfreq freq)]))]
        (into {} normalize-tf-xf tfreqs)))))

(defn tf-reduce
  "Returns a map of the normalized term frequencies for a sequence of words."
  ;; Note: currently implements an augmented term frequency,
  [wordseq]
  (let [tfreq (frequencies wordseq)
        maxfreq (val (apply max-key val tfreq))]
    (reduce (fn [resmap freq]
              (update resmap freq #(+ 0.4 (/ (* 0.6 %) maxfreq))))
            tfreq (keys tfreq))))

(defn tfmap-to-termvector [tf-row terms]
  "Convert tf-row into a vector of frequencies (potentially 0) for all terms in tf-row."
  (reduce (fn [tfvec term]
            (conj tfvec (get tf-row term 0)))
          [] terms))

(defn tf-from-docs [documents]
  (let [tf-rows (map tf documents)
        terms (vec (into #{} (flatten (map keys tf-rows))))]
    (vector terms
            (pmap #(tfmap-to-termvector % terms) tf-rows))))

(defn idf
  "Returns a map of the inverse document frequency for a sequence of texts (sequence of words)."
  [textseq]
  (let [alltfs (map tf textseq)
        terms (reduce conj #{}
                      (flatten (map keys alltfs)))
        count-docs-with-term (fn [term]
                               (apply + (map #(if (get % term) 1 0)
                                             alltfs)))
        doccount (count textseq)]
    (reduce (fn [resmap term]
              (assoc resmap term
                     (Math/log (/ (+ doccount 1) ; apply smoothing!
                                  (+ (count-docs-with-term term) 1)))))
            {} terms)))

(defn tfidf
  "Returns a sequence of the terms and the tf-idf values for a sequence of texts (sequence of words)."
  [textseq]
  (let [alltfs (map tf textseq)
        terms (into []
                    (reduce conj #{}
                            (flatten (map keys alltfs))))
        count-docs-with-term (fn [term]
                               (apply + (map #(if (get % term)
                                                1
                                                0)
                                             alltfs)))
        doccount (count textseq)
        idf (reduce (fn [resmap term] ; Note: no smoothing here!
                      (assoc resmap term
                             (Math/log10 (/ doccount
                                            (count-docs-with-term term)))))
                    {} terms)
        matrix (map (fn [tfpdoc]
                      (map (fn [term]
                             (* (get tfpdoc term 0)
                                (get idf term 0)))
                           terms))
                    alltfs)]
    [terms matrix]))

(defprotocol TfIdfProtocol
  (add-doc [tfidfr doc])
  (get-tfidf-for-doc [tfidfr docid])
  (tfidf-doc-term [tfidfr docid term])
  (term2pos [tfidfr term])
  (docs-with-term [tfidfr term])
  (doc2pos [tfidfr docid]))

(defrecord TfIdf [docs maxdocid terms maxtermpos tfidfmat]
  TfIdfProtocol
  (add-doc [tfidfr doc]
    (let [newdocid (inc maxdocid)
          newtf (tf (:wordseq doc))
          newdocs (assoc docs (:title doc) {:pos newdocid :tf newtf})
          idf-helper (fn [newdocid docwithterms]
                       (Math/log (/ (+ newdocid 1)
                                    (+ docwithterms 1))))
          [newterms newmaxtermpos]
                  ;; collect all new terms, increase maxtermpos
                  (loop [[term & restterms] (keys newtf)
                          curmaxtermpos maxtermpos
                          nterms terms]
                     (if-let [termpos (get-in nterms [term :pos])]
                       ;; term already known, update docswithterm (pos doesn't change)
                       (let [lnterms (update-in nterms [term :docswithterm] inc)]
                         (if (seq restterms)
                           (recur restterms curmaxtermpos lnterms)
                           [lnterms curmaxtermpos]))
                       ;; term is new, hence docswithterm=1 and pos=max term pos+1
                       (let [lnterms (assoc nterms term {:pos (inc curmaxtermpos)
                                                         :docswithterm 1})]
                         (if (seq restterms)
                           (recur restterms (inc curmaxtermpos) lnterms)
                           [lnterms  (inc curmaxtermpos)]))))
          idfterms (reduce (fn [nterms term]  ;; update idf for all terms
                             (assoc-in nterms [term :idf]
                                       (idf-helper newdocid
                                                   (get-in nterms [term :docswithterm]))))
                           newterms (keys newterms))]
      (->  tfidfr
           (assoc :docs newdocs)
           (assoc :maxdocid newdocid)
           (assoc :terms idfterms)
           (assoc :maxtermpos newmaxtermpos)
           (assoc :tfidfmat
             ;; calculate tf-idf for all terms, for all documents
             (let [nterms (keys newterms)]
               (map (fn [[doctitle docdata]]
                      (map (fn [term]
                             (* (get (:tf docdata) term 0)
                                (get-in idfterms [term :idf] 0)))
                           nterms))
                    (seq newdocs)))))))
  (get-tfidf-for-doc [tfidfr docid]
    (when-let [docpos (doc2pos tfidfr docid)]
      (nth (:tfidfmat tfidfr) docpos)))
  (tfidf-doc-term [tfidfr docid term]
    (when-let [doc-tfidf (get-tfidf-for-doc tfidfr docid)]
      (when-let [termpos (term2pos tfidfr term)]
        (nth doc-tfidf termpos))))
  (term2pos [tfidfr term]
    (dec (get-in tfidfr [:terms term :pos])))
  (doc2pos [tfidfr doctitle]
    (dec (get-in tfidfr [:docs doctitle :pos])))
  (docs-with-term [tfidfr term]
    (get-in tfidfr [:terms term :docwithterm] 0)))

(defn tfidf-from-docs
  "Returns a sequence of the tf-idf values for a sequence of documents."
  [docseq]
  (reduce (fn [tfidfr doc]
            (add-doc tfidfr doc))
          (->TfIdf {} 0 {} 0 []) docseq))
