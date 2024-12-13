(ns mirror.tracer.file
  "Use a file as tracer"
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]
    [duct.logger :refer [log]]
    [integrant.core :as ig]
    [manifold.stream :as stm]))


(def ig-key :mirror.tracer/file)

(derive ig-key :mirror/tracer)


(defn process-file!
  "Run the side-effect function on each line of the file"
  [filename func]
  (with-open [rdr (io/reader filename)]
    (doseq [line (line-seq rdr)]
      (func line))))


(defn get-traces
  [stream line]
  (stm/put! stream (edn/read-string line)))


(defn trace!
  "Use FILENAME file as trace source for the TRACER"
  [tracer filename]
  (process-file! filename (partial get-traces (:stream tracer))))


;; integrant methods

(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [logger]} config
        stream (stm/stream)]
    (log logger :info ::init)
    {:stream stream
     :logger logger}))


(defmethod ig/halt-key! ig-key [_ sys]
  (let [{:keys [stream logger]} sys]
    (stm/close! stream)
    (log logger :info ::halt)))


(spec/def ::filename string?)
