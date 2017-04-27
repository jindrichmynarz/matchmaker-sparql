(ns matchmaker-sparql.evaluation.metrics
  (:require [clojure.set :refer [union]]
            [clojure.spec :as s]
            [sparclj.core :as sparql]
            [taoensso.timbre :as timbre]))

; ----- Specs -----

; ----- Generic specs -----

(s/def ::fraction (s/and number? #(<= 0 % 1)))

(s/def ::positive-integer (s/and integer? pos?))

; ----- Concrete specs -----

(s/def ::empty? boolean?)

(s/def ::empties (s/coll-of ::empty?))

(s/def ::matches (s/map-of ::sparql/iri ::positive-integer))

(s/def ::rank (s/or :position ::positive-integer
                    :not-found #{:not-found}))

(s/def ::ranks (s/coll-of ::rank))

(s/def ::metrics (s/keys :req-un [::rank ::empty? ::matches]))

(s/fdef compute-metrics
        :args (s/cat :match ::sparql/iri
                     :matches (s/coll-of ::sparql/iri :distinct true))
        :ret ::metrics)
(defn compute-metrics
  "Compute metrics of `matches` given correct `match`."
  [match matches]
  (let [rank (.indexOf matches match)]
    {:rank (if (not= rank -1) (inc rank) :not-found)
     :empty? (not (seq matches))
     :matches (zipmap matches (repeat 1))}))

(s/fdef union-matches
        :args (s/cat :metrics (s/coll-of ::metrics))
        :ret ::matches)
(defn- union-matches
  "Set union matches."
  [metrics]
  (transduce (map :matches) (partial merge-with +) metrics))

(s/fdef aggregate-fold-metrics
        :args (s/cat :metrics (s/coll-of ::metrics))
        :ret (s/keys :req-un [::matches ::empties ::ranks]))
(defn aggregate-fold-metrics
  "Compute aggregate metrics for an evaluation fold."
  [metrics]
  {:matches (union-matches metrics)
   :empties (sequence (map :empty?) metrics) ; FIXME: Needless double iteration.
   :ranks (sequence (map :rank) metrics)})

(s/def ::bidder-count ::positive-integer)

(s/def ::contract-count ::positive-integer)

(s/def ::long-tail? (s/fspec :args (s/cat :bidder ::sparql/iri)
                             :ret boolean?))

(s/def ::evaluation (s/keys :req-un [::bidder-count ::contract-count ::long-tail?]))

(s/def ::catalog-coverage ::fraction)

(s/fdef aggregate-evaluation-metrics
        :args (s/cat :evaluation ::evaluation
                     :metrics (s/coll-of ::metrics))
        :ret (s/keys :req-un [::catalog-coverage ::long-tail-coverage
                              ::predication-coverage ::ranks]))
(defn aggregate-evaluation-metrics
  "Compute aggregate metrics for the whole evaluation."
  [{:keys [bidder-count contract-count long-tail?]}
   metrics]
  (let [matches (union-matches metrics)
        matches-count (transduce (map val) + matches)
        long-tail-matches-count (->> matches
                                     (filter (comp long-tail? key))
                                     (transduce (map val) +))]
    {:catalog-coverage (/ (count matches) bidder-count)
     :long-tail-percentage (/ long-tail-matches-count matches-count)
     :prediction-coverage (/ (count (filter false? (mapcat :empties metrics))) contract-count)
     :ranks (mapcat :ranks metrics)}))
