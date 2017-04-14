(ns matchmaker-sparql.evaluation.fold.teardown
  "Tear down a fold of evaluation run."
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.util :refer [teardown-template]]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]
            [taoensso.timbre :as timbre]))

(defn- evaluation-data-empty?
  "Test if the `evaluation-graph` is empty."
  [evaluation-graph]
  (let [query (teardown-template "evaluation_data_empty" {:evaluation-graph evaluation-graph})]
    (assert (not (sparql/ask-query endpoint query)) "Evaluation data not empty!")))

(defn return-matches
  "Return matches used in evaluation to the source `graph`."
  [{:keys [evaluation-graph graph]}]
  (timbre/info "Tearing down an evaluation fold...")
  (let [operation (teardown-template "return_matches"
                                     {:evaluation-graph evaluation-graph
                                      :graph graph})]
    (sparql/update-operation endpoint operation)
    (evaluation-data-empty? evaluation-graph)))
