# An example firestore App

This tiny application is meant to test and show you how to use the firestore API.

All the code is in one namespace: `src/firestore/core.cljs`.

## Run it and change it

Steps:

1. Check out the re-frame repo.
2. Open a command line.
3. `cd` to the root of this subproject (where this README is located).
4. Run `lein do clean, figwheel` to compile the app and start figwheel's hot-reloading.
5. Open `http://localhost:3449/` to see the app.

While step 4 is running, any changes you make to the ClojureScript source files
(in `src`) will be re-compiled and reflected in the running page immediately.

## Production version

Run `lein do clean, with-profile prod compile` to compile an optimized version,
and then open `resources/public/index.html` in a browser.
