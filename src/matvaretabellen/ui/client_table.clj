(ns matvaretabellen.ui.client-table
  (:require [broch.core :as b]
            [fontawesome.icons :as icons]
            [matvaretabellen.food :as food]
            [matvaretabellen.food-group :as food-group]
            [matvaretabellen.nutrient :as nutrient]
            [matvaretabellen.pages.food-page :as food-page]
            [mmm.components.button :refer [Button]]
            [mmm.components.checkbox :refer [Checkbox]]
            [mmm.components.icon-button :refer [IconButton]]))

(def default-checked #{"Fett" "Karbo" "Protein" "Fiber"})

(defn prepare-foods-table [nutrients opt]
  (merge
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
                :data-id (:nutrient/id nutrient)}))}]}
   opt))

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

(defn render-table-skeleton [foods-db & [opt]]
  (let [nutrients (->> (nutrient/get-used-nutrients foods-db)
                       nutrient/sort-by-preference)]
    [:div.mmm-sidescroller.mmm-col
     [:div.mmm-hidden
      (icons/render :fontawesome.solid/sort {:class [:mmm-svg :mvt-sort]})
      (icons/render :fontawesome.solid/arrow-up-wide-short {:class [:mmm-svg :mvt-desc]})
      (icons/render :fontawesome.solid/arrow-down-short-wide {:class [:mmm-svg :mvt-asc]})]
     (->> (prepare-foods-table nutrients opt)
          food-page/render-table)
     [:div.mmm-buttons.mmm-mvm
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
        :icon-position :after})]]))

(defn render-food-groups-toggle []
  (IconButton
   {:label [:i18n ::food-groups]
    :data-toggle-target "#food-group-panel"
    :icon :fontawesome.solid/gear}))

(defn render-nutrients-toggle []
  (IconButton
   {:label [:i18n ::columns]
    :data-toggle-target "#columns-panel"
    :icon :fontawesome.solid/table}))
