(ns matchmaker-sparql.core
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.config :refer [config]]
            [matchmaker-sparql.util :as util]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.string :as string]))

(defn- kind->template
  "Convert matchmaker's kind to template path."
  [kind]
  (str "templates/matchmaker/contract/bidder/" (string/replace (name kind) #"-" "_")))

(defn- match-contract'
  "Match `contract` to bidders using the `query`."
  [query]
  (map :match (sparql/select-query endpoint query)))

(def match-contract-memoized
  (memoize match-contract'))

(defn match-contract
  "Match `contract` to bidders.
  Potentially retries if it encounters HTTP 404."
  [contract & {:keys [retries]
               :or {retries 0}}]
  (let [{{:keys [kind]
          :as params} :matchmaker
         {:keys [graph]} :data
         {::sparql/keys [max-retries]
          :or {max-retries 5}} :endpoint} config
        template (kind->template kind)
        data (assoc params
                    :graph graph
                    :contract contract)
        query (stencil/render-file template data)]
    (try+ (match-contract-memoized query)
          ; Virtuoso tends to throw HTTP 404 if under load, so we retry.
          (catch [:type ::sparql/endpoint-not-found] exception
            (if (< retries max-retries)
              (do (Thread/sleep (+ (* retries 1000) 1000))
                  (match-contract contract :retries (inc retries)))
              (throw+ exception)))
          (catch [:status 500] {:keys [body]
                                :as exception}
            (if (string/includes? body "Undefined procedure DB.DBA.IRI_RANK")
              (throw+ {:type ::util/iri-rank-undefined})
              (throw+ exception))))))
