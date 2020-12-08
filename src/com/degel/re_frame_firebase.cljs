;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx

(ns com.degel.re-frame-firebase
  (:require
   [re-frame.core :as re-frame]
   [com.degel.re-frame-firebase.core :as core]
   [com.degel.re-frame-firebase.auth :as auth]
   [com.degel.re-frame-firebase.database :as database]
   [com.degel.re-frame-firebase.firestore :as firestore]
   [com.degel.re-frame-firebase.storage :as storage]))

;;; Write a value to Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#set
;;;
;;; Example FX:
;;; {:firebase/write [:path [:my :data]
;;;                   :value 42
;;;                   :on-success #(prn "Write succeeded")
;;;                   :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/write database/write-effect)

;;; Update values to Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#update
;;;
;;; Example FX:
;;; {:firebase/update [:path [:my :data]
;;;                   :value {:life 42, :universe 42, :everything 42}
;;;                   :on-success #(prn "Write succeeded")
;;;                   :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/update database/update-effect)

;;; Transactionally reads and writes a value to Firebase.  NB: :transaction-update function
;;; may run more than once so must be free of side effects.  Importantly, it must be able
;;; to handle null data.  To abort a transaction, return js/undefined.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#transaction
;;;
;;; Examples FX:
;;; {:firebase/transaction {:path [:my :data]
;;;                   :transaction-update (fn [old-val] (if old-val (inc old-val)))
;;;                   :apply-locally false  ;; default is true = multiple update events may be received if transaction-update needs to be run more than once.
;;;                   ;; The on-* handlers can also take a re-frame event
;;;                   :on-success (fn [snapshot committed] (if committed (prn "Transaction committed: " snapshot)))
;;;                   :on-failure (fn [err snapshot committed] (prn "Error: " err))}}
;;;
;;; {:firebase/swap {:path [:my :data]
;;;                   :f +
;;;                   :argv [2 3]
;;;                   :apply-locally false  ;; default is true = multiple update events may be received if transaction-update needs to be run more than once.
;;;                   ;; The on-* handlers can also take a re-frame event
;;;                   :on-success (fn [snapshot committed] (if committed (prn "Transaction committed: " snapshot)))
;;;                   :on-failure [:firebase-error]}}
(re-frame/reg-fx :firebase/transaction database/transaction-effect)
(re-frame/reg-fx :firebase/swap database/swap-effect)  ; A synonym with :argv for update function :f

;;; Write a value to a Firebase list.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#push
;;;
;;; Example FX:
;;; {:firebase/push [:path [:my :collection]
;;;                  :value "Hello world"
;;;                  :on-success #(prn "Push succeeded")
;;;                  :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/push database/push-effect)


;;; Asynch one-time read of a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#once
;;;
;;; Example FX:
;;; {:firebase/read-once [:path [:my :data]
;;;                      :on-success [:got-my-data]
;;;                      :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/read-once database/once-effect)


;;; Dispatch a vector of firebase effects
;;;
;;; Example FX:
;;; {:firebase/multi [[:firebase/write {:path ,,,}]
;;;                   [:firebase/push {:path ,,,}]
;;;                   ,,,]}
;;;
(re-frame/reg-fx
 :firebase/multi
 (fn [effects]
   (run! (fn [[event-type args]]
           (case event-type
             :firebase/write        (database/write-effect args)
             :firebase/update       (database/update-effect args)
             :firebase/push         (database/push-effect args)
             :firebase/transaction  (database/transaction-effect args)
             :firebase/swap         (database/swap-effect args)
             :firebase/read-once    (database/once-effect args)
             :firestore/delete      (firestore/delete-effect args)
             :firestore/set         (firestore/set-effect args)
             :firestore/update      (firestore/update-effect args)
             :firestore/add         (firestore/add-effect args)
             :firestore/write-batch (firestore/write-batch-effect args)
             :firestore/get         (firestore/get-effect args)
             :firestore/on-snapshot (firestore/on-snapshot args)
             (js/alert "Internal error: unknown firebase effect: " event-type " (" args ")")))
         effects)))


;;; Watch a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#on
;;;
;;; Example Subscription:
;;; (re-frame/subscribe
;;;   [:firebase/on-value {:path [:my :data]
;;;                        :on-failure [:firebase-error]}])
;;;
(re-frame/reg-sub-raw :firebase/on-value  database/on-value-sub)


;;; Login to firebase, using one of OAuth providers
;;;
;;; Accepts a map of the following options:
;;;
;;; - :sign-in-method  either :redirect (default) or :popup mode
;;;
;;; - :scopes       a sequence of additional OAuth scopes; supported for following auth providers:
;;;       Google:   https://developers.google.com/identity/protocols/googlescopes
;;;       Facebook: https://developers.facebook.com/docs/facebook-login/permissions
;;;       GitHub:   https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/
;;;
;;; - :custom-parameters  check auth providers documentation for supported values:
;;;       Google:   https://firebase.google.com/docs/reference/js/firebase.auth.GoogleAuthProvider#setCustomParameters
;;;       Facebook: https://firebase.google.com/docs/reference/js/firebase.auth.FacebookAuthProvider#setCustomParameters
;;;       Twitter:  https://firebase.google.com/docs/reference/js/firebase.auth.TwitterAuthProvider#setCustomParameters
;;;       GitHub:   https://firebase.google.com/docs/reference/js/firebase.auth.GithubAuthProvider#setCustomParameters
;;;
;;; Example FX:
;;; {firebase/google-sign-in {:sign-in-method :popup
;;;                           :scopes ["https://www.googleapis.com/auth/contacts.readonly"
;;;                                    "https://www.googleapis.com/auth/calendar.readonly"]
;;;                           :custom-parameters {"login_hint" "user@example.com"}}}
;;;
(re-frame/reg-fx :firebase/google-sign-in    auth/google-sign-in)
(re-frame/reg-fx :firebase/facebook-sign-in  auth/facebook-sign-in)
(re-frame/reg-fx :firebase/twitter-sign-in   auth/twitter-sign-in)
(re-frame/reg-fx :firebase/github-sign-in    auth/github-sign-in)
(re-frame/reg-fx :firebase/microsoft-sign-in auth/microsoft-sign-in)


;;; Login to firebase using email/password authentication
;;; or registers a new user for email/password authentication.
;;;
;;; Accepts a map with :email and :password
;;;
;;; Example FX:
;;; {:firebase/email-sign-in {:email "test@github.com" :password "myverysecretpassword"}}
;;;
;;; or to create a new user:
;;; {:firebase/email-create-user {:email "newuser@github.com" :password "anotherverysecretpassword"}}
;;;
(re-frame/reg-fx :firebase/email-sign-in     auth/email-sign-in)
(re-frame/reg-fx :firebase/email-create-user auth/email-create-user)


;;; Login to firebase anonymously
;;;
;;; Parameter is not used
;;;
(re-frame/reg-fx :firebase/anonymous-sign-in auth/anonymous-sign-in)

;;; Login to firebase using a custom token
;;;
;;; Accept a map with :token, a JWT as a string
;;;
;;; Example FX:
;;; {:firebase/custom-token-sign-in {:token "eyJhbGciOiJS.."}}
(re-frame/reg-fx :firebase/custom-token-sign-in auth/custom-token-sign-in)


;;; Login to firebase with a phone
;;;
;;; Initialise recaptcha
;;;
;;; Accepts a map with
;;; - :container-id - DOM id (typically the "submit phone number" button)
;;; - :on-solve - re-frame event vector for verification (when user is verified as human)
;;;
;;; Example FX:
;;; {:firebase/init-recaptcha {:container-id "sign-in-btn"
;;;                            :on-solve     [:welcome-human]}}
;;;
;;; Phone number sign-in
;;;
;;; Accepts a map with
;;; - :phone-number - a string in e164 format
;;; - :on-send - an event vector for when code is sent
;;;
;;; Example FX:
;;; {:firebase/phone-number-sign-in {:phonenumber "+27820000000"
;;;                                  :on-send     [:notify-sms-sent]}}
;;;
;;; Confirm coe
;;;
;;; Accepts a map with :code, as sent in the SMS
;;;
;;; Example FX:
;;; {:firebase/phone-number-confirm-code {:code "123456}}
(re-frame/reg-fx :firebase/init-recaptcha            auth/init-recaptcha)
(re-frame/reg-fx :firebase/phone-number-sign-in      auth/phone-number-sign-in)
(re-frame/reg-fx :firebase/phone-number-confirm-code auth/phone-number-confirm-code)


;;; Logout
;;;
;;; Example FX:
;;; {firebase/sign-out []}
;;;
(re-frame/reg-fx :firebase/sign-out auth/sign-out)


;;; Monitor connection status
;;;
(re-frame/reg-sub
 :firebase/connection-state
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path [:.info :connected]}]))
 (fn [connected? _]
   {:firebase/connected? (= connected? true)}))


;;; Set a document to Firestore.
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference#se
;;;
;;; Key arguments:
;;; - :path        Vector of keywords and/or strings representing the path to the document.
;;; - :data        Map corresponding to the document.
;;; - :set-options Map containing additional options, see firestore/clj->SetOptions.
;;; - :on-success  Function or re-frame event vector to be dispatched.
;;; - :on-failure  Function or re-frame event vector to be dispatched.
;;;
;;; Example FX:
;;; {:firestore/set {:path [:my-collection "my-document"]
;;;                  :data {:field1 "value1"
;;;                         :field2 {:inner1 "a" :inner2 "b"}}
;;;                  :set-options {:merge false
;;;                                :merge-fields [:field1 [:field2 :inner1]]}
;;;                  :on-success [:success-event]
;;;                  :on-failure #(prn "Error:" %)}}
;;;
(re-frame/reg-fx :firestore/set firestore/set-effect)


;;; Update a document to Firestore.
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference#update
;;;
;;; Key arguments: :path, :data, :on-success, :on-failure
;;;
(re-frame/reg-fx :firestore/update firestore/update-effect)


;;; Delete a document from Firestore.
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference#delete
;;;
;;; Key arguments: :path, :on-success, :on-failure
;;;
(re-frame/reg-fx :firestore/delete firestore/delete-effect)


;;; Execute multiple write operations using Firestore's WriteBatch
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.WriteBatch
;;;
;;; WriteBatches only support :firestore/set, :firestore/update and :firestore/delete.
;;; Key arguments:
;;; - :operations  Vector of effect maps for each of the wanted operations.
;;; - :on-success  You should supply a single callback function/events here for all of the operations.
;;; - :on-failure
;;;
;;; Example FX:
;;; {:firestore/batch-write
;;;  {:operations
;;;   [[:firestore/set {:path [:cities "SF"] :data {:name "San Francisco" :state "CA"}}]
;;;    [:firestore/set {:path [:cities "LA"] :data {:name "Los Angeles" :state "CA"}}]
;;;    [:firestore/set {:path [:cities "DC"] :data {:name "Washington, D.C." :state nil}}]]
;;;   :on-success #(prn "Cities added to database.")
;;;   :on-failure #(prn "Couldn't add cities to database. Error:" %)}}
;;;
(re-frame/reg-fx :firestore/write-batch firestore/write-batch-effect)


;;; Add a document to a Firestore collection.
;;;
;;; Key arguments: :path, :data, :on-success, :on-failure
;;;
;;; - :path       Should be a path to a collection.
;;; - :on-success Will be provided with a vector of strings representing the path to the created document.
;;;
;;; Example FX:
;;; {:firestore/add {:path [:my-collection]
;;;                  :data my-data
;;;                  :on-success #(prn "Added document ID:" (last %))}}
;;;
(re-frame/reg-fx :firestore/add firestore/add-effect)


;;; Get a document or collection query from Firestore.
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference#get
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.CollectionReference#get
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.Query
;;;
;;; When querying for a document, you can supply the following key arguments:
;;; - :path-document    The same vector of keywords/strings as other effects.
;;; - :get-options      Map containing additional options. See firestore/clj->GetOptions.
;;; - :snapshot-options Map to be passed when retrieving data from Snapshots.
;;;                     See firestore/clj->SnapshotOpions.
;;; - :expose-objects   When set to true, the original Snapshot will be attached
;;;                     under the :object key, see firestore/DocumentSnapshot->clj.
;;; - :on-success       The clojure object will be passed as an argument to the event or fn.
;;; - :on-failure
;;;
;;; When querying for a collection, you can supply the following key arguments:
;;; - :path-collection
;;; - :get-options
;;; - :where          A seq of triples [field-path op value] where op should be
;;;                   :>, :>=, :< :<=, or :==. You can also provide strings like "<=".
;;; - :order-by       A seq of pairs [field-path direction] where direction should
;;;                   either be :asc or :desc. Ascending is the default.
;;;                   You can also provide strings like "desc".
;;; - :limit          Limit the number of documents to the specified number.
;;; - :start-at, :start-after, :end-at, :end-before
;;;                   Limit the query at the provided document. Either by providing
;;;                   a seq with a single DocumentSnapshot or multiple field values
;;;                   in the same order as :order-by.
;;; - :doc-changes    If set to true, a vector of parsed DocumentChanges will be
;;;                   provided under :doc-changes. See firestore/DocumentChange->clj.
;;; - :snapshot-options
;;; - :snapshot-listen-options Map to be passed when retrieving doc changes.
;;;                            See firestore/SnapshotListenOptions->clj.
;;; - :expose-objects See firestore/QuerySnapshot->clj.
;;; - :on-success, :on-failure
;;;
;;; Example FX:
;;; {:firestore/get {:path-document [:my-collection :my-document]
;;;                  :expose-objects false
;;;                  :on-success #(prn "Objects's contents:" (:data %))}}
;;; {:firestore/get {:path-collection [:cities]
;;;                  :where [[:state :>= "CA"]
;;;                          [:population :< 1000000]]
;;;                  :limit 1000
;;;                  :order-by [[:state :desc]
;;;                             [:population :desc]]
;;;                  :start-at ["CA" 1000]
;;;                  :doc-changes false
;;;                  :on-success #(prn "Number of documents:" (:size %))}}
;;;
(re-frame/reg-fx :firestore/get firestore/get-effect)


;;; Set up a listener for changes in a Firestore collection/document query.
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference#onSnapshot
;;; See https://firebase.google.com/docs/reference/js/firebase.firestore.Query#onSnapshot
;;;
;;; You can provide the same key-arguments as to :firestore/get, except for :get-options,
;;; :on-success and :on-failure. Instead, you can/should provide the following:
;;; - :snapshot-listen-options Map containing additional options.
;;;                            See firestore/clj->SnapshotListenOptions.
;;; - :on-next  Event/function to be called every time a change happens.
;;;             The clojure object will be passed as an argument to the event or fn.
;;; - :on-error
;;;
(re-frame/reg-fx :firestore/on-snapshot firestore/on-snapshot-effect)


;;; Subscribe to a Firestore collection/document query.
;;;
;;; Takes the same arguments as :firestore/on-snapshot effect, except for :on-next,
;;; as it is meant to be used as a subscription.
;;;
;;; Example Subscription:
;;; (re-frame/subscribe
;;;   [:firestore/on-snapshot {:path-document [:my :document]}])
;;;
(re-frame/reg-sub-raw :firestore/on-snapshot firestore/on-snapshot-sub)

;;; Firebase Storage, an online object store, different from the similarly named Firestore.

(re-frame/reg-fx :storage/put storage/put-effect)
;;(re-frame/reg-fx :storage/delete storage/delete-effect)


;;; Start library and register callbacks.
;;;
;;;
;;; In Iron style, most of the parameters can be either a function or a
;;; re-frame event/sub vector. If there is a parameter, it will passed to the
;;; function or conj'd onto the vector.
;;;
;;; - :firebase-app-info - Firebase application credentials. This is the one
;;;   parameter that takes a map:
;;;    {:apiKey "MY-KEY-MY-KEY-MY-KEY-MY-KEY"
;;;     :authDomain "my-app.firebaseapp.com"
;;;     :databaseURL "https://my-app.firebaseio.com"
;;;     :projectId: "my-app"
;;;     :storageBucket "my-app.appspot.com"
;;;     :messagingSenderId: "000000000000"}
;;;
;;; - :set-user-event - Function or re-frame event that will be called back
;;;     to receive and store the user object from us, when login succeeds.
;;;     This object is a map that includes several fields that we need, plus
;;;     the following that may be useful to the calling app:
;;;       :display-name - The user's full name
;;;       :email - The user's email address
;;;       :photo-url - The user's photo
;;;       :uid - The user's unique id, used by Firebase.
;;;
;;; - :get-user-sub - Function or re-frame subscription vector that this
;;;   library will use to access the user object stored by :set-user-event
;;;
;;; - :default-error-handler - Function or re-frame event that will be called
;;;   to handle any otherwise unhandled errors.
;;;
(defn init [& {:keys [firebase-app-info
                      firestore-settings
                      get-user-sub
                      set-user-event
                      default-error-handler]}]
  (core/set-firebase-state :get-user-sub          get-user-sub
                           :set-user-event        set-user-event
                           :default-error-handler default-error-handler)
  (core/initialize-app firebase-app-info)
  (firestore/set-firestore-settings firestore-settings)
  (auth/init-auth))
