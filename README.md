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

## Configuration

[![Clojars Project](https://img.shields.io/clojars/v/com.degel/re-frame-firebase.svg)](https://clojars.org/com.degel/re-frame-firebase)

- Add this project to your dependencies. Note this this automatically includes firebase too; currenntly v4.2.0
- Reference the main namespace in your code as  `[com.degel.re-frame-firebase :as firebase]`
- Initialize the library in your app initialization, probably just before you call `(mount-root)`

````
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

````

## Usage (TBD)

- Fns and event/sub vectors
- API (see [source](src/com/degel/re_frame_firebase.cljs))
- Open TODO issues
- Uncovered parts of firebase



## Setup

_This section is Mies boilerplate. Not yet reviewed._

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
