(defproject matchmaker-sparql "0.1.0-SNAPSHOT"
  :description "SPARQL-based matchmaking of bidders to public contracts"
  :url "http://github.com/jindrichmynarz/matchmaker-sparql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/tools.cli "0.3.5"]
                 [sparclj "0.1.6"]
                 [mount "0.1.11"]
                 [stencil "0.5.0"]
                 [slingshot "0.12.2"]
                 [com.taoensso/timbre "4.8.0"]]
  :main matchmaker-sparql.cli
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]
                   :plugins [[lein-binplus "0.4.2"]]}
             :uberjar {:aot :all
                       :uberjar-name "matchmaker_sparql.jar"}}
  :bin {:name "matchmaker_sparql"})
