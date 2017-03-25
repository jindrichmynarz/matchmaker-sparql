(ns matchmaker-sparql.evaluation.teardown
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.config :refer [config]]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]))

(defn return-withheld-data
  "Return pc:awardedTender links from `withheld-graph` back to source graph."
  [withheld-graph]
  (let [{{:keys [graph]} :data} config
        update-operation (stencil/render-file "templates/evaluation/teardown/return_withheld_data"
                                              {:graph graph
                                               :withheld-graph withheld-graph})]
    (sparql/update-operation endpoint update-operation)))

(defn teardown-evaluation
  [{:keys [data-reduced? withheld-graph]}]
  (when data-reduced? (return-withheld-data withheld-graph)))
