(ns matvaretabellen.pages.api
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [datomic-type-extensions.api :as d]
            [matvaretabellen.food :as food]))

(defn get-all-foods [context page]
  (->> (d/q '[:find [?f ...]
              :where
              [?f :food/id]]
            (:foods/db context))
       (map #(d/entity (:foods/db context) %))
       (sort-by (comp (:page/locale page) :food/name))))

(defn camel-case [s]
  (let [[w & ws] (str/split s #"-")]
    (str w (str/join (map str/capitalize ws)))))

(def bespoke-json-keys
  {:food/id :foodId
   :food/name :foodName
   :portion-kind/name :portionName
   :portion-kind/unit :portionUnit
   :quantity/number :quantity
   :nutrient/id :nutrientId
   :source/id :sourceId})

(defn ->json [data]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (or (bespoke-json-keys x)
           (camel-case (name x)))
       x))
   data))

(defn prepare-response [context page data]
  (let [base-url (-> context :powerpack/app :site/base-url)]
    (cond-> (walk/postwalk
             (fn [x]
               (cond-> x
                 (:page/uri x) (update :page/uri #(str base-url %))))
             data)
      (= :json (:page/format page)) ->json)))

(defn render-compact-foods [context page]
  {:content-type :json
   :body (->> (get-all-foods context page)
              (map #(food/food->compact-api-data (:page/locale page) %)))})

(defn render-food-data [context page]
  {:content-type (:page/format page)
   :body (->> (get-all-foods context page)
              (map #(food/food->api-data (:page/locale page) %))
              (prepare-response context page))})
