# An example firestore App

This tiny application is meant to test and show you how to use the firestore API.

All the code is in one namespace: `src/firestore/core.cljs`.

### Run It And Change It   

Steps:

1. Check out the re-frame repo
2. Get a command line
3. `cd` to the root of this sub project (where this README exists)
4. run "`lein do clean, figwheel`"  to compile the app and start up figwheel hot-reloading,
5. open `http://localhost:3449/example.html` to see the app

While step 4 is running, any changes you make to the ClojureScript
source files (in `src`) will be re-compiled and reflected in the running
page immediately.

### Production Version

Run "`lein do clean, with-profile prod compile`" to compile an optimized
version, and then open `resources/public/index.html` in a browser.
