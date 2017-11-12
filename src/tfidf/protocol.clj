(ns tfidf.protocol
  (:require [tfidf.freq :refer [freq]]
            [tfidf.tfidf :refer [tf]]))

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
