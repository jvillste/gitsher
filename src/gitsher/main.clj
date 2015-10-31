(ns gitsher.main
  (:require [ring.adapter.jetty :as jetty]
            [gitsher.handler :as handler])
  (:gen-class))

(defn -main [& args]
  (jetty/run-jetty handler/app {:port (read-string (first args))}))

;; development

(def server (atom nil))

(defn start []
  (when @server (.stop @server))
  (reset! server (jetty/run-jetty handler/app {:port 3001 :join? false}))
  #_(.start (Thread. (fn [] ))))
