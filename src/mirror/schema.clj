(ns mirror.schema
  (:require
    [malli.core :as m]
    [malli.generator :as mg]
    ))

(def max-x 20967)

(def X [:int {:min 0 :max max-x}])

(def max-y 15725)

(def Y [:int {:min 0 :max max-y}])

(def Point
  [:tuple #'X #'Y])

(def Event
[:map
 [:x {:optional true} #'X]
 [:y {:optional true} #'Y]
 [:pres {:optional true} int?]
 [:pen {:optional true} [:enum 0 1]]
 [:rubber {:optional true} [:enum 0 1]]
 [:touch {:optional true} [:enum 0 1]]])

(def Line
  [:map
   [:type [:enum :pen :rubber]]
   [:points [:vector #'Point]]])

(def State
  [:map
   [:lines [:vector #'Line]]
   [:current-point #'Point]
   [:current-line #'Line]
   [:in-line? boolean]])


(comment
  (mg/generate X)
  ;; => 11653
  (mg/generate Y)
  ;; => 53
  (mg/generate Point)
  ;; => {:x 15496, :y 1365}
  (mg/generate Line)
  ;; => {:type :pen,
  ;;     :points
  ;;     [[507 14228]
  ;;      [6906 1]
  ;;      [13 9966]
  ;;      [4 890]
  ;;      [12480 8402]
  ;;      [11910 115]
  ;;      [4 2]
  ;;      [2 9472]
  ;;      [15821 12392]
  ;;      [12061 13336]
  ;;      [11774 10081]
  ;;      [352 191]
  ;;      [19985 60]
  ;;      [15741 5961]
  ;;      [1076 14937]
  ;;      [16159 12174]
  ;;      [18198 4]
  ;;      [529 1257]
  ;;      [14352 8352]
  ;;      [0 8952]
  ;;      [2 15540]]}

  ;;
  )
