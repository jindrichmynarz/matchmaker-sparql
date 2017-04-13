(ns matchmaker-sparql.evaluation.setup-test
  (:require [matchmaker-sparql.evaluation.setup :as setup]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]))

(defspec split-to-ints-sum
  ; Sum of splits is equal to sample size.
  100
  (prop/for-all [sample-size gen/s-pos-int
                 split-count gen/s-pos-int]
                (= (apply + (setup/split-to-ints sample-size split-count))
                   sample-size)))

(defspec split-to-ints-count
  ; The number of splits is as given by split-count.
  100
  (prop/for-all [sample-size gen/s-pos-int
                 split-count gen/s-pos-int]
                (= (count (setup/split-to-ints sample-size split-count))
                   split-count)))

(defspec get-splits-sum
  ; Sum of the split sizes is equal to the sample size * reduction ratio.
  100
  (prop/for-all [sample-size gen/s-pos-int
                 split-count gen/s-pos-int
                 reduction-ratio (gen/fmap (partial / 1) gen/s-pos-int)]
                (= (apply + (map :limit (setup/get-splits sample-size split-count reduction-ratio)))
                   (int (Math/ceil (* (- 1 reduction-ratio) sample-size))))))
