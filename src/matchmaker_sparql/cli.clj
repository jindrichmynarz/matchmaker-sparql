(ns matchmaker-sparql.cli
  (:gen-class)
  (:require [matchmaker-sparql.spec :as spec]
            [matchmaker-sparql.util :as util]
            [matchmaker-sparql.config :refer [config]]
            [matchmaker-sparql.evaluation.core :as evaluation]
            [clojure.spec :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [slingshot.slingshot :refer [try+]]
            [sparclj.core :as sparql]
            [taoensso.timbre :as timbre]))

; ----- Private functions -----

(defn- usage
  [summary]
  (util/join-lines ["SPARQL-based matchmaking of bidders to public contracts"
                    ""
                    "Usage: matchmaker_sparql [options]"
                    ""
                    "Options:"
                    summary]))

(defn- error-msg
  [errors]
  (util/join-lines (cons "The following errors occurred while parsing your command:\n" errors)))

(defn- validate-params
  [params]
  (when-not (s/valid? ::spec/params params)
    (util/die (str "The provided arguments are invalid.\n\n"
                   (s/explain-str ::spec/params params)))))

(defn- main
  [params]
  (validate-params params)
  (try+ (mount/start-with-args params)
        (catch [:type ::util/invalid-config] {:keys [message]}
          (util/die (format "Invalid configuration:\n %s" message)))
        (catch [:type ::sparql/endpoint-not-found] _
          (util/die (format "SPARQL endpoint <%s> was not found."
                            (get-in config [:sparql ::sparql/url])))))
  (try+ (let [output (str "results_" (util/uuid) ".edn")
              results (evaluation/run-evaluation)]
          (spit output (pr-str (assoc results :config config)))
          (timbre/info (format "Evaluation results saved to %s." output)))
        (catch [:type ::util/data-empty] _
          (util/die "Data is empty!"))
        (catch [:type ::util/blank-nodes-present] _
          (util/die "Blank nodes detected!"))
        (catch [:type ::util/duplicate-tenders] _
          (util/die "Duplicate tenders detected!"))
        (catch [:type ::util/matches-not-withdrawn] _
          (util/die "Matches not all withdrawn!"))
        (catch [:type ::util/iri-rank-undefined] _
          (util/die "You need to setup IRI_RANK in Virtuoso!")))
  (shutdown-agents))

; ----- Private vars -----

(def ^:private cli-options
  [["-c" "--config CONFIG" "Matchmaker's configuration"
    :id ::spec/config-file
    :parse-fn io/as-file
    :validate [util/file-exists? "The configuration file does not exist."]]
   ["-h" "--help" "Display help information"
    :id ::spec/help?]])

; ----- Public functions -----

(defn -main
  [& args]
  (let [{{::spec/keys [help?]
          :as params} :options
         :keys [errors summary]} (parse-opts args cli-options)]
    (cond help? (util/info (usage summary))
          errors (util/die (error-msg errors))
          :else (main params))))
