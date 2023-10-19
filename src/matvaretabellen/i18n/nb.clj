(ns matvaretabellen.i18n.nb)

(def dictionary
  (->>
   [#:matvaretabellen.crumbs
    {:all-food-groups "Alle matvaregrupper"
     :food-groups-url "/matvaregrupper/"
     :home "Hjem"
     :search-label "Søk i Matvaretabellen"
     }

    #:matvaretabellen.pages.food-page
    {:adi-title "Anbefalt daglig inntak (ADI)"
     :carbohydrates-title "Karbohydrater"
     :category [:fn/str "Kategori: {{:category}}"]
     :description-title "Beskrivelse av matvaren"
     :energy-title "Sammensetning og energiinnhold"
     :fat-title "Fettsyrer"
     :food-id [:fn/str "Matvare-ID: {{:id}}"]
     :latin-name [:fn/str "Latin: {{:food/latin-name}}"]
     :minerals-title "Mineraler"
     :nutrition-title "Næringsinnhold"
     :toc-title "Innhold"
     :vitamins-title "Vitaminer"
     }

    #:matvaretabellen.pages.food-groups-page
    {:all-food-groups "Alle matvaregrupper"
     }

    #:matvaretabellen.pages.frontpage
    {:search-label "Søk i Matvaretabellen"
     :search-button "Søk"
     }]
   (apply merge)))
