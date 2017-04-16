(ns matchmaker-sparql.evaluation.setup
  "Setup a whole evaluation run."
  (:require [matchmaker-sparql.config :refer [config]]
            [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.util :as util :refer [setup-template]]
            [sparclj.core :as sparql]
            [taoensso.timbre :as timbre]
            [slingshot.slingshot :refer [throw+]]))

(defn- has-contracts-with-multiple-winners?
  "Test if there are contracts awarded to multiple bidders."
  []
  (let [{{:keys [graph]} :data} config
        query (setup-template "has_contracts_with_multiple_winners" {:graph graph})]
    (sparql/ask-query endpoint query)))

(defn- clear-evaluation-graph
  "CLEAR GRAPH `evaluation-graph`."
  [{:keys [evaluation-graph]}]
  (let [update-operation (setup-template "clear_evaluation_graph" {:evaluation-graph evaluation-graph})]
    (sparql/update-operation endpoint update-operation)))

(defn- delete-multiple-awards
  "Delete contracts awarded to multiple bidders."
  []
  (let [{{:keys [graph]} :data} config
        update-operation (setup-template "delete_multiple_awards" {:graph graph})]
    (sparql/update-operation endpoint update-operation)))

(defn split-to-ints
  "Split `sample-size` into `split-count` of integer-sized splits."
  [sample-size split-count]
  (let [to-increment (mod sample-size split-count)]
    (map-indexed (fn [index size] (if (< index to-increment) (inc size) size))
                 (repeat split-count (int (/ sample-size split-count))))))

(defn get-splits
  "Split `contract-count` into `split-count` splits, without which
  the `contract-count` = (* `contract-count` `data-reduction`)"
  [contract-count split-count data-reduction]
  (let [window-sizes (split-to-ints contract-count split-count)
        windows (map (fn [offset limit]
                       {:limit limit
                        :offset offset})
                     (reductions + 0 window-sizes)
                     window-sizes)
        reduction-size (->> contract-count
                            (* (- 1 data-reduction))
                            Math/ceil
                            int)
        split-sizes (split-to-ints reduction-size split-count)]
    (map (fn [{:keys [limit offset]} split-limit decrease]
           {:limit split-limit
            :offset (-> limit
                        (- split-limit)
                        rand-int
                        (+ offset)
                        (- decrease))})
         windows
         split-sizes
         (reductions + 0 split-sizes))))

(defn- reduce-data
  "Reduce the available contract awards (`contract-count`)
  by `data-reduction` ratio from (0, 1].
  Contract awards are moved temporarily from `graph` into the `withheld-graph`.
  Withheld contract awards are randomized by splitting their count
  into number of `windows`."
  [{:keys [contract-count data-reduction graph windows withheld-graph]
    :or {windows 25}}]
  (timbre/info "Reducing data...")
  (let [splits (get-splits contract-count windows data-reduction)]
    (doseq [{:keys [limit offset]} splits
            :let [update-operation (setup-template "reduce_data"
                                                   {:limit limit
                                                    :offset offset
                                                    :graph graph
                                                    :withheld-graph withheld-graph})]]
      (sparql/update-operation endpoint update-operation))))

(defn- count-query
  "Run a SPARQL query that produces a single ?count."
  [template]
  (let [{{:keys [graph]} :data} config]
    (-> endpoint
        (sparql/select-template template {:graph graph})
        first
        :count)))

(defn count-awarded-contracts
  []
  (count-query "templates/evaluation/setup/count_contracts"))

(defn count-bidders
  []
  (count-query "templates/evaluation/setup/count_bidders"))

(defn- fold-limits-and-offsets
  "Returns a sequence of limits and offsets delimiting the evaluation folds.
  Takes number of `folds` and `contract-count`."
  [folds contract-count]
  (let [; int rounds down sample sizes (limits)
        basic-limits (repeat folds (int (/ contract-count folds)))
        ; First samples will be incremented by 1 to cover the complete dataset.
        incremented-samples (mod contract-count folds)
        sample-limits (concat (map inc (take incremented-samples basic-limits))
                              (take-last (- folds incremented-samples) basic-limits))]
    (map (fn [limit offset] {:limit limit :offset offset})
         sample-limits
         (conj (butlast (reductions + sample-limits)) 0))))

(defn assert-query
  "Assert that the result of the SPARQL ASK query rendered from `template`
  must satisfy the `assertion` (false? by default)."
  [template
   exception
   & {:keys [assertion data] :or {assertion false?}}]
  (let [{{:keys [graph]} :data} config
        query (setup-template template (merge {:graph graph} data))]
    (when-not (assertion (sparql/ask-query endpoint query))
      (throw+ exception))))

(defn- are-blank-nodes-present?
  "Test if there are no blank nodes in the evaluated data."
  []
  (assert-query "are_blank_nodes_present" {:type ::util/blank-nodes-present}))

(defn- data-empty?
  "Test if the data is not empty."
  []
  (assert-query "data_empty" {:type ::util/data-empty} :assertion true?))

(defn- evaluation-data-empty?
  "Test if the evaluation graph is empty before loading it."
  [{:keys [evaluation-graph]}]
  (assert-query "evaluation_data_empty"
                {:type ::util/evaluation-data-empty}
                :data {:evaluation-graph evaluation-graph}))

(defn- has-duplicate-tenders?
  "Test if there are duplicate tenders in the evaluated data."
  []
  (assert-query "has_duplicate_tenders" {:type ::util/duplicate-tenders}))

(defn- test-data-assumptions
  "Test assumptions about the evaluated data."
  []
  (data-empty?)
  (are-blank-nodes-present?)
  (has-duplicate-tenders?))

(defn setup-evaluation
  "Setup data for evaluation."
  [{:keys [data-reduction folds]
    :as args}]
  (let [{{:keys [graph]} :data} config
        withheld-graph (str graph "/withheld")
        evaluation-graph (str graph "/evaluation")
        contract-count (count-awarded-contracts)
        data-reduced? (and data-reduction (< data-reduction 1))
        evaluation (assoc args
                          :contract-count contract-count
                          :data-reduced? data-reduced?
                          :evaluation-graph evaluation-graph
                          :graph graph
                          :withheld-graph withheld-graph)]
    (test-data-assumptions)
    (when-not (evaluation-data-empty? evaluation)
      (timbre/info "Cleaning non-empty evaluation graph...")
      (clear-evaluation-graph evaluation))
    (when (has-contracts-with-multiple-winners?)
      (timbre/info "Deleting contracts with multiple awards...")
      (delete-multiple-awards))
    ; Reduce data when required.
    (when data-reduced? (reduce-data evaluation))
    ; Re-COUNT contracts if data was reduced.
    (let [contract-count' (if data-reduced? (count-awarded-contracts) contract-count)
          bidder-count (count-bidders)
          limits-and-offsets (fold-limits-and-offsets folds contract-count')]
      (assoc evaluation
             :bidder-count bidder-count
             :contract-count contract-count'
             :limits-and-offsets limits-and-offsets))))
