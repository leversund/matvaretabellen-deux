(ns matvaretabellen.elements.image-scenes
  (:require [matvaretabellen.elements :as e]
            [portfolio.dumdom :as portfolio :refer [defscene]]))

(defscene image
  "Bilder har maksbredde på 100%"
  (e/img {:src "/images/dog.jpg"}))
