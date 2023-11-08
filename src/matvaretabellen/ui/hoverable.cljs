(ns matvaretabellen.ui.hoverable)

(defn get-hover-target [component event]
  (.getElementById
   component
   (.getAttribute (.-target event) "data-hover_target_id")))

(defn show-target [component event]
  (let [target (get-hover-target component event)
        size (.getBoundingClientRect target)
        container (.-parentElement target)
        bounds (.getBoundingClientRect container)
        cx-ratio (.getAttribute (.-target event) "data-hover_cx_ratio")
        cy-ratio (.getAttribute (.-target event) "data-hover_cy_ratio")]
    (set! (.-top (.-style target)) (str (- (* cy-ratio (.-height bounds))
                                           (.-height size)
                                           10) ;; height of arrow
                                        "px"))
    (set! (.-left (.-style target)) (str (- (* cx-ratio (.-width bounds))
                                            (/ (.-width size) 2)) "px"))
    (.add (.-classList target) "mtv-hover-popup-visible")))

(defn hide-target [component event]
  (-> (get-hover-target component event)
      .-classList
      (.remove "mtv-hover-popup-visible")))

(defn set-up [component]
  (when component
    (doseq [element (.querySelectorAll component ".js-hoverable")]
      (.addEventListener element "mouseenter" #(show-target component %) false)
      (.addEventListener element "mouseleave" #(hide-target component %) false)
      ;; apparently mobile browsers will simulate these events when clicking inside and outside an element,
      ;; so I have removed the click handler that used to be here
      )))
