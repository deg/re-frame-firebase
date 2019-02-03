;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.helpers
  (:require
   [clojure.spec.alpha :as s]
   [iron.re-utils :as re-utils]
   [iron.utils :as utils]
   [com.degel.re-frame-firebase.core :as core]))


;;; Helper functions that straddle the line between this library and Iron
;;; utils. These may move, change, or be abandoned, as I get more comfortable
;;; with them.


(defn js->clj-tree [x]
  (-> (.val x)
      js->clj
      clojure.walk/keywordize-keys))


(defn promise-wrapper [promise on-success on-failure]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) on-success)
         (utils/validate (s/nilable :re-frame/vec-or-fn) on-failure)]}
  (when on-success
    (.then promise (re-utils/event->fn on-success)))
  (if on-failure
    (.catch promise (re-utils/event->fn on-failure))
    (.catch promise (core/default-error-handler))))


(defn success-failure-wrapper [on-success on-failure]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) on-success)
         (utils/validate (s/nilable :re-frame/vec-or-fn) on-failure)]
   :post (fn? %)}
  (let [on-success (and on-success (re-utils/event->fn on-success))
        on-failure (and on-failure (re-utils/event->fn on-failure))]
    (fn [err]
      (cond (nil? err) (when on-success (on-success))
            on-failure (on-failure err)
            :else      ((core/default-error-handler) err)))))
