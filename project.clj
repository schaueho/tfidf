(defproject de.find-method/tfidf "0.1.1-SNAPSHOT"
  :description "Compute tfidf"
  :url "http://github.com/schaueho/tfidf"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :profiles
  {:dev {:dependencies [[org.clojure/core.async "0.4.490"]
                        [midje "1.9.4"]
                        [com.clojure-goes-fast/clj-async-profiler "0.1.0"]]}})
