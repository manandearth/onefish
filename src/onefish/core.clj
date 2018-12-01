(ns onefish.core
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check]
            [clojure.spec.test.alpha :as stest])) ;;for instrument

;;from carin Meier blog post about spec:
;; http://gigasquidsoftware.com/blog/2016/05/29/one-fish-spec-fish/

;a map the keys are ints the vals strings
(def fish-numbers {0 "zero"
                   1 "one"
                   2 "two"})

;spec'ed numbers
(s/def ::fish-number (set (keys fish-numbers)))

(s/valid? ::fish-number 1)
(s/explain ::fish-number 5)  ; prints explenation to repl

;;set as predicate
(s/def ::color #{"Red" "Blue" "Dun"})

(s/valid? ::color "Red")


;concatenating predicates stage 1:
(s/def ::first-line (s/cat :n1 ::fish-number :n2 ::fish-number :c1 ::color :c2 ::color))

(s/explain ::first-line [1 2 "Red" "Dun"])


;;and stage two:
;;making sure the keys inc by one
(defn one-bigger?
  [{:keys [n1 n2]}]
  (= n2 (inc n1)))
;;and making sure the values don't repeat
(s/def ::first-line (s/and (s/cat :n1 ::fish-number :n2 ::fish-number :c1 ::color :c2 ::color)
                           one-bigger?
                           #(not= (:c1 %) (:c2 %))))

(s/valid? ::first-line [1 2 "Red" "Blue"]) ;;=> true


;;s/conform gives back the destructed values:
(s/conform ::first-line [1 2 "Red" "Blue"])
;; {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}

;;generating data
;;this is why the test.check library is included
(s/exercise ::first-line)
;;([(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}]
;;[(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;[(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;[(1 2 "Red" "Dun") {:n1 1, :n2 2, :c1 "Red", :c2 "Dun"}]
;;[(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}]
;;[(1 2 "Dun" "Red") {:n1 1, :n2 2, :c1 "Dun", :c2 "Red"}]
;;[(1 2 "Dun" "Red") {:n1 1, :n2 2, :c1 "Dun", :c2 "Red"}]
;;[(0 1 "Red" "Blue") {:n1 0, :n2 1, :c1 "Red", :c2 "Blue"}]
;;[(0 1 "Dun" "Blue") {:n1 0, :n2 1, :c1 "Dun", :c2 "Blue"}]
;;[(0 1 "Blue" "Red") {:n1 0, :n2 1, :c1 "Blue", :c2 "Red"}]
;)

;;Although, it meets our criteria, it’s missing one essential ingredient – rhyming!

;;Let’s fix this by adding an extra predicate number-rhymes-with-color?

(defn fish-name-rhymes-with-number? [{n :n2 c :c2}]
(or
 (= [n c] [2 "Blue"])
 (= [n c] [1 "Dun"])))


(s/def ::first-line (s/and (s/cat :n1 ::fish-number :n2 ::fish-number :c1 ::color :c2 ::color)
                           one-bigger?
                           fish-name-rhymes-with-number?
                           #(not= (:c1 %) (:c2 %))))

(s/exercise ::first-line 5)
;;([(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;;[(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;;[(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;;[(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;;[(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}]
;)

;;failing a test for not rhyming..
(s/explain ::first-line [1 2 "Red" "Dun"])
;;val: {:n1 1, :n2 2, :c1 "Red", :c2 "Dun"} fails spec: :onefish.core/first-line predicate: fish-name-rhymes-with-number?


;;Creating a function that creates a string for the poem
;;it will be tested the the spec.test instrument function
(defn fish-line
 [n1 n2 c1 c2]
  (clojure.string/join " "
   (map #(str % " fish.")
        [(get fish-numbers n1)
         (get fish-numbers n2)
         c1
         c2])))

(s/fdef fish-line
        :args ::first-line
        :ret string?)


(stest/instrument 'fish-line)

(fish-line 1 2 "Red" "Blue")
;;"one fish. two fish. Red fish. Blue fish."


;;TODO this does't seem to throw an error!
(fish-line 2 1 "Red" "Blue")
