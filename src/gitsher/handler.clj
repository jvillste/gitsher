(ns gitsher.handler
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [clojure.edn :as edn]
            [hiccup.core :as hiccup]
            [ring.middleware.file :as file]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [markdown.core :as markdown]
            [clj-jgit.querying :as git-querying]
            [clj-jgit.porcelain :as git-porcelain]
            [clj-jgit.internal :as git-internal]
            [diff-match-patch.core :as dmp])
  (:import [java.io File]
           [java.net URLEncoder URLDecoder]))


(defn file-contents [git commit file-name]
  (let [repo (.getRepository git)
        resolved-blob (.resolve repo (git-porcelain/get-blob git commit file-name))
        baos (java.io.ByteArrayOutputStream.)]
    (-> (.open repo resolved-blob)
        (.copyTo baos))

    (.toString baos)))

(defn file-log [git file-name]
  (.call (.addPath (.log git) file-name)))

(defn open-git [repo-path]
  (git-porcelain/load-repo repo-path))




#_(let [git (git-porcelain/load-repo "../blog")
        repo (.getRepository git)
        rev-walk (git-internal/new-rev-walk git)
        commit (first (git-porcelain/git-log git))
        blob (git-porcelain/get-blob git commit "blog_with_git.md")
        object (git-internal/resolve-object blob git)
        head (-> repo
                 (.resolve "HEAD"))
        resolved-blob (.resolve repo blob)
        baos (java.io.ByteArrayOutputStream.)]
    #_(-> (.open repo resolved-blob)
          (.copyTo baos))
    #_(println (.toString baos))

    (doseq [commit (.call (.addPath (.log git) "blog_with_git.md"))]
      (println commit))
    
    
    #_(git-querying/commit-info git (git-internal/resolve-object "HEAD" git))

    #_(.getRef git blob)
    #_(git-querying/rev-list-for git rev-walk object))



  (def bootstrap-head "<link href=\"/bootstrap-3.3.4-dist/css/bootstrap.min.css\" rel=\"stylesheet\">

  <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
  <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
  <!--[if lt IE 9]>
  <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>
  <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>
    <![endif]-->")

  (def bootstrap-footer "<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
  <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js\"></script>
  <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src=\"/bootstrap-3.3.4-dist/js/bootstrap.min.js\"></script>")

(defn page [title breadcrumb & content]
  (hiccup/html [:html
                [:head
                 [:title title]

                 bootstrap-head
                 
                 [:link {:href "/publisher.css" :rel "stylesheet"}]]
                
                [:body
                 
                 [:div {:class "container"}
                  [:div {:class "header"}
                   [:a {:href "/" :style "color: black"} [:h3 "Just thinking"]]
                   breadcrumb]
                  
                  content]
                 
                 bootstrap-footer]]))

(def files-directory "../blog")

(defn get-metadata [file-name]
  (with-open [rdr (clojure.java.io/reader (str files-directory "/" file-name))]
    (try (edn/read-string (first (line-seq rdr)))
         (catch Exception e))))

(defn index []
  (page "index" ""
        [:table {:class "table table-striped"}
         [:thead [:tr [:th "Title"]
                  [:th "Date published"]
                  [:th "Date modified"]
                  [:th "Tags"]]]
         [:tbody (for [file (->> (.listFiles (File. files-directory))
                                 (filter #(not (.isDirectory %))))]
                   (let [metadata (get-metadata file)]
                     [:tr [:td [:a {:href (str "/" (URLEncoder/encode (.getName file)))}
                                (.getName file)]]
                      [:td "published"]
                      [:td "modified"]
                      [:td (str (:tags metadata))]]))]]))



(defn post [name]
  (with-open [rdr (clojure.java.io/reader (str files-directory "/" name))]
    (.readLine rdr)
    (page name
          ""
          [:div (markdown/md-to-html-string (slurp rdr))
           [:a {:href (str "/history/" name)} "History"]])))

(defn history [file-name]
  (page "history" ""
        (let [file-name file-name
              git (open-git "../blog")]

          [:table {:class "table table-striped"}
           [:tbody (for [[commit-1 commit-2] (partition 2 1 (file-log git file-name))]
                     [:tr
                      [:td (let [commit-info (git-querying/commit-info git commit-1)]
                             (str (:time commit-info)
                                  " : "
                                  (:message commit-info)))]
                      [:td (dmp/pretty-html (dmp/calc-diffs (file-contents git commit-2 file-name)
                                                            (file-contents git commit-1 file-name)))]])]])))

(def app-routes
  (compojure/routes (compojure/GET "/" [] (index))
                    
                    (compojure/GET ["/:name"]  [name]
                                   (post name))
                    
                    (compojure/GET ["/history/:name"]  [name]
                                   (history name))))

(def app
  (-> (wrap-defaults app-routes site-defaults)
      (file/wrap-file "resources")))


