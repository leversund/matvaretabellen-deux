(ns mt-designsystem.components.search-input)

(defn SearchInput [{:keys [label button input]}]
  [:form.form-layout
   [:label.form-label {:for (:name input)} label]
   [:div.search-wrap
    [:input.form-field.input-search.hasButton {:id (:name input) :name (:name input) :type "search"}]
    [:button.button.button--flat.form-field.button-search-primary.icon--search-before-beige
     {:type "submit"} (:text button)]]])