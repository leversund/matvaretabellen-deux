(ns matvaretabellen.pages.search-page
  (:require [matvaretabellen.layout :as layout]
            [matvaretabellen.ui.client-table :as client-table]
            [matvaretabellen.urls :as urls]
            [mmm.components.search-input :refer [SearchInput]]))

(defn render [context page]
  (layout/layout
   context
   page
   [:head
    [:title [:i18n ::page-title]]
    [:meta
     {:property "og:description"
      :content [:i18n ::open-graph-description]}]]
   [:body
    (layout/render-header (:page/locale page) urls/get-search-url)
    [:form.mmm-container-narrow.mmm-section.mmm-mbl.mmm-mtxl
     {:action (urls/get-search-url (:page/locale page))
      :method :get}
     [:h1.mmm-h2.mmm-mbm [:i18n :i18n/search-label]]
     (SearchInput
      {:button {:text [:i18n :i18n/search-button]}
       :input {:name "q"}
       :class :mvt-filter-search})
     [:div.mmm-mts
      (client-table/render-food-groups-toggle)
      [:span.mmm-mlm (client-table/render-nutrients-toggle)]]]
    [:div.mmm-container.mmm-section.mmm-mobile-phn.mmm-vert-layout-m
     (client-table/render-column-settings (:foods/db context))
     [:div.mmm-cols.mmm-cols-d1_2
      (client-table/render-food-group-settings context page)
      (client-table/render-table-skeleton (:foods/db context))]]]))
