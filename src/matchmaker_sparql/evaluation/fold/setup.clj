(ns matchmaker-sparql.evaluation.fold.setup
  "Setup a fold of evaluation run."
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]
            [taoensso.timbre :as timbre]))

(defn load-matches
  "Load correct matches from `evaluation-graph` in a sequence
  of pairs of contracts and their awarded bidders."
  [{:keys [evaluation-graph]}]
  (letfn [(query-fn [[limit offset]]
            (stencil/render-file "templates/evaluation/setup/load_matches"
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
  (let [operation (stencil/render-file "templates/evaluation/setup/withdraw_matches"
                                       {:evaluation-graph evaluation-graph
                                        :graph graph
                                        :limit limit
                                        :offset offset})]
    (sparql/update-operation endpoint operation)))

(defn setup-fold
  "Setup a fold for evaluation."
  [evaluation limit-and-offset]
  (timbre/info "Setting up an evaluation fold...")
  (withdraw-matches evaluation limit-and-offset)
  (load-matches evaluation))
