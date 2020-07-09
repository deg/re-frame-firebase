(ns com.degel.re-frame-firebase.storage
  (:require
   [firebase.storage :as firebase-storage]
   [com.degel.re-frame-firebase.helpers :refer [promise-wrapper]]))

(defn- get-storage [bucket]
  (if bucket
    (.storage (.app js/firebase) (str "gs://" bucket))
    (js/firebase.storage)))                                 ;default firebase bucket

(defn- put [path file metadata on-success on-error on-progress bucket]
  (let [upload-task (.put (-> (.ref (get-storage bucket))
                              (.child path))
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
  (promise-wrapper (.delete (-> (.ref (get-storage bucket))
                                (.child path)))
                   on-success
                   on-error))

(defn download-url-effect [{:keys [path on-success on-error bucket]}]
  (promise-wrapper (.getDownloadURL (-> (.ref (get-storage bucket))
                                        (.child path)))
                   on-success
                   on-error))

(defn put-effect [items _]
  (doseq [item items]
    (let [{:keys [path file metadata on-success on-error on-progress bucket]} item]
      (put path file
           (clj->js metadata)
           on-success on-error on-progress bucket))))

(defn delete-effect [items _]
  (doseq [item items]
    (let [{:keys [path on-success on-error bucket]} item]
      (delete path on-success on-error bucket))))




