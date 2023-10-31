(ns matvaretabellen.excel
  (:import [org.apache.poi.xssf.usermodel XSSFWorkbook])
  (:require [broch.core :as b]
            [datomic-type-extensions.api :as d]))

(defn add-index [coll]
  (map-indexed (fn [i m] (assoc m :index i)) coll))

(defn create-bold-style [workbook]
  (let [font (.createFont workbook)
        style (.createCellStyle workbook)]
    (.setBold font true)
    (.setFont style font)
    style))

(defn create-excel-file [file-name sheets]
  (let [workbook (XSSFWorkbook.)
        bold-style (create-bold-style workbook)]
    (doseq [{:keys [title rows]} sheets]
      (let [sheet (.createSheet workbook title)]
        (doseq [{:keys [index cells bold?]} (add-index rows)]
          (let [row (.createRow sheet index)]
            (doseq [{:keys [index text]} (add-index cells)]
              (let [cell (.createCell row index)]
                (when bold? (.setCellStyle cell bold-style))
                (.setCellValue cell text)))))))

    (let [file-out (java.io.FileOutputStream. file-name)]
      (.write workbook file-out)
      (.close file-out))

    :done))

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
    {:text (str (cond
                  path (get-scalar-at-path food path)
                  measurement (get-in food (into (:path measurement) [:measurement/origin :origin/id]))
                  (:nutrient/id f) (some-> (get-constituent food (:nutrient/id f))
                                           :measurement/origin
                                           :origin/id)))}))

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

(comment

  (def db (d/db matvaretabellen.dev/conn))
  (def food (d/entity db [:food/id "06.531"]))
  (def locale :nb)
  (def fields (get-basic-food-fields db locale))

  (prepare-food-cells fields food)

  (def foods [food])
  (def foods (for [eid (d/q '[:find [?e ...] :where [?e :food/id]] db)]
               (d/entity db eid)))

  (prepare-foods-sheet locale "Matvarer" (get-basic-food-fields db locale) foods)
  (prepare-foods-sheet locale "Matvarer (alle næringsstoffer)" (get-all-food-fields db locale) foods)

  (create-excel-file "test.xlsx"
                     (let [basic-fields (get-basic-food-fields db locale)
                           all-fields (get-all-food-fields db locale)]
                       [(prepare-foods-sheet locale "Matvarer" basic-fields foods)
                        (prepare-reference-sheet locale "Referanser" basic-fields foods)
                        (prepare-foods-sheet locale "Matvarer (alle næringsstoffer)" all-fields foods)
                        (prepare-reference-sheet locale "Referanser (alle næringsstoffer)" all-fields foods)]))

  )
