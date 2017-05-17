(ns bmtagger.freq)

(defn freq
  "Returns a map from distinct items in coll to the number of times
  they appear. Returns a stateful transducer when no collection is provided."
  ([]
   (fn [rf]
     (let [freqm (atom {})]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (swap! freqm update input (fnil inc 0))
          (rf result @freqm))))))
  ([coll]
   (into {} (freq) coll)))
