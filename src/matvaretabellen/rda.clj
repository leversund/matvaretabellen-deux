(ns matvaretabellen.rda
  "Recommended Daily Allowance (RDA - ADI in Norwegian). Functions to import
  ADI/RDA from CSV, and work with the resulting data structures."
  (:require [broch.core :as b]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [datomic-type-extensions.api :as d]
            [matvaretabellen.misc :as misc]))

(defn blank-line? [s]
  (re-find #"^;+\r$" s))

(defn parse-nor-double [s]
  (parse-double (str/replace s #"," ".")))

(def kind->key
  {"min" :rda.recommendation/min-amount
   "max" :rda.recommendation/max-amount
   "gjsn" :rda.recommendation/average-amount})

(def nb-aliases
  {"Generell 10 mj" "Generell 6-65 år"})

(def en-dictionary
  {"Ammende" "Breastfeeding"
   "Generell 6-65 år" "General 6-65 years"
   "Gravid" "Pregnant"
   "Gutt 10-13 år" "Boy 10-13 years"
   "Gutt 14-17 år" "Boy 14-17 years"
   "Gutt 2-5 år" "Boy 2-5 years"
   "Gutt 6-9 år" "Boy 6-9 years"
   "Jente 10-13 år" "Girl 10-13 years"
   "Jente 14-17 år" "Girl 14-17 years"
   "Jente 2-5 år" "Girl 2-5 years"
   "Jente 6-9 år" "Girl 6-9 years"
   "Kvinne 18-30 år" "Woman 18-30 years"
   "Kvinne 31-60 år" "Woman 31-60 years"
   "Kvinne 61-74 år" "Woman 61-74 years"
   "Kvinne 75+ år" "Woman 75+ years"
   "Mann 18-30 år" "Man 18-30 years"
   "Mann 31-60 år" "Man 31-60 years"
   "Mann 61-74 år" "Man 61-74 years"
   "Mann 75+ år" "Man 75+ years"
   "Spedbarn 12-23 mnd" "Infant 12-23 months"
   "Spedbarn 6-11 mnd" "Infant 6-11 months"
   "Sykehjem/hjemmetjeneste - Energi- og næringstett kost" "Nursing home/home care - Energy and nutrient-dense diet"
   "Sykehjem/hjemmetjeneste - Nøkkelrådskost" "Nursing home/home care - Key dietary advice"

   "Aktiv 2-3 timer trening per uke" "Active 2-3 hours of exercise per week"
   "Fysisk hardt arbeid" "Physically demanding work"
   "Gjennomsnittlig aktivitetsnivå" "Moderate activity level"
   "Høyt aktivitetsnivå" "High activity level"
   "Lavt aktivitetsnivå" "Low activity level"
   "Lite aktiv mindre enn 2 timer trening per uke" "Sedentary, less than 2 hours of exercise per week"
   "Sengeliggende/inaktiv" "Bedridden/Inactive"
   "Stillesittende arbeid" "Sedentary work"
   "Stående arbeid" "Standing work"
   "Svært aktiv mer enn 3 timer trening per uke" "Very active, more than 3 hours of exercise per week"})

(defn get-recommendation [nutrient header kind v]
  (let [kind (if (re-find #"^<" v) "max" kind)]
    (if (re-find #"(E %)" header)
      [(cond
         (= "max" kind)
         :rda.recommendation/max-energy-pct

         (= "min" kind)
         :rda.recommendation/min-energy-pct

         :else
         :rda.recommendation/average-energy-pct)
       (parse-long (str/replace v #"[^\d]" ""))]
      (let [n (parse-nor-double (str/replace v #"[^\d,]" ""))]
        (cond
          (re-find #"\(g\)" header)
          [(kind->key kind) (b/from-edn [n "g"])]

          :else
          [(kind->key kind)
           (b/from-edn [n (:nutrient/unit nutrient)])])))))

(defn ->recommendation [foods-db [nutrient-id recommendations]]
  (->> (for [[header _ kind v] (remove (comp empty? last) recommendations)]
         (get-recommendation (d/entity foods-db [:nutrient/id nutrient-id]) header kind v))
       (into {:rda.recommendation/nutrient-id nutrient-id})))

(defn internationalize [nb]
  (let [nb (get nb-aliases nb nb)]
    {:nb nb
     :en (en-dictionary nb)}))

(defn get-demographic [sex-ish age-ish]
  (internationalize
   (str sex-ish
        (when-not (#{"Gravid" "Ammende"} sex-ish)
          (str
           (if (re-find #"^[\d\-\+]+" age-ish)
             " "
             " - ")
           (str/capitalize age-ish)
           (when (or (re-find #"\d-\d+$" age-ish)
                     (re-find #"\d\++$" age-ish))
             " år"))))))

(defn parse-row [foods-db headers row]
  (let [cols (map str/trim (str/split row #";"))
        leisure-activity (not-empty (nth cols 8))]
    (cond->
        {:rda/id (str "rda" (hash (nth cols 1)))
         :rda/order (parse-long (nth cols 2))
         :rda/demographic (get-demographic (nth cols 3) (nth cols 4))
         :rda/energy-recommendation (misc/kilojoules (parse-nor-double (nth cols 13)))
         :rda/kcal-recommendation (parse-nor-double (nth cols 14))
         :rda/work-activity-level (internationalize (str/capitalize (nth cols 7)))
         :rda/recommendations (->> (map conj (drop 15 headers) (drop 15 cols))
                                   (group-by second)
                                   (remove (comp empty? first))
                                   (map (partial ->recommendation foods-db))
                                   set)}
      leisure-activity (assoc :rda/leisure-activity-level (->> leisure-activity
                                                               str/capitalize
                                                               internationalize)))))

(defn read-csv
  "This reads the CSV file as exported from the very hand-tailored spreadsheet we
  once got from Jorån. I don't know if updates will follow the same format. The
  source CSV file is stored in the data directory of this repo."
  [foods-db csv-str]
  (let [[names ids modes & rows] (str/split csv-str #"\n")
        headers (map vector
                     (map str/trim (str/split names #";"))
                     (map str/trim (str/split ids #";"))
                     (map str/trim (str/split modes #";")))]
    (->> rows
         (partition-by blank-line?)
         (partition-all 2)
         (mapcat first)
         (map #(parse-row foods-db headers %)))))

(defn get-rda-url [locale profile]
  (str "/rda/" (name locale) "/" (:rda/id profile) ".json"))

(defn get-rda-pages [locales rda-profiles]
  (->> rda-profiles
       (mapcat (fn [profile]
                 (for [locale locales]
                   {:page/uri (get-rda-url locale profile)
                    :page/kind :page.kind/rda-profile
                    :page/locale locale
                    :page/rda-id (:rda/id profile)})))))

(defn unbroch [q]
  (when q
    {:n (b/num q)
     :symbol (b/symbol q)}))

(defn recommendation->json [recommendation]
  (->> {:nutrient-id (:rda.recommendation/nutrient-id recommendation)
        :min-energy-pct (:rda.recommendation/min-energy-pct recommendation)
        :max-energy-pct (:rda.recommendation/max-energy-pct recommendation)
        :average-energy-pct (:rda.recommendation/average-energy-pct recommendation)
        :min-amount (unbroch (:rda.recommendation/min-amount recommendation))
        :max-amount (unbroch (:rda.recommendation/max-amount recommendation))
        :average-amount (unbroch (:rda.recommendation/average-amount recommendation))}
       (remove (comp nil? second))
       (into {})))

(defn ->json [locale profile]
  (cond-> {:id (:rda/id profile)
           :demographic (get (:rda/demographic profile) locale)
           :energy-recommendation (unbroch (:rda/energy-recommendation profile))
           :kcal-recommendation (:rda/kcal-recommendation profile)
           :recommendations (set (map recommendation->json (:rda/recommendations profile)))}
    (:rda/work-activity-level profile)
    (assoc :work-activity-level (get (:rda/work-activity-level profile) locale))

    (:rda/leisure-activity-level profile)
    (assoc :leisure-activity-level (get (:rda/leisure-activity-level profile) locale))))

(defn render-json [context page]
  {:content-type :json
   :body (->> (d/entity (:app/db context) [:rda/id (:page/rda-id page)])
              (->json (:page/locale page)))})

(defn sort-order [rda-profile]
  (let [demographic (get-in rda-profile [:rda/demographic :nb])]
    [(cond
       (str/starts-with? demographic "Generell") 0
       (str/starts-with? demographic "Kvinne") 1
       (str/starts-with? demographic "Mann") 2
       (str/starts-with? demographic "Jente") 3
       (str/starts-with? demographic "Gutt") 4
       (str/starts-with? demographic "Spedbarn") 5
       (str/starts-with? demographic "Gravid") 6
       (str/starts-with? demographic "Ammende") 7
       :else 8)
     demographic]))

(defn get-profiles-per-demographic [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :rda/id]]
            db)
       (map #(d/entity db %))
       (group-by :rda/demographic)
       (map #(first (sort-by :rda/id (second %))))
       (sort-by sort-order)))

(comment

  (def conn matvaretabellen.dev/conn)
  (def app-db matvaretabellen.dev/app-db)

  (read-csv (d/db conn) (slurp (io/file "data/adi.csv")))

)
