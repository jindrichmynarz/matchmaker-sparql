(ns matchmaker-sparql.evaluation.fold.core
  (:require [matchmaker-sparql.evaluation.fold.setup :as setup]
            [matchmaker-sparql.evaluation.fold.teardown :as teardown]
            [matchmaker-sparql.evaluation.metrics :as metrics]
            [matchmaker-sparql.core :as core]
            [sparclj.core :as sparql]
            [taoensso.timbre :as timbre]))

(defn run-fold
  "Run evaluation of one fold delimited by `limit-and-offset` ([limit offset])."
  [{::sparql/keys [parallel?]
    :as evaluation}
   limit-and-offset]
  (try (let [map-fn (if parallel? pmap map)
             fold (setup/setup-fold evaluation limit-and-offset)
             evaluate-fn (fn [[contract match]]
                           (metrics/compute-metrics match (core/match-contract contract)))]
         (timbre/info "Running evaluation on a fold...")
         (metrics/aggregate-fold-metrics (map-fn evaluate-fn fold)))
       (finally (teardown/return-matches evaluation))))
