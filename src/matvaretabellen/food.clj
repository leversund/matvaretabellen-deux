(ns matvaretabellen.food
  (:require [clojure.string :as str]
            [datomic-type-extensions.api :as d]
            [matvaretabellen.misc :as misc]
            [matvaretabellen.nutrient :as nutrient]))

(defn get-nutrients [food nutrient-id]
  (->> (:food/constituents food)
       (map :constituent/nutrient)
       (filter (comp #{nutrient-id}
                     :nutrient/id
                     :nutrient/parent))
       nutrient/sort-by-preference
       seq))

(defn get-nutrient-group [food nutrient-id]
  {:food food
   :nutrients (get-nutrients food nutrient-id)
   :group (d/entity (d/entity-db food) [:nutrient/id nutrient-id])})

(defn get-flattened-nutrient-group [food nutrient-id]
  (-> (get-nutrient-group food nutrient-id)
      (update :nutrients #(mapcat
                           (fn [nutrient]
                             (or (get-nutrients food (:nutrient/id nutrient))
                                 [nutrient]))
                           %))))

(defn humanize-langual-classification [text]
  (->> (re-seq #"([^\.]+)(?:([\.] ))?" text)
       (map
        (fn [[_ part separator]]
          (str (->> (str/trim part)
                    (re-seq #"([^\(]+)(?:(\([^\)]+\)))?")
                    (map (fn [[_ text parens]]
                           (str/join [(str/capitalize text) parens])))
                    str/join)
               (some-> separator str/trim))))
       (str/join " ")))

(defn get-langual-codes [food]
  (->> (:food/langual-codes food)
       (sort-by :langual-code/id)))

(defn get-sources [food]
  (->> (:food/constituents food)
       (keep :measurement/origin)
       set
       (sort-by (comp misc/natural-order-comparator-ish :origin/id))))

(defn hyperlink-string [desc]
  (loop [words (-> (str/replace desc #"," ", ")
                   (str/split #"\s+"))
         res []]
    (let [word (first words)]
      (cond
        (nil? word)
        (let [groups (partition-by type res)]
          (mapcat
           (fn [i xs]
             (if (string? (first xs))
               [(str (when (not= 0 i) " ")
                     (str/join " " xs)
                     (when (not= (dec (count groups)) i) " "))]
               (interpose " " xs)))
           (range)
           groups))

        (re-find #"https?://.+" word)
        (recur
         (next words)
         (conj res (let [url (str/trim (str/replace word #" " ""))]
                     [:a {:href url}
                      (if (< 35 (count url))
                        (second (re-find #"https?://([^/]+)/" url))
                        url)])))

        :else
        (recur (next words) (conj res word))))))

(comment

  (def conn matvaretabellen.dev/conn)

  (def dried-apple "06.531")
  (def banana "06.525")

  (->> "WaterSolubleVitamins"
       (get-nutrients (d/entity (d/db conn) [:food/id banana]))
       (map #(into {} %)))

  (->> (d/entity (d/db conn) [:nutrient/id "Vit A"])
       :nutrient/_parent
       (map #(into {} %)))

  (d/q '[:find ?d
         :where
         [_ :langual-code/description ?d]]
       (d/db conn))
)
