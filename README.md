# re-frame-firebase

[Re-frame](https://github.com/Day8/re-frame) wrapper around Google's
[Firebase](https://firebase.google.com) database.


## Overview

There are already several ClojureScript wrappers of Firebase, most notably
[Matchbox](https://github.com/crisptrutski/matchbox). However, I was not able to find
any that work with recent version of Firebase, nor that smoothly integrate with
re-frame.

Re-frame-firebase is based on ideas, and some code, from Timothy Pratley's [blog
post](http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html)
and [VoterX](https://github.com/timothypratley/voterx) project. I've added the packaging
as a standalone project, the integration with re-frame and, I'm sure, any mistakes that
I've not yet caught.

_Note: This project is under active development, and exists primarily to meet my immediate
needs. Therefore, many Firebase features are still missing. I will probably only add
them as I need. But, I am receptive to feature requests and happy to accept PRs._

## Configuration

[![Clojars Project](https://img.shields.io/clojars/v/com.degel/re-frame-firebase.svg)](https://clojars.org/com.degel/re-frame-firebase)

- Add this project to your dependencies. The current version is
  `[com.degel/re-frame-firebase "0.1.0"]`. Note this this automatically includes firebase
  too; currently v4.2.0.
- Reference the main namespace in your code: `[com.degel.re-frame-firebase :as firebase]`
- Initialize the library in your app initialization, probably just before you call
  `(mount-root)`. See below for details.

## Usage

The public portions of the API are all in
[re\_frame\_firebase.cljs](src/com/degel/re_frame_firebase.cljs). That file also
includes API documentation that may sometimes be more current or complete than
what is here.

This is a re-frame API. It is primarily accessed through re-frame events and
subscriptions.

### Initialization

Initialize the library in your app initialization, probably just before you call `(mount-root)`.


```
;;; From https://console.firebase.google.com/u/0/project/trilystro/overview - "Add Firebase to your web app"
(defonce firebase-app-info
  {:apiKey "YOUR-KEY-HERE"
   :authDomain "YOUR-APP.firebaseapp.com"
   :databaseURL "https://YOUR-APP.firebaseio.com"
   :storageBucket "YOUR-APP.appspot.com"})

(defn ^:export init []
  ,,,
  (firebase/init :firebase-app-info firebase-app-info
                 :get-user-sub           [:user]
                 :set-user-event         [:set-user]
                 :default-error-handler  [:firebase-error])
  ,,,
)

```

This initialization does two things:

1. It supplies your Firebase credentials to this library
2. It defines several callbacks to your project from the library

#### Credentials

You need to create your Firebase project on its
[site](https://firebase.google.com). This will supply you wiht a set of credentials: an
API key, domain, URL, and bucket. Mimicking the sample above, copy these into your code.

Note that it is ok to have these credentials visible in your client-side code. But, you
must configure Firebase rules to safely control access to your database.

#### Callbacks

This library relies on your code to implement two behaviors:
- Storing the user object
- Reporting errors

It communicates with your code via three callbacks that you define in `firebase/init`:
`:get-user-sub`, `:set-user-event`, and `:default-error-handler`. The first of these is
normally a re-frame subscription vector, while the latter two are re-frame event
vectors. As is typical in re-frame, info will be passed by appending it to the the vector.

Note that re-frame-firebase uses the [Sodium](https://github.com/deg/sodium) library,
which supports passings functions instead of re-frame subscriptions or events. Each of
these callbacks can, therefore, also be plain functions.

For more details, e.g. the parameters passed to each callback, see the documentation in
the [source](src/com/degel/re_frame_firebase.cljs).

You can also see some sample code in my toy project
[trilystro](https://github.com/deg/trilystro). But, tread carefully here, this is my
experimental stomping ground and things may be broken at any time.


### Authentication

Firebase supports a variety of user authentication mechanisms. Currently,
re-frame-firebase supports following Firebase authentication providers:
 - Google
 - Facebook
 - Twitter
 - GitHub
 
(PRs welcome that add to this!)

Before an authentication provider can be used, it has to be enabled and configured in
Firebase Console (Authentication -> Sign-in method section).

You need to write three events: two to handle login and logout requests from your views,
and and one to store the user information returned to you from the library. You also
need to write a subscription to return the user information to the library.  For
example:

```
;;; Simple sign-in event. Just trampoline down to the re-frame-firebase
;;; fx handler.
(re-frame/reg-event-fx
 :sign-in
 (fn [_ _] {:firebase/google-sign-in {:sign-in-method :popup}))


;;; Ditto for sign-out
(re-frame/reg-event-fx
 :sign-out
 (fn [_ _] {:firebase/sign-out nil}))


;;; Store the user object
(re-frame/reg-event-db
 :set-user
 (fn [db [_ user]]
   (assoc db :user user)))

;;; A subscription to return the user to the library
(re-frame/reg-sub
  :user
  (fn [db _] (:user db)))

```

The user object contains several opaque fields used by the library and firebase,
and also several fields that may be useful for your application, including:

- `:display-name` - The user's full name
- `:email` - The user's email address
- `:photo-url` - The user's photo
- `:uid` - The user's unique id, used by Firebase. Helpful for setting up private areas in
  the db


### Writing to the database

The firebase database is a tree. You can write values to nodes in a tree, or push them
to auto-generated unique sub-nodes of a node.  In re-frame-firebase, these are exposed
through the `:firebase/write` and `:firebase/push` effect handlers.

Each takes parameters:
- `:path` - A vector representing a node in the firebase tree, e.g. `[:my :node]`
- `:value` - The value to write or push
- `:on-success` - Event vector or function to call when write succeeds.
- `:on-failure` - Event vector or function to call with the error.

Example:

```
(re-frame/reg-event-fx
  :write-status
  (fn [{db :db} [_ status]]
    {:firebase/write {:path [:status]
                      :value status
                      :on-success #(js/console.log "Wrote status")
                      :on-failure [:handle-failure]}}))
                      
;;; :firebase/push is treated the same
```

Re-frame-firebase also supplies `:firebase/multi` to allow multiple write and/or
pushes from a single event:

```
(re-frame/reg-event-fx
  :write-keyed-message
  (fn [{db :db} [_ message]]
    {:firebase/multi [[:firebase/write {:path [:latest-message] :value :message :on-,,,}]
                      [:firebase/push  {:path [:messages] :value :message :on-,,,}]]}))
```

### Reading from the database

Firebase supports one-time reads of a node and also subscribing to a node to receive
updates anytime its content changes.  Both are supported by re-frame-firebase. (But,
note, we don't yet support all the subscription variatons offered by Firebase).

`firebase/read-once` handles one-time reads. Perhaps surprisingly, it is an event
handler, not a subscription. This is because a one-time read is a sink not a
source. Your application actively requests a value. The response then returns,
triggering another event. Conceptually, this is very much like an http request.

```
(re-frame/reg-event-fx
  :read-motd
  (fn [{db :db} [_ status]]
    {:firebase/read-once {:path [:message-of-the-day]
                          :on-success [:got-motd]
                          :on-failure [:handle-failure]}}))
```

Firebase '`:on`' subscriptions are handled as re-frame subscriptions:

```
(re-frame/subscribe [:firebase/on-value {:path [:latest-message]}])
```

The firebase subscription will remain active only while the re-base subscription is
active. Effectively, this is when any variable bound to the subscription remains in
scope.

This, combined with re-frame 0.9's beautiful subscription caching leads to some very
nice behavior: If you want to subscribe to a re-frame value for a long period of time,
but want to access it deep inside a component, you can do this easily and efficiently by
subscribing twice to the same path. _(If you are not familiar with this area,
<https://github.com/Day8/re-frame/issues/218> is a useful read)_

You subscribe once in the outermost component of your page, which will, presumably,
never be reloaded. This causes the subsription to become and remain active.

You subcribe again within any component that wants to access the value. This causes
_zero_ extra work. The firebase subscription only happens once. Firebase pushes any
changes as they happen, precisely once per change. Re-frame-firebase caches the current
value locally. The subscriptions read the value from the local cache.

_Note well: It is not sufficient to just mention the subscription in the outer
component. You must actually use it in the component, so that it is embedded in a
mounted component._

_Internal detail: The values are currently cached in your app db, under the key
`com.degel.re-frame-firebase.core\cache`. But, this is an implementation detail, subject
to change. Please do not rely on this for anything except, perhaps, debugging._


## Setup

This is a library project. Although it still includes some of the Mies
templates's scaffolding for a standalone project, I have not used these
features and they may have decayed.

For development, I just include this project in the
[checkouts directory](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md#checkout-dependencies)
of a project that uses it.

Then, for deployment, simply:

```
lein deploy clojars
```

_The rest of this section is Mies boilerplate. Probably all correct, but not necesarily relevant._

    Most of the following scripts require [rlwrap](http://utopia.knoware.nl/~hlub/uck/rlwrap/) (on OS X installable via brew).
    
    Build your project once in dev mode with the following script and then open `index.html` in your browser.
    
        ./scripts/build
    
    To auto build your project in dev mode:
    
        ./scripts/watch
    
    To start an auto-building Node REPL:
    
        ./scripts/repl
    
    To get source map support in the Node REPL:
    
        lein npm install
        
    To start a browser REPL:
        
    1. Uncomment the following lines in src/re_frame_firebase/core.cljs:
    ```clojure
    ;; (defonce conn
    ;;   (repl/connect "http://localhost:9000/repl"))
    ```
    2. Run `./scripts/brepl`
    3. Browse to `http://localhost:9000` (you should see `Hello world!` in the web console)
    4. (back to step 3) you should now see the REPL prompt: `cljs.user=>`
    5. You may now evaluate ClojureScript statements in the browser context.
        
    For more info using the browser as a REPL environment, see
    [this](https://github.com/clojure/clojurescript/wiki/The-REPL-and-Evaluation-Environments#browser-as-evaluation-environment).
        
    Clean project specific out:
    
        lein clean
         
    Build a single release artifact with the following script and then open `index_release.html` in your browser.
    
        ./scripts/release

## Questions

I can usually be found on the [Clojurians Slack](https://clojurians.net) #reagent or
#re-frame slack channels. My handle is @deg. Email is also fine.

## License

Copyright © 2017 David Goldfarb

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
