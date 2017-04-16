(ns matchmaker-sparql.evaluation.fold.setup
  "Setup a fold of evaluation run."
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.evaluation.setup :refer [assert-query]]
            [matchmaker-sparql.util :as util :refer [setup-template]]
            [sparclj.core :as sparql]
            [taoensso.timbre :as timbre]))

(defn load-matches
  "Load correct matches from `evaluation-graph` in a sequence
  of pairs of contracts and their awarded bidders."
  [{:keys [evaluation-graph]}]
  (letfn [(query-fn [[limit offset]]
            (setup-template "load_matches"
                            {:evaluation-graph evaluation-graph
                             :limit limit
                             :offset offset}))]
    (sequence
      (map (juxt :resource :match))
      (sparql/select-paged endpoint query-fn))))

(defn withdraw-matches
  "Withdraw a subset of matches to evaluate on."
  [{:keys [evaluation-graph graph]}
   {:keys [limit offset]}]
  (let [operation (setup-template "withdraw_matches"
                                  {:evaluation-graph evaluation-graph
                                   :graph graph
                                   :limit limit
                                   :offset offset})]
    (sparql/update-operation endpoint operation)))

(defn- are-matches-withdrawn?
  "Test if matches in the `evaluation-graph` are correctly withdrawn from the data in `graph`."
  [{:keys [evaluation-graph]}]
  (assert-query "are_matches_withdrawn"
                {:type ::util/matches-not-withdrawn}
                :data {:evaluation-graph evaluation-graph}))

(defn setup-fold
  "Setup a fold for evaluation."
  [evaluation limit-and-offset]
  (timbre/info "Setting up an evaluation fold...")
  (withdraw-matches evaluation limit-and-offset)
  (are-matches-withdrawn? evaluation)
  (load-matches evaluation))
