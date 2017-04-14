(ns matchmaker-sparql.evaluation.teardown
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.config :refer [config]]
            [matchmaker-sparql.util :refer [teardown-template]]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]
            [taoensso.timbre :as timbre]))

(defn return-withheld-data
  "Return pc:awardedTender links from `withheld-graph` back to source graph."
  [withheld-graph]
  (let [{{:keys [graph]} :data} config
        update-operation (teardown-template "return_withheld_data"
                                            {:graph graph
                                             :withheld-graph withheld-graph})]
    (timbre/info "Returning reduced data...")
    (sparql/update-operation endpoint update-operation)))

(defn- withheld-data-empty?
  "Test if the `withheld-graph` is empty (after returning the withheld data)."
  [withheld-graph]
  (let [query (teardown-template "withheld_data_empty" {:withheld-graph withheld-graph})]
    (assert (not (sparql/ask-query endpoint query)) "Withheld data not empty!")))

(defn teardown-evaluation
  [{:keys [data-reduced? withheld-graph]}]
  (when data-reduced?
    (return-withheld-data withheld-graph)
    (withheld-data-empty? withheld-graph)))
