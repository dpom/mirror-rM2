(ns mirror.renderer.screen
  "render the tablet on screen"
  (:require
    [duct.logger :refer [log]]
    [integrant.core :as ig]
    [manifold.stream :as stm]
    [mirror.schema :as sch]
    [quil.core :as q]
    [quil.middleware :as qm])
  (:import
    (processing.core
      PApplet)))


(def ig-key :mirror.renderer/screen)

(derive ig-key :mirror/renderer)

(def ratio (/ sch/x-max sch/y-max))


(def screen
  {:width 1000
   :height 1404})


(def x-scale (/ (:height screen) sch/x-max))
(def y-scale (/ (:width screen) sch/y-max))


(def background 255)
(def color 0)
(def line-weight 3)
(def dist-max-display 10)
(def smooth-bin-length 5)


(def colors
  {:pen color
   :rubber background})


(defn get-x-screen
  [x]
  (when x
    (Math/round (float (* (- sch/x-max x) x-scale)))))


(defn get-y-screen
  [y]
  (when y
    (Math/round (float (* y y-scale)))))


(defn mean
  [coll]
  (let [sum (apply + coll)
        len (count coll)]
    (if (pos? len)
      (int (/ sum len))
      0)))


(defn median
  [coll]
  (let [sorted (sort coll)
        cnt (count sorted)
        halfway (quot cnt 2)]
    (if (odd? cnt)
      (nth sorted halfway) ; (1)
      (let [bottom (dec halfway)
            bottom-val (nth sorted bottom)
            top-val (nth sorted halfway)]
        (mean [bottom-val top-val])))))


(defn smooth-line
  [line]
  (let [points (:points line)
        smooth-bins (partition-all smooth-bin-length points)]
    (assoc line :points
           (map (fn [bin]
                  [(median (map first bin))
                   (median (map second bin))])
                smooth-bins))))


;; state

(defn make-state
  []
  {:lines (atom [])
   :current-point (atom [])
   :current-dist (atom sch/dist-max)
   :current-touch (atom 0)
   :current-line (atom {:tool nil :points []})
   :in-line? (atom false)})


(defn init-state!
  [state]
  (reset! (:lines state) [])
  (reset! (:current-point state) [])
  (reset! (:current-dist state) sch/dist-max)
  (reset! (:current-touch state) 0)
  (reset! (:current-line state) {:tool nil :points []})
  (reset! (:in-line? state) false))


(defn process-event!
  [{:keys [state logger]}
   {:keys [pen x y rubber dist touch] :as event}]
  (log logger :debug ::process-event! event)
  (let [{:keys [lines current-point current-line current-dist current-touch in-line?]}  state]
    (if @in-line?
      (if (or (= pen 0) (= rubber 0))
        (do
          (swap! lines conj (smooth-line @current-line))
          (reset! current-line {:tool nil :points []})
          (reset! in-line? false))
        (do
          (reset! current-point [(or (get-y-screen y) (first @current-point))
                                 (or (get-x-screen x) (second @current-point))])
          (reset! current-dist (or dist @current-dist))
          (reset! current-touch (or touch @current-touch))
          (when (and (= @current-touch 1) (< @current-dist dist-max-display))
            (swap! current-line assoc :points (dedupe (conj (:points @current-line) @current-point))))))
      (cond
        (= pen 1) (do
                    (reset! in-line? true)
                    (reset! current-point [(get-y-screen y) (get-x-screen x)])
                    (reset! current-line {:tool :pen :points [@current-point]}))
        (= rubber 1) (do
                       (reset! in-line? true)
                       (reset! current-point [(get-y-screen y) (get-x-screen x)])
                       (reset! current-line {:tool :rubber :points [@current-point]}))
        :else nil))
    state))


;; UI

(defn setup
  []
  (q/frame-rate 6) ; draw will be called 60 times per second
  (q/background background)
  (q/stroke-weight line-weight)
  (q/smooth))


(defn clean
  [state]
  (init-state! state)
  (setup))


(defn ui-key-press
  [{:keys [state logger]}]
  (let  [raw-key (q/raw-key)
         key-code (q/key-code)]
    (log logger :debug :ui-key-press {:raw-key raw-key
                                      :key-code key-code})
    (case raw-key
      \c (clean state)
      nil)))


(defn draw-line
  [{:keys [tool points]}]
  (q/stroke (get colors tool color))
  (doseq [pair (partition 2 1 points)]
    (apply q/line pair)))


(defn draw
  [{:keys [state logger]}]
  (let [{:keys [lines in-line? current-line]} state]
    (doseq [l @lines]
      (draw-line l))
    (when @in-line?
      (draw-line @current-line))))


(defn create-sketch
  [renderer]
  (q/sketch
    :title "mirror"
    :size [(:width screen) (:height screen)]
    :setup #'setup
    :draw #(draw renderer)
    :key-pressed #(ui-key-press renderer)
    :middleware [qm/pause-on-error]))


;; integrant methods


(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [logger]} config
        _ (log logger :info ::init)
        stream (stm/stream)
        state  (make-state)
        renderer {:stream stream
                  :state state
                  :logger logger}
        sketch (create-sketch renderer)]
    (stm/consume #(process-event! renderer %) stream)
    (assoc renderer :sketch sketch)))


(defmethod ig/halt-key! ig-key [_ sys]
  (let [{:keys [stream logger sketch]} sys]
    (stm/close! stream)
    (.exit ^PApplet sketch)
    (log logger :info ::halt)))


(comment
  (require '[user :as user])

  (user/go)
  
  (keys (user/system))
  ;; => (:duct.logger.timbre/println
  ;;     [:duct/logger :duct.logger/timbre]
  ;;     :mirror.renderer/screen
  ;;     :mirror.tracer/file
  ;;     :mirror/main)

  (def main (:mirror/main (user/system)))

  (def tracer (:tracer main))

  (def renderer (:renderer main))

  (def state (:state renderer))
  state

  (user/halt)

  ;;
  )
