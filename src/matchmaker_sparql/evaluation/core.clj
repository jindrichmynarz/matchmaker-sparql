(ns matchmaker-sparql.evaluation.core
  "Evaluation using n-fold cross validation."
  (:require [matchmaker-sparql.config :refer [config]]
            [matchmaker-sparql.evaluation.setup :as setup]
            [matchmaker-sparql.evaluation.teardown :as teardown]
            [matchmaker-sparql.evaluation.fold.core :as fold]
            [matchmaker-sparql.evaluation.metrics :as metrics]
            [taoensso.timbre :as timbre]))

(defn run-evaluation
  []
  (timbre/info "Starting evaluation...")
  (let [{:keys [evaluation]} config
        {:keys [limits-and-offsets]
         :as evaluation-params} (setup/setup-evaluation evaluation)]
    (try (->> limits-and-offsets
              (map (partial fold/run-fold evaluation-params))
              (metrics/aggregate-evaluation-metrics evaluation-params))
         (finally (teardown/teardown-evaluation evaluation-params)))))
