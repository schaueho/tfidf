(ns tfidf.bench
  (:require [clojure.string :as str]
            [tfidf.tfidf :as tfidf :refer :all]
            [tfidf.xf :as xf]
            [tfidf.protocol :as protocol :refer :all]))

(defn file2wordlist [wordlistfile]
  (-> (slurp wordlistfile)
      (str/triml)
      (str/split #"\s+")
      ((partial concat ["." ","]))))

(defn generate-random-text-coll
  "Generate a random text collection with nrtexts texts that are up to maxlength long.
Wordlist should be a file with one word per line."
  [wordlist nrtexts maxlength]
  (let [words (file2wordlist wordlist)
        lengths (range 1 maxlength)] ; we don't want 0 length texts
    (repeatedly nrtexts
                #(repeatedly (rand-nth lengths)
                             (fn []
                               (rand-nth words))))))

(defrecord Document [title url tags content status])
(defn textcoll2doccoll [textcoll]
  (map-indexed (fn [index text]
                 (-> (->Document (str "Text " index) "http://localhost/index.html" [] [] :faked )
                     (assoc :wordseq text)))
               textcoll))

;; (defn setup-testcolls [& {:keys [wordlist nrtexts maxlength] :or {wordlist "wordlist.txt"
;;                                                                   nrtexts 1000 maxlength 1000}}]
;;   (def textcoll (generate-random-text-coll wordlist nrtexts maxlength))
;;   (def doccoll (textcoll2doccoll textcoll)))

(defn tfidf-at-once-bench [textcoll]
  (time (do (tfidf textcoll)
            (println "tfidf done."))))

(defn tfidf-xf-bench [textcoll]
  (time (do (into {} (comp (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) (pmap xf/tf textcoll))
            (println "tfidf done."))))

(defn tfidf-xf-frequencies-bench [textcoll]
  (time (do (into {} (comp (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) (pmap frequencies textcoll))
            (println "tfidf done."))))

(defn tfidf-incremental-bench [doccoll]
  (time (do (tfidf-from-docs doccoll)
            (println "incremental tfidf done."))))
