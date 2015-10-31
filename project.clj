(defproject gitsher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [markdown-clj "0.9.77"]
                 [hiccup "1.0.5"]
                 [clj-jgit "0.8.8"]
                 [diff-match-patch "0.1.0-SNAPSHOT"]]
  :aot [gitsher.main]
  :main gitsher.main
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler gitsher.handler/app})
