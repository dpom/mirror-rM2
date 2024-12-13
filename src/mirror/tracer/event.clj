(ns mirror.tracer.event
  "Trace the stilus using event"
  (:require
    [byte-streams :as bs]
    [clojure.spec.alpha :as spec]
    [duct.logger :refer [log]]
    [integrant.core :as ig]
    [manifold.stream :as stm]
    [me.raynes.conch.low-level :as sh]))


(def ig-key :mirror.tracer/event)

(derive ig-key :mirror/tracer)

(def abs-type 3)
(def sync-type 0)
(def key-type 1)


(def abs-codes
  {0  :x
   1  :y
   24 :pres
   25 :noop ; dist
   26 :noop ; xtilt
   27 :noop ; ytilt
   })


(def key-codes
  {320 :pen
   321 :rubber
   330 :touch
   331 :stylus
   332 :stylus2})


(defmulti format-event first)


(defmethod format-event :default [ev]
  [:noop ev])


(defmethod format-event sync-type [_]
  [:sync 0])


(defmethod format-event abs-type [[_ code val]]
  [(get abs-codes code :noop) val])


(defmethod format-event key-type [[_ code val]]
  [(get key-codes code :noop) val])


(defn noop?
  [ev]
  (= (first ev) :noop))


(defn sync?
  [ev]
  (= (first ev) :sync))


(defn format-raw-event
  "Extract usefull infor from a raw event"
  [event]
  (let [type (get event 8)
        code (+ (get event 10)
                (* (get event 11) 0x100))
        val  (+ (get event 12)
                (* (get event 13) 0x100)
                (* (get event 14) 0x10000)
                (* (get event 15) 0x1000000))]
    [type code val]))


(defn format-events
  "Format raw events received from a stream and put them in an other stream"
  [instream]
  (->> instream
       (stm/map format-raw-event)
       (stm/map format-event)
       (stm/transform (remove noop?))
       (stm/transform (partition-by sync?))
       (stm/transform (remove (fn [x] (sync? (first x)))))
       (stm/map (fn [x] (into {} x)))))


(defn out->stream
  "Convert out input stream in a manifold stream"
  [out]
  (bs/convert out (bs/stream-of bytes) {:chunk-size 16}))


;; integrant methods


(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [server logger]} config
        proc (sh/proc "ssh"
                      server
                      "cat /dev/input/event1")
        stream (-> proc
                   :out
                   out->stream
                   format-events)
        sys {:proc proc
             :logger logger
             :stream stream}]
    (log logger :info ::init)
    sys))


(defmethod ig/halt-key! ig-key [_ sys]
  (let [{:keys [proc stream logger]} sys]
    (stm/close! stream)
    (sh/destroy proc)
    (log logger :info ::halt)))


(spec/def ::server string?)
