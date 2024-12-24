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

(def scale-value 20.0)
(def background 255)
(def color 0)

(def colors
  {:pen color
   :rubber background})

(defn scale
  [x max-x]
  (if x
    (Math/round (float (/ (abs (- max-x x)) scale-value)))
    x))


(def screen {:width (scale sch/max-y 0)
             :height (scale sch/max-x 0)})

;; state

(defn init-state!
  [state]
  (reset! (:lines state) [])
  (reset! (:current-point state) [])
  (reset! (:current-line state) {:tool nil :points []})
  (reset! (:in-line? state) false))

(defn process-event!
  [{:keys [lines current-point current-line in-line?] :as state}
   {:keys [pen x y rubber]}]
  (if @in-line?
    (if (or (= pen 0) (= rubber 0))
      (do
        (swap! lines conj @current-line)
        (reset! current-line {:tool nil :points []})
        (reset! in-line? false)
        )
      (do
        (reset! current-point [(or (scale y 0) (first @current-point))
                               (or (scale x sch/max-x) (second @current-point))])
        (swap! current-line assoc :points (dedupe (conj (:points @current-line) @current-point)))
        (tap> @current-line)))
    (cond
      (= pen 1) (do
                  (reset! in-line? true)
                  (reset! current-point [(scale y 0) (scale x sch/max-x)])
                  (reset! current-line {:tool :pen :points [@current-point]}))
      (= rubber 1) (do
                     (reset! in-line? true)
                     (reset! current-point [(scale y 0) (scale x sch/max-x)])
                     (reset! current-line {:tool :rubber :points [@current-point]}))
      :else nil))
  state)


;; UI

(defn setup
  []
  (q/frame-rate 6) ; draw will be called 60 times per second
  (q/background 255)
  (q/stroke-weight 1)
  (q/smooth))

(defn clean
  [state]
  (init-state! state)
  (setup))

(defn ui-key-press
  [state]
  (let  [raw-key (q/raw-key)
         key-code (q/key-code)]
    (tap> {:raw-key raw-key
           :key-code key-code})
    (case raw-key
      \c (clean state)
      nil)))

(defn draw-line
  [{:keys [tool points]}]
  (q/stroke (get colors tool color))
  (doseq [pair (partition 2 1 points)]
    (apply q/line pair)))


(defn draw [{:keys [lines in-line? current-line]}]
  (doseq [l @lines]
    (draw-line l))
  (when @in-line?
    (draw-line @current-line)))


(defn create-sketch
  [state]
  (q/sketch
    :title "mirror"
    :size [(:width screen) (:height screen)]
    :setup #'setup
    :draw #(draw state)
    :key-pressed #(ui-key-press state)
    :middleware [qm/pause-on-error])
  )

;; integrant methods


(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [logger]} config
        stream (stm/stream)
        state  {:lines (atom [])
                :current-point (atom [])
                :current-line (atom {:tool nil :points []})
                :in-line? (atom false)}]
    (stm/consume #(process-event! state %) stream)
    (log logger :info ::init)
    {:stream stream
     :state state
     :logger logger
     :skatch (create-sketch state)}))

(defmethod ig/halt-key! ig-key [_ sys]
  (let [{:keys [stream logger skatch]} sys]
    (stm/close! stream)
    (.exit ^PApplet skatch)
    (log logger :info ::halt)))
