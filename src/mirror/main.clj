(ns mirror.main
  (:gen-class)
  (:require
    [duct.logger :refer [log]]
    [integrant.core :as ig]
    [manifold.stream :as stm]
    [mirror.system :as sys]))


(def ig-key :mirror/main)


(defmethod ig/init-key ig-key [_ config]
  (let [{:keys [tracer renderer logger]} config
        s1 (:stream tracer)
        s2 (:stream renderer)]
    (log logger :info ::init)
    (stm/connect s1 s2)
    config))


(defmethod ig/halt-key! ig-key [_ sys]
  (log (:logger sys) :info ::halt))


(defn -main
  "Mirror entry point"
  []
  (let [system (-> :prod
                   sys/prep
                   ig/init)
        logger (get system [ig-key :logger])]
    (log logger :info ::main)))
