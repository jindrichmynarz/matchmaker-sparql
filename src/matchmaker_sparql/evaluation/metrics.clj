(ns matchmaker-sparql.evaluation.metrics
  (:require [clojure.set :refer [union]]
            [taoensso.timbre :as timbre]))

(defn compute-metrics
  "Compute metrics of `matches` given correct `match`."
  [match matches]
  (let [rank (.indexOf matches match)]
    {:rank (if (not= rank -1) (inc rank) :not-found)
     :empty? (not (seq matches))
     :matches (set matches)}))

(defn- union-matches
  "Set union matches."
  [metrics]
  (transduce (map :matches) union metrics))

(defn aggregate-fold-metrics
  "Compute aggregate metrics for an evaluation fold."
  [metrics]
  {:matches (union-matches metrics)
   :empties (sequence (map :empty?) metrics) ; FIXME: Needless double iteration.
   :ranks (sequence (map :rank) metrics)})

(defn aggregate-evaluation-metrics
  "Compute aggregate metrics for the whole evaluation."
  [{:keys [bidder-count contract-count long-tail?]}
   metrics]
  (let [matches (union-matches metrics)
        matches-count (count matches)]
    {:catalog-coverage (/ matches-count bidder-count)
     :long-tail-percentage (/ (count (filter long-tail? matches)) matches-count)
     :prediction-coverage (/ (count (filter false? (mapcat :empties metrics))) contract-count)
     :ranks (mapcat :ranks metrics)}))
