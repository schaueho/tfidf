(defproject de.find-method/tfidf "0.1.0"
  :description "Compute tfidf"
  :url "http://github.com/schaueho/tfidf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev {:dependencies [[midje "1.8.2"]
                        [com.clojure-goes-fast/clj-async-profiler "0.1.0"]]}})
