(ns com.degel.re-frame-firebase.storage
  (:require
   ["@firebase/storage" :refer (getStorage uploadBytesResumable ref deleteObject getDownloadURL)]
   [com.degel.re-frame-firebase.core :as core]
   [com.degel.re-frame-firebase.helpers :refer [promise-wrapper]]))

(defn- get-storage [bucket]
  (if bucket
    (-> @core/firebase-state
        :app
        (getStorage (str "gs://" bucket)))
    (-> @core/firebase-state
        :app
        getStorage)))                                 ;default firebase bucket

(defn- put [path file metadata on-success on-error on-progress bucket]
  (let [upload-task (uploadBytesResumable (ref (get-storage bucket) path)
                                          file metadata)]
    (.on
     upload-task
     "state_changed"
     #(if on-progress
        (on-progress (* (/ (.-bytesTransferred %) (.-totalBytes %)) 100))
        (fn []))
     #(if on-error
        (on-error %)
        (fn []))
     #(if on-success
        (on-success)
        (fn [])))))

(defn- delete [path on-success on-error bucket]
  (promise-wrapper (deleteObject (ref (get-storage bucket) path))
                   on-success
                   on-error))

(defn download-url-effect [{:keys [path on-success on-error bucket]}]
  (promise-wrapper (getDownloadURL (ref (get-storage bucket) path))
                   on-success
                   #(on-error (-> % js->clj .-message))))

(defn put-effect [items _]
  (doseq [item items]
    (let [{:keys [path file metadata on-success on-error on-progress bucket]} item]
      (put path file
           (clj->js metadata)
           on-success
           #(on-error (-> % js->clj .-message))
           on-progress
           bucket))))

(defn delete-effect [items _]
  (doseq [item items]
    (let [{:keys [path on-success on-error bucket]} item]
      (delete path
              on-success
              #(on-error (-> % js->clj .-message))
              bucket))))
