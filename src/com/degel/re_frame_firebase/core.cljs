;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.core
  (:require
   [iron.re-utils :refer [<sub >evt event->fn sub->fn]]
   ["@firebase/app" :refer (initializeApp)]))

;;; Used mostly to register client handlers
(defonce firebase-state (atom {}))

(defn set-firebase-state [& {:keys [get-user-sub set-user-event default-error-handler]}]
  (swap! firebase-state assoc
         :set-user-fn           (event->fn set-user-event)
         :get-user-fn           (sub->fn get-user-sub)
         :default-error-handler (event->fn (or default-error-handler js/alert))))

(defn initialize-app [firebase-app-info]
  (swap! firebase-state assoc
         :app (initializeApp (clj->js firebase-app-info))))

;;; [TODO] Consider adding a default atom to hold the user state when :get-user-fn and
;;; and :set-user-fn are not defined. Need to do this carefully, so as not to cause any
;;; surprises for users who accidentally defined just one of the two callbacks.
(defn current-user []
  (when-let [handler (:get-user-fn @firebase-state)]
    (handler)))

(defn set-current-user [user]
  (when-let [handler (:set-user-fn @firebase-state)]
    (handler user)))

(defn default-error-handler []
  (:default-error-handler @firebase-state))
