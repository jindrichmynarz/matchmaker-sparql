(ns matchmaker-sparql.config
  (:require [matchmaker-sparql.spec :as spec]
            [matchmaker-sparql.util :as util]
            [mount.core :refer [defstate] :as mount]
            [clojure.edn :as edn]
            [clojure.spec :as s]
            [slingshot.slingshot :refer [throw+]]))

(defn validate-config
  "Validate parsed configuration according to its specification."
  [parsed-config]
  (if (s/valid? ::spec/config parsed-config)
    parsed-config
    (throw+ {:type ::util/invalid-config
             :message (str "The provided configuration is invalid.\n\n"
                           (s/explain-str ::spec/config parsed-config))})))

(defn load-config
  "Read configuration from file."
  [{path ::spec/config-file}]
  (-> path slurp edn/read-string validate-config))

(defstate config
  :start (load-config (mount/args)))
