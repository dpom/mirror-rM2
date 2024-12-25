(ns mirror.system
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [duct.logger]
    [integrant.core :as ig]))


(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))


(defn config
  [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))


(defn prep
  [profile]
  (let [conf (config profile)]
    (ig/load-namespaces conf)
    conf))


(comment

  (config :dev)
  ;; => {:mirror/main
  ;;     {:tracer {:key :mirror/tracer},
  ;;      :renderer {:key :mirror/renderer},
  ;;      :logger {:key :duct/logger}},
  ;;     :mirror.tracer/file {:logger {:key :duct/logger}},
  ;;     :mirror.renderer/screen {:logger {:key :duct/logger}},
  ;;     [:duct/logger :duct.logger/timbre]
  ;;     {:level :debug,
  ;;      :appenders {:println {:key :duct.logger.timbre/println}}},
  ;;     :duct.logger.timbre/println {}}

  (prep :dev)
  ;; => {:mirror/main
  ;;     {:tracer {:key :mirror/tracer},
  ;;      :renderer {:key :mirror/renderer},
  ;;      :logger {:key :duct/logger}},
  ;;     :mirror.tracer/file {:logger {:key :duct/logger}},
  ;;     :mirror.renderer/screen {:logger {:key :duct/logger}},
  ;;     [:duct/logger :duct.logger/timbre]
  ;;     {:level :debug,
  ;;      :appenders {:println {:key :duct.logger.timbre/println}}},
  ;;     :duct.logger.timbre/println {}}
  
  ;;
  )
