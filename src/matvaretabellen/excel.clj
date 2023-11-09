(ns matvaretabellen.excel
  (:import [java.io ByteArrayOutputStream FileOutputStream]
           [org.apache.poi.xssf.usermodel XSSFWorkbook])
  (:require [broch.core :as b]
            [datomic-type-extensions.api :as d]
            [matvaretabellen.misc :as misc]))

(defn add-index [coll]
  (map-indexed (fn [i m] (assoc m :index i)) coll))

(defn create-bold-style [workbook]
  (let [style (.createCellStyle workbook)
        font (.createFont workbook)]
    (.setBold font true)
    (.setFont style font)
    style))

(defn create-title-style [workbook]
  (let [style (.createCellStyle workbook)
        font (.createFont workbook)]
    (.setBold font true)
    (.setFontHeightInPoints font 16)
    (.setFont style font)
    style))

(defn count-columns [rows]
  (apply max 0 (map (comp count :cells) rows)))

(defn create-workbook [sheets]
  (let [workbook (XSSFWorkbook.)
        bold-style (create-bold-style workbook)
        title-style (create-title-style workbook)]
    (doseq [{:keys [title rows]} sheets]
      (let [sheet (.createSheet workbook title)]
        (doseq [{:keys [index cells bold? title?]} (add-index rows)]
          (let [row (.createRow sheet index)]
            (doseq [{:keys [index text]} (add-index cells)]
              (let [cell (.createCell row index)]
                (when bold? (.setCellStyle cell bold-style))
                (when title? (.setCellStyle cell title-style))
                (.setCellValue cell text)))))
        (doseq [i (range (count-columns rows))]
          (.autoSizeColumn sheet i))))
    workbook))

(defn create-excel-file [file-name workbook]
  (let [file-out (FileOutputStream. file-name)]
    (.write workbook file-out)
    (.close file-out))
  :done)

(defn create-excel-byte-array [workbook]
  (with-open [stream (ByteArrayOutputStream.)]
    (.write workbook stream)
    (.toByteArray stream)))

(defn get-basic-food-fields [db locale]
  [{:title "Matvare ID" :path [:food/id]}
   {:title "Matvare" :path [:food/name locale]}
   {:title "Spiselig del (%)" :measurement {:path [:food/edible-part]
                                            :field :measurement/percent}}
   (d/entity db [:nutrient/id "Vann"])
   {:title "Kilojoule (kJ)" :measurement {:path [:food/energy]
                                          :field :measurement/quantity}}
   {:title "Kilokalorier (kcal)" :measurement {:path [:food/calories]
                                               :field :measurement/observation}}
   (d/entity db [:nutrient/id "Fett"])
   (d/entity db [:nutrient/id "Karbo"])
   (d/entity db [:nutrient/id "Fiber"])
   (d/entity db [:nutrient/id "Protein"])
   (d/entity db [:nutrient/id "Alko"])])

(defn get-all-food-fields [db locale]
  (->> (d/q '[:find [?e ...] :where [?e :nutrient/id]]
            db)
       (map #(d/entity db %))
       (remove (comp empty? :nutrient/unit))
       (sort-by :nutrient/id)
       (into [{:title "Matvare ID" :path [:food/id]}
              {:title "Matvare" :path [:food/name locale]}])))

(defn get-constituent [food nutrient-id]
  (some->> (:food/constituents food)
           (filter (comp #{nutrient-id} :nutrient/id :constituent/nutrient))
           first))

(defn get-scalar-at-path [food path]
  (let [v (get-in food path)]
    (cond-> v
      (instance? broch.impl.Quantity v)
      b/num)))

(defn prepare-food-cells [fields food]
  (for [{:keys [path measurement] :as f} fields]
    {:text (str (cond
                  path (get-scalar-at-path food path)
                  measurement (get-in food (conj (:path measurement) (:field measurement)))
                  (:nutrient/id f) (some-> (get-constituent food (:nutrient/id f))
                                           :measurement/quantity
                                           b/num)))}))

(defn prepare-reference-cells [fields food]
  (for [{:keys [path measurement] :as f} fields]
    (let [text (str (cond
                      path (get-scalar-at-path food path)
                      measurement (get-in food (into (:path measurement) [:measurement/origin :origin/id]))
                      (:nutrient/id f) (some-> (get-constituent food (:nutrient/id f))
                                               :measurement/origin
                                               :origin/id)))]
      (cond-> {:text text}
        (not path) (assoc :origin/id text)))))

(defn prepare-foods-header-row [fields locale]
  {:bold? true
   :cells (for [field fields]
            {:text (or (:title field)
                       (str (get-in field [:nutrient/name locale])
                            " (" (:nutrient/unit field) ")"))})})

(defn prepare-foods-sheet [locale title fields foods]
  {:title title
   :rows (into [(prepare-foods-header-row fields locale)]
               (for [food (sort-by :food/id foods)]
                 {:cells (prepare-food-cells fields food)}))})

(defn prepare-reference-sheet [locale title fields foods]
  {:title title
   :rows (into [(prepare-foods-header-row fields locale)]
               (for [food (sort-by :food/id foods)]
                 {:cells (prepare-reference-cells fields food)}))})

(defn prepare-reference-lookup-sheet [db locale title reference-sheet]
  {:title title
   :rows (for [origin-id (->> (:rows reference-sheet)
                              (mapcat :cells)
                              (keep :origin/id)
                              set
                              (sort-by misc/natural-order-comparator-ish))]
           {:cells [{:text origin-id}
                    {:text (get-in (d/entity db [:origin/id origin-id])
                                   [:origin/description locale])}]})})

(def cover-sheet-text
  {:nb ["Her finner du informasjon om næringsstoffer i ulike matvarer, fordelt på fem forskjellige ark."
        ""
        "I arket Matvarer finner du de viktigste verdiene for hver matvare."
        "I arket Matvarer (alle næringsstoffer) finner du en oversikt over all tilgjengelig informasjon for hver matvare."
        "Til hvert av disse arkene finnes et vedleggsark med kildeverdier. Disse forteller hvordan verdien er funnet."
        "Til slutt finner du arket Kildeoppslag med en beskrivelse av hver kildeverdi."
        ""
        "Matvaretabellen er en tjeneste fra Mattilsynet."
        "Husk å oppgi kilde når du bruker tabellverdiene."
        "Du kan alltid finne nyeste versjon på matvaretabellen.no"
        #_""
        #_"PS! Har du behov for å behandle disse dataene programatisk er de samme verdiene tilgjengelig som JSON og EDN på matvaretabellen.no"]
   :en ["This workbook has information on nutrients in various foods, divided into five different sheets."
        ""
        "In the sheet Food Items, you will find the most important values for each food item."
        "In the sheet Food Items (all nutrients), you will find an overview of all available information for each food item."
        "For each of these sheets, there is an annex sheet with source values. These explain how the value was determined."
        "Finally, you will find the sheet Source Lookup with a description of each source value."
        ""
        "The Food Composition Table is a service from the Norwegian Food Safety Authority."
        "Remember to cite the source when using the table values."
        "You can always find the latest version at matvaretabellen.no/en/"
        #_""
        #_"PS! If you need to process these data programmatically, the same values are available as JSON and EDN at matvaretabellen.no/en/"]})

(def i18n
  {:information {:nb "Informasjon"
                 :en "Information"}
   :header {:nb "Matvaretabellen"
            :en "Food Composition Table"}
   :foods {:nb "Matvarer"
           :en "Food Items"}
   :sources {:nb "Kilder"
             :en "Sources"}
   :foods-all-nutrients {:nb "Matvarer (alle næringsstoffer)"
                         :en "Food Items (all nutrients)"}
   :sources-all-nutrientes {:nb "Kilder (alle næringsstoffer)"
                            :en "Sources (all nutrients)"}
   :source-lookup {:nb "Kildeoppslag"
                   :en "Source Lookup"}})

(defn prepare-foods-cover-sheet [locale]
  {:title (-> i18n :information locale)
   :rows (into [{:cells [{:text (str (-> i18n :header locale)
                                     " 2022")}] :title? true}
                {:cells [{:text ""}]}]
               (for [line (cover-sheet-text locale)]
                 {:cells [{:text line}]}))})

(defn prepare-food-sheets [db locale foods]
  (let [basic-fields (get-basic-food-fields db locale)
        all-fields (get-all-food-fields db locale)
        reference-sheet (prepare-reference-sheet locale (-> i18n :sources-all-nutrientes locale) all-fields foods)]
    [(prepare-foods-cover-sheet locale)
     (prepare-foods-sheet locale (-> i18n :foods locale) basic-fields foods)
     (prepare-reference-sheet locale (-> i18n :sources locale) basic-fields foods)
     (prepare-foods-sheet locale (-> i18n :foods-all-nutrients locale) all-fields foods)
     reference-sheet
     (prepare-reference-lookup-sheet db locale (-> i18n :source-lookup locale) reference-sheet)]))

(defn render-all-foods [db page]
  (let [locale (:page/locale page)
        foods (for [eid (d/q '[:find [?e ...] :where [?e :food/id]] db)]
                (d/entity db eid))]
    {:status 200
     :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}
     :body (create-excel-byte-array
            (create-workbook
             (prepare-food-sheets db locale foods)))}))

(comment

  (def db (d/db matvaretabellen.dev/conn))
  (def food (d/entity db [:food/id "06.531"]))
  (def locale :nb)
  (def fields (get-basic-food-fields db locale))

  (prepare-food-cells fields food)

  (def foods [food])
  (def foods (for [eid (d/q '[:find [?e ...] :where [?e :food/id]] db)]
               (d/entity db eid)))

  (set (keep :origin/id (mapcat :cells (:rows (prepare-reference-sheet locale "Referanser" fields foods)))))

  (prepare-foods-sheet locale "Matvarer" (get-basic-food-fields db locale) foods)
  (prepare-foods-sheet locale "Matvarer (alle næringsstoffer)" (get-all-food-fields db locale) foods)

  (create-excel-file "test.xlsx" (create-workbook (prepare-food-sheets db locale foods)))

  )
