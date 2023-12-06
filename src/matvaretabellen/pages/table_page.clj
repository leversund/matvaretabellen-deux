(ns matvaretabellen.pages.table-page
  (:require [broch.core :as b]
            [fontawesome.icons :as icons]
            [matvaretabellen.food :as food]
            [matvaretabellen.food-group :as food-group]
            [matvaretabellen.layout :as layout]
            [matvaretabellen.nutrient :as nutrient]
            [matvaretabellen.pages.food-page :as food-page]
            [matvaretabellen.urls :as urls]
            [mmm.components.button :refer [Button]]
            [mmm.components.checkbox :refer [Checkbox]]
            [mmm.components.search-input :refer [SearchInput]]))

(def default-checked #{"Fett" "Karbo" "Protein" "Fiber"})

(defn prepare-foods-table [nutrients]
  {:headers (concat [{:text (list [:i18n ::food]
                                  [:span.mvt-sort-icon
                                   (icons/render :fontawesome.solid/sort {:class :mmm-svg})])
                      :class [:mmm-nbr]
                      :data-id "foodName"}
                     {:text (list [:i18n ::energy-kj]
                                  [:span.mvt-sort-icon
                                   (icons/render :fontawesome.solid/sort {:class :mmm-svg})])
                      :class [:mmm-nbr :mmm-tar]
                      :data-id "energyKj"}
                     {:text (list [:i18n ::energy-kcal]
                                  [:span.mvt-sort-icon
                                   (icons/render :fontawesome.solid/sort {:class :mmm-svg})])
                      :class [:mmm-nbr :mmm-tar]
                      :data-id "energyKcal"}]
                    (for [nutrient nutrients]
                      {:text (list [:i18n :i18n/lookup (:nutrient/name nutrient)]
                                   [:span.mvt-sort-icon
                                    (icons/render :fontawesome.solid/sort {:class :mmm-svg})])
                       :data-id (:nutrient/id nutrient)
                       :class (if (not (default-checked (:nutrient/id nutrient)))
                                [:mmm-nbr :mmm-tar :mmm-hidden]
                                [:mmm-nbr :mmm-tar])}))
   :id "filtered-giant-table"
   :classes [:mmm-hidden]
   :data-page-size 250
   :rows [{:cols
           (concat
            [{:text [:a.mmm-link]
              :data-id "foodName"}
             {:text (list [:span.mvt-num "0"] " "
                          [:span.mvt-sym "kJ"])
              :class :mmm-tar
              :data-id "energyKj"}
             {:text "0 kcal"
              :class :mmm-tar
              :data-id "energyKcal"}]
            (for [nutrient nutrients]
              {:text (food/get-calculable-quantity
                      {:measurement/quantity (b/from-edn [0 (:nutrient/unit nutrient)])}
                      {:decimals (:nutrient/decimal-precision nutrient)})
               :class (cond-> [:mmm-tar :mmm-nbr :mvt-amount]
                        (not (default-checked (:nutrient/id nutrient)))
                        (conj :mmm-hidden))
               :data-id (:nutrient/id nutrient)}))}]})

(defn render-filter-list [options]
  (when (seq options)
    [:ul.mmm-ul.mmm-unadorned-list
     (for [filter-m options]
       [:li
        (Checkbox filter-m)
        (render-filter-list (:options filter-m))])]))

(defn render-nutrient-filter-column [filters]
  [:div.mmm-col.mmm-vert-layout-m
   (for [filter-m filters]
     (if (:data-filter-id filter-m)
       (render-filter-list [filter-m])
       (->> (list (when (:label filter-m)
                    [:h3 (select-keys filter-m [:class]) (:label filter-m)])
                  (render-filter-list (:options filter-m)))
            (remove nil?))))])

(defn render-column-settings [foods-db]
  [:div.mmm-divider.mmm-vert-layout-m.mmm-bottom-divider.mmm-hidden#columns-panel
   [:div.mmm-cols.mmm-twocols
    (->> (nutrient/prepare-filters foods-db {:columns 2})
         (map render-nutrient-filter-column))]])

(defn render-food-group-settings [context page]
  [:div.mmm-vert-layout-m.mmm-col.mmm-hidden#food-group-panel
   (food-group/render-food-group-filters
    (:app/db context)
    (food-group/get-food-groups (:foods/db context))
    nil
    (:page/locale page))])

(defn render-table-skeleton [nutrients]
  [:div.mmm-sidescroller.mmm-col
   [:div.mmm-hidden
    (icons/render :fontawesome.solid/sort {:class [:mmm-svg :mvt-sort]})
    (icons/render :fontawesome.solid/arrow-up-wide-short {:class [:mmm-svg :mvt-desc]})
    (icons/render :fontawesome.solid/arrow-down-short-wide {:class [:mmm-svg :mvt-asc]})]
   (->> (prepare-foods-table nutrients)
        food-page/render-table)
   [:div.mmm-buttons.mmm-mbm
    (Button
     {:text [:i18n ::prev]
      :class [:mvt-prev :mmm-hidden]
      :secondary? true
      :inline? true
      :icon :fontawesome.solid/chevron-left})
    (Button
     {:text [:i18n ::next]
      :class [:mvt-next :mmm-hidden]
      :secondary? true
      :inline? true
      :icon :fontawesome.solid/chevron-right
      :icon-position :after})]])

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
    (layout/render-header (:page/locale page) urls/get-table-url)
    [:div.mmm-themed.mmm-brand-theme1
     (layout/render-toolbar
      {:locale (:page/locale page)
       :crumbs [{:text [:i18n :i18n/search-label]
                 :url (urls/get-base-url (:page/locale page))}
                {:text [:i18n ::page-title]}]})
     [:div.mmm-container.mmm-section
      [:div.mmm-media
       [:article.mmm-vert-layout-m
        [:div.mmm-vert-layout-s
         [:h1.mmm-h1 [:i18n ::page-title]]]
        [:div
         (Button {:text [:i18n ::download]
                  :href (urls/get-foods-excel-url (:page/locale page))
                  :icon :fontawesome.solid/arrow-down
                  :inline? true
                  :secondary? true})]]]]]
    (let [nutrients (->> (nutrient/get-used-nutrients (:foods/db context))
                         nutrient/sort-by-preference)]
      [:div.mmm-container.mmm-section.mmm-mobile-phn.mmm-vert-layout-m
       [:div.mmm-flex-desktop.mmm-flex-middle.mmm-pvs.mmm-mobile-container-p
        [:form.mmm-block.mvt-filter-search
         (SearchInput
          {:button {:text [:i18n :i18n/search-button]}
           :input {:name "foods-search"
                   :data-suggestions "8"
                   :placeholder [:i18n ::placeholder]}
           :autocomplete-id "foods-results"
           :size :small})]
        [:div.mmm-flex.mmm-flex-gap
         (Button
          {:text [:i18n ::food-groups]
           :data-toggle-target "#food-group-panel"
           :secondary? true
           :inline? true
           :class [:mmm-desktop]
           :icon :fontawesome.solid/gear})
         (Button
          {:text [:i18n ::columns]
           :data-toggle-target "#columns-panel"
           :secondary? true
           :inline? true
           :class [:mmm-desktop]
           :icon :fontawesome.solid/table})]]
       (render-column-settings (:foods/db context))
       [:div.mmm-cols.mmm-cols-d1_2
        (render-food-group-settings context page)
        (render-table-skeleton nutrients)]])]))
