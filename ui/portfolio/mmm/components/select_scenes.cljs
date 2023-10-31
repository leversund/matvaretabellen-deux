(ns mmm.components.select-scenes
  (:require [mmm.components.select :refer [Select]]
            [mmm.elements :as e]
            [portfolio.dumdom :as portfolio :refer [defscene]]))

(defscene select
  (e/block
   (Select
    {:options [[:option {:value "100"} "100 gram"]]})))
