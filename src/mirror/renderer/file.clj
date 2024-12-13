(ns mirror.renderer.file
  (:require
    [clojure.spec.alpha :as spec]
    [duct.logger :refer [log]]
    [integrant.core :as ig]
    [manifold.stream :as stm]))


(def ig-key :mirror.renderer/file)

(derive ig-key :mirror/renderer)


(defn save-trace
  [file logger trace]
  (log logger :debug ::save-trace trace)
  (spit file (str trace "\n") :append true))


;; integrant methods


(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [filename logger]} config
        stream (stm/stream)
        sav-tr (partial save-trace filename logger)]
    (stm/consume sav-tr stream)
    (log logger :info ::init)
    {:stream stream
     :logger logger}))


(defmethod ig/halt-key! ig-key [_ sys]
  (let [{:keys [stream logger]} sys]
    (stm/close! stream)
    (log logger :info ::halt)))


(spec/def ::filename string?)
