(ns matchmaker-sparql.spec
  (:require [clojure.spec :as s]
            [sparclj.core :as sparql]
            [sparclj.spec :as sparql-spec])
  (:import (java.io File)))

; Generic specs

(s/def ::file (partial instance? File))

(s/def ::non-negative-int (s/and integer? (complement neg?)))

(s/def ::positive-int (s/and integer? pos?))

(s/def ::weight (s/and number? pos? (partial >= 1)))

(s/def ::in ::weight)

(s/def ::out ::weight)

(s/def ::inhibition (s/keys :opt-un [::in ::out]))

; Configuration

(s/def ::additional-object-inhibition ::inhibition)

(s/def ::graph ::sparql-spec/iri-urn)

(s/def ::data (s/keys :req-un [::graph]))

(s/def ::data-reduction ::weight)

(s/def ::folds ::positive-int)

(s/def ::inferred-object-inhibition ::weight)

(s/def ::kind #{:exact-cpv
                :exact-cpv-and-nace
                :exact-cpv-goedel
                :exact-cpv-additional-object
                :exact-cpv-with-qualifier
                :exact-cpv-additional-object-with-qualifier
                :exact-cpv-with-zindex
                :exact-cpv-with-idf
                :exact-cpv-only-open-procedure
                :expand-to-broader-cpv
                :expand-to-broader-cpv-with-idf
                :expand-to-narrower-cpv
                :expand-to-narrower-cpv-with-idf
                :expand-to-cpv-bidirectional
                :expand-to-cpv-bidirectional-with-idf
                :kind
                :random
                :service-category
                :top-page-rank-bidders
                :top-winning-bidders
                :additional-object
                :nace
                :exact-cpv-and-kind
                :exact-cpv-and-service-category
                :exact-cpv-additional-object-kind-and-service-category
                :exact-cpv-lukasiewicz})

(s/def ::limit ::positive-int)

(s/def ::hops-to-broader ::non-negative-int)

(s/def ::hops-to-narrower ::non-negative-int)

(s/def ::kind-inhibition ::weight)

(s/def ::main-object-inhibition ::weight)

(s/def ::qualifier-inhibition ::inhibition)

(s/def ::query-expansion (s/keys :opt-un [::hops-to-broader ::hops-to-narrower]))

(s/def ::service-category-inhibition ::weight)

(s/def ::matchmaker (s/keys :req-un [::kind]
                            :opt-un [::additional-object-inhibition ::inferred-object-inhibition
                                     ::kind-inhibition ::limit
                                     ::main-object-inhibition
                                     ::qualifier-inhibition ::query-expansion
                                     ::service-category-inhibition]))

(s/def ::windows ::positive-int)

(s/def ::evaluation (s/keys :req-un [::folds]
                            :opt-un [::data-reduction ::sparql/parallel? ::windows]))

(s/def ::config (s/keys :req-un [::evaluation ::data ::sparql/endpoint ::matchmaker]))

; CLI parameters

(s/def ::config-file ::file)

(s/def ::help? true?)

(s/def ::params (s/keys :req [::config-file]
                        :opt [::help?]))
