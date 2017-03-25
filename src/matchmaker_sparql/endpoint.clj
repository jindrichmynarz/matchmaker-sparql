(ns matchmaker-sparql.endpoint
  (:require [matchmaker-sparql.config :refer [config]]
            [mount.core :as mount :refer [defstate]]
            [sparclj.core :as sparql]))

(defstate endpoint
  :start (sparql/init-endpoint (:endpoint config)))
