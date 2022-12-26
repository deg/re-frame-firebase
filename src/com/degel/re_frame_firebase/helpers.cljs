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
        on-failure (and on-failure (re-utils/event->fn on-failure))
        wrapped-handler (fn 
                          ([err] (cond (nil? err) (when on-success (on-success))
                                       on-failure (on-failure err)
                                       :else      ((core/default-error-handler) err)))

                          ;; I am unable to find in the Google Firebase documentation* a 2-arity
                          ;; callback for .set .update or .transaction that uses this wrapper. Yet, I've
                          ;; observed that such a callback exists specifically on .update.  With
                          ;; trepidation arising from minimal ad hoc testing, I am forwarding the second
                          ;; parameter, assuming that this behavior was undetected and inconsequential before
                          ;; I wrote wrapped-handler to be multi-arity.
                          ;;
                          ;; [TODO] Find the reason for this 2-arity version and properly dispatch it.
                          ;;
                          ;; * https://firebase.google.com/docs/reference/js/firebase.database.Reference
                          ([err other]
                           (cond (nil? err) (when on-success (on-success other))
                                 on-failure (on-failure err other)
                                 :else ((core/default-error-handler) err)))
                          
                          ;; onComplete invoked in :firebase/transaction and :firebase/swap accepts an
                          ;; error code, a boolean indicating committed status, and a snapshot of the
                          ;; data at that path.
                          ;;
                          ;; This is useful for exposing state changes upon completion of the
                          ;; transaction, as the transaction-update or f functions must be side-effect
                          ;; free.  Notably here, we reverse the order of committed and snapshot in the
                          ;; cljs versions on-success and on-failure.  So, if the on-success handler is
                          ;; a re-frame event vector (in iron.re-utils/re-utils they only take the first
                          ;; parameter), it gets the snapshotted data.  An on-failure event handler
                          ;; would get the error code; it has snapshot and committed reversed for
                          ;; continuity.
                          ([err committed snapshot]
                           (cond (nil? err) (when on-success (on-success (js->clj-tree snapshot) committed))
                                 on-failure (on-failure err (js->clj-tree snapshot) committed)
                                 :else ((core/default-error-handler) err))))]
    wrapped-handler))




