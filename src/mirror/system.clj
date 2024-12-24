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
  (aero/read-config (io/resource "mirror/config.edn") {:profile profile}))


(defn prep
  [profile]
  (let [conf (config profile)]
    (ig/load-namespaces conf)
    config))
