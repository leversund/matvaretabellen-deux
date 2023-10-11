(ns matvaretabellen.pages
  (:require [clojure.data.json :as json]
            [datomic-type-extensions.api :as d]
            [matvaretabellen.search :as search]
            [mt-designsystem.components.search-input :refer [SearchInput]]
            [mt-designsystem.components.site-header :refer [SiteHeader]]
            [powerpack.html :as html]))

(def static-pages
  [{:page/uri "/"
    :page/kind :frontpage
    :page/locale :nb}
   {:page/uri "/en/"
    :page/kind :frontpage
    :page/locale :en}
   {:page/uri "/index/nb.json"
    :page/kind :foods-index
    :page/locale :nb}
   {:page/uri "/index/en.json"
    :page/kind :foods-index
    :page/locale :en}])

(defn render-foods-index [db page]
  {:content-type "application/json"
   :body (json/write-str (search/index-foods {} db (:page/locale page)))})

(defn render-frontpage [context db page]
  (html/render-hiccup
   context
   page
   (list
    (SiteHeader {:home-url "/"})
    [:div.search-input-wrap
     (SearchInput {:label "Søk i Matvaretabellen"
                   :button {:text "Søk"}
                   :input {:name "my-search"}})])))

(defn render-page [context page]
  (let [db (d/db (:foods/conn context))]
    (case (:page/kind page)
      :foods-index (render-foods-index db page)
      :frontpage (render-frontpage context db page)
      ))
  )
