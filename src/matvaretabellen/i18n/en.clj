(ns matvaretabellen.i18n.en)

(def dictionary
  (->>
   [#:matvaretabellen.crumbs
    {:all-food-groups "All Food Groups"
     :food-groups-url "/food-groups/"
     :home "Home"
     :search-label "Search in Matvaretabellen"
     }

    #:matvaretabellen.pages.food-page
    {:adi-title "Recommended Daily Intake (ADI)"
     :carbohydrates-title "Carbohydrates"
     :category [:fn/str "Category: {{:category}}"]
     :description-title "Description of the Food Item"
     :energy-title "Composition and Energy Content"
     :fat-title "Fatty Acids"
     :food-id [:fn/str "Food ID: {{:id}}"]
     :latin-name [:fn/str "Latin: {{:food/latin-name}}"]
     :minerals-title "Minerals"
     :nutrition-title "Nutritional Information"
     :toc-title "Contents"
     :vitamins-title "Vitamins"
     }

    #:matvaretabellen.pages.food-groups-page
    {:all-food-groups "All Food Groups"}

    #:matvaretabellen.pages.frontpage
    {:search-button "Search"
     :search-label "Search in Matvaretabellen"
     }]
   (apply merge)))
