(ns matvaretabellen.components.pie-chart)

(def r 100)
(def cx r)
(def cy r)
(def d (* r 2))

(defn deg->rad [deg]
  (/ (* Math/PI deg) 180))

(defn render-arc [from-deg to-deg]
  (let [x1 (+ cx (* r (Math/cos (deg->rad from-deg))))
        y1 (+ cy (* r (Math/sin (deg->rad from-deg))))
        x2 (+ cx (* r (Math/cos (deg->rad to-deg))))
        y2 (+ cy (* r (Math/sin (deg->rad to-deg))))]
    (str "M " cx " " cy " "                   ;; Move to center
         "L " x1 " " y1 " "                   ;; Line to first point on radius
         "A " r " " r " 0 0 1 " x2 " " y2 " " ;; Arc to second point
         "L " cx " " cy                       ;; Line back to center
         )))

(defn assoc-degrees [start-deg slices]
  (let [total (apply + (map :value slices))]
    (loop [[slice & tail] slices
           deg start-deg
           result []]
      (if (nil? slice)
        result
        (let [delta-deg (* 360 (/ (:value slice) total))]
          (recur tail
                 (+ deg delta-deg)
                 (conj result (assoc slice
                                     :from-deg deg
                                     :to-deg (+ deg delta-deg)))))))))

(defn PieChart [{:keys [slices]}]
  [:svg {:viewBox (str "0 0 " d " " d)}
   (for [{:keys [from-deg to-deg color]} slices]
     [:path {:d (render-arc from-deg to-deg)
             :fill color}])])