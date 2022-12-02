(ns com.degel.re-frame-firebase.storage
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.ratom :as ratom :refer [make-reaction]]
   [iron.re-utils :as re-utils :refer [<sub >evt event->fn sub->fn]]
   [iron.utils :as utils]
   [firebase.app :as firebase-app]
   [firebase.storage :as firebase-storage]
   [com.degel.re-frame-firebase.core :as core]
   [com.degel.re-frame-firebase.specs :as specs]
   [com.degel.re-frame-firebase.helpers :refer [promise-wrapper]]))


;;; 1. Create a root reference
;;; 2. Create reference to end object
;;; 3. Upload blob/file

(defn clj->StorageReference
  "Converts path, a string/keyword or seq of string/keywords, into a StorageReference"
  [path]
  {:pre [(utils/validate ::specs/path path)]}
  (if (instance? js/firebase.storage.Reference path)
    path
    (.child
     (.ref (js/firebase.storage))
     (str/join "/" (clj->js path)))))

(defn- putter
  [path blob]
  (.put (clj->StorageReference path)
        blob))

(defn put-effect [{:keys [path data on-success on-failure]}]
  (promise-wrapper (putter path data) on-success on-failure))

(defn- deleter
  [path]
  (.delete (clj->StorageReference path)))

(defn delete-effect [{:keys [path on-succes on-failure]}]
  (promise-wrapper (deleter path) on-success on-failure))
