;; -*- indent-tabs-mode: nil -*-

;; Note: checkers need to be exported in ../checkers.clj

(ns ^{:doc "Prepackaged functions that perform common checks."}
  midje.checkers.simple
  (:use [midje.checkers.defining :only [as-checker checker defchecker]]
  	[midje.checkers.extended-equality :only [extended-=]]
  	[midje.checkers.util :only [named-as-call]]
  	[midje.error-handling.exceptions :only [captured-throwable?]]
    [midje.util.ecosystem :only [clojure-1-3? +M -M *M]]
    [midje.util.form-utils :only [pred-cond regex? def-many-methods]]
    [midje.util.backwards-compatible-utils :only [every-pred-m some-fn-m]])
  (:import [midje.error_handling.exceptions ICapturedThrowable]))

(defchecker truthy 
  "Returns precisely true if actual is not nil and not false."
  [actual] 
  (and (not (captured-throwable? actual))
       (not (not actual))))
(def TRUTHY truthy)

(defchecker falsey 
  "Returns precisely true if actual is nil or false."
  [actual] 
  (not actual))
(def FALSEY falsey)

(defchecker anything
  "Accepts any value."
  [actual]
  (not (captured-throwable? actual)))
(def irrelevant anything)

(defchecker exactly
  "Checks for equality. Use to avoid default handling of functions."
  [expected]
    (named-as-call 'exactly expected
                   (checker [actual] (= expected actual))))

(letfn [(abs [n]
          (if (pos? n)
            n
            (-M n)))] ;; -M not strictly necessary, but...

  (defchecker roughly
    "With two arguments, accepts a value within delta of the
     expected value. With one argument, the delta is 1/1000th
     of the expected value."
    ([expected delta]
       (checker [actual]
         (and (>= expected (-M actual delta))
              (<= expected (+M actual delta)))))
    ([expected]
       (roughly expected (abs (*M 0.001 expected))))))


;; Concerning Throwables

(defmulti throws
  "Checks for a thrown Throwable. Argumentss can occur in any order and 
   in any number, except for the specified Throwable class: 
   messages (or regexes), predicates, or 0 or 1 Throwable classes.
   
   Ex. (fact (foo) => (throws IllegalArgumentException 
                              #(= bar (.getCause %))  
                              #(= baz (.getLocalizedMessage %))  
                              #\"^An exception described as: \"
                              #\"was thrown.$\"  ))"
  (fn [& args]
                   (set (for [arg args]
                          (pred-cond arg
                            fn?                        :predicate
                            (some-fn-m string? regex?) :message
                            class?                     :throwable )))))

(defmethod throws #{:message } [& expected-msgs]
  (checker [^ICapturedThrowable wrapped-throwable]
    (let [actual-msg (.getMessage ^Throwable (.throwable wrapped-throwable))]
      (every? (partial extended-= actual-msg) expected-msgs))))

(defmethod throws #{:predicate} [& preds]
  (checker [^ICapturedThrowable wrapped-throwable]
    ((apply every-pred-m preds) (.throwable wrapped-throwable))))

(defmethod throws #{:throwable} [clazz]
  (checker [^ICapturedThrowable wrapped-throwable]
    (= clazz (class (.throwable wrapped-throwable)))))

(def-many-methods throws [#{:throwable :predicate}, #{:message :predicate },
                          #{:throwable :message}, #{:throwable :message :predicate}] [& args]
  (as-checker (apply every-pred-m (map throws args))))