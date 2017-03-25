(ns matchmaker-sparql.core
  (:require [matchmaker-sparql.endpoint :refer [endpoint]]
            [matchmaker-sparql.config :refer [config]]
            [sparclj.core :as sparql]
            [stencil.core :as stencil]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.string :as string]))

(defn- kind->template
  "Convert matchmaker's kind to template path."
  [kind]
  (str "templates/matchmaker/contract/bidder/" (string/replace (name kind) #"-" "_")))

(defn match-contract
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
                    :contract contract)]
    (try+ (map :match (sparql/select-template endpoint template data))
          ; Virtuoso tends to throw HTTP 404 if under load, so we retry.
          (catch [:type ::sparql/endpoint-not-found] exception
            (if (< retries max-retries)
              (do (Thread/sleep (+ (* retries 1000) 1000))
                  (match-contract contract :retries (inc retries)))
              (throw+ exception))))))
