(ns tfidf.bench
  (:require [clojure.string :as str]
            [clojure.core.async :as async]
            [tfidf.tfidf :as tfidf :refer :all]
            [tfidf.xf :as xf]
            [tfidf.freq :as freq]
            [tfidf.protocol :as protocol :refer :all]))

(defn file2wordlist [wordlistfile]
  (-> (slurp wordlistfile)
      (str/triml)
      (str/split #"\s+")
      ((partial concat ["." ","]))))

(defn generate-random-text-coll-sequential
  "Generate a random text collection with nrtexts texts that are up to maxlength long.
Wordlist should be a file with one word per line."
  [wordlist nrtexts maxlength]
  (let [words (file2wordlist wordlist)
        lengths (range 1 maxlength)] ; we don't want 0 length texts
    (doall (repeatedly nrtexts
                #(doall (repeatedly (rand-nth lengths)
                                    (fn []
                                      (rand-nth words))))))))

(defn generate-random-text-coll
  "Generate a random text collection with nrtexts texts that are up to maxlength long.
Wordlist should be a file with one word per line."
  [wordlist nrtexts maxlength]
  (let [words (file2wordlist wordlist)
        lengths (range 1 maxlength) ; we don't want 0 length texts
        random-word (fn [_] (rand-nth words))]
    (doall (pmap (fn [_]
                   (doall (pmap random-word (range 1 (rand-nth lengths)))))
                 (range 1 nrtexts)))))

(defrecord Document [title url tags content status])
(defn textcoll2doccoll [textcoll]
  (doall (map-indexed (fn [index text]
                 (-> (->Document (str "Text " index) "http://localhost/index.html" [] [] :faked )
                     (assoc :wordseq text)))
               textcoll)))

(defn setup-testcolls [& {:keys [wordlist nrtexts maxlength] :or {wordlist "wordlist.txt"
                                                                  nrtexts 200 maxlength 1000}}]
  (def textcoll (generate-random-text-coll wordlist nrtexts maxlength))
  (def doccoll (textcoll2doccoll textcoll)))

(defn tfidf-at-once-bench [textcoll & {:keys [results] :or {results false}}]
  (time (let [result (tfidf textcoll)]
          (println "tfidf done.")
          (when results
            result))))

(defn tfidf-xf-bench [textcoll]
  (time (do (into {} (comp (map xf/tf) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) textcoll)
            (println "tfidf done."))))

(defn tfidf-xf-freq-bench [textcoll]
  (time (do (into {} (comp (map freq/freq) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) textcoll)
            (println "tfidf done."))))

(defn tfidf-xf-frequencies-bench [textcoll]
  (time (do (into {} (comp (map frequencies) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) textcoll)
            (println "tfidf done."))))

(defn tfidf-xf-frequencies-pmap-bench [textcoll]
  (time (do (into {} (comp (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) (doall (pmap frequencies textcoll)))
            (println "tfidf done."))))

(defn take-or-timeout! [channel waittime]
  (let [tchannel (async/timeout waittime)
        [value ch] (async/alts!! [channel tchannel])]
    (if (= ch tchannel)
      (do ;(println "Timed out after " waittime " ms.")
          :timeout)
      value)))

(defn tfidf-go-bench [textcoll & {:keys [f timeout results] :or {f xf/tf
                                                                 timeout 60000
                                                                 results false}}]
  (time (do
          (let [tfidf (comp (map f) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf))
                c (async/chan 50 tfidf)
                resultchan (async/chan)]
            (async/onto-chan c textcoll)
            (async/go
              (loop [n (async/<!! c)
                     results []]
                (if n
                  (recur (async/<!! c) (conj results n))
                  (async/put! resultchan results))))
            (let [result (take-or-timeout! resultchan timeout)]
              (if (= result :timeout)
                (println "timed out.")
                (do (println "tfidf done.")
                    (when results
                      result))))))))

(defn tfidf-ppl-bench [textcoll & {:keys [f parallel timeout results]
                                   :or {f xf/tf
                                        parallel 10
                                        timeout 60000
                                        results false}}]
  (time (do
          (let [tfidf (comp (map f) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf))
                c1 (async/chan 1)
                c2 (async/chan 1)
                resultchan (async/chan)]
            (async/pipeline parallel c2 tfidf c1)
            (async/onto-chan c1 textcoll)
            (async/go
              (loop [n (async/<!! c2)
                     results []]
                (if n
                  (recur (async/<!! c2) (conj results n))
                  (async/put! resultchan results))))
            (let [result (take-or-timeout! resultchan timeout)]
              (if (= result :timeout)
                (println "timed out.")
                (do (println "tfidf done.")
                    (when results
                      result))))))))


(defn tfidf-xf-parts-bench [textcoll & {:keys [f] :or {f xf/tf}}]
  (time (do (into {} (map f textcoll))
            (println "f done.")))
  (time (do (into {} (comp (map f) (xf/tf-from-docs-xf)) textcoll)
            (println "tf-from-docs done.")))
  (time (do (into {} (comp (map f) (xf/tf-from-docs-xf) (xf/idf-xf)) textcoll)
            (println "idf done.")))
  (time (do (into {} (comp (map f) (xf/tf-from-docs-xf) (xf/idf-xf) (xf/tfidf-xf)) textcoll)
            (println "tfidf done."))))

(defn tfidf-incremental-bench [doccoll]
  (time (do (tfidf-from-docs doccoll)
            (println "incremental tfidf done."))))
