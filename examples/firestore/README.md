# An example firestore App

This tiny application is meant to test and show you how to use the firestore API.

All the code is in one namespace: `src/firestore/core.cljs`, api-keys are in
`src/firestore/api-keys.cljs`.

## How to run it

### Firebase steps (from [here](https://github.com/firebase/quickstart-js/tree/master/firestore))
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com).
2. In the Firebase console, enable Cloud Firestore on your project by doing: **Database > Create Database**
3. Select testing mode for the security rules
4. Copy/Download this repo and open this folder in a Terminal.
5. Install the Firebase CLI if you do not have it installed on your machine:
   ```bash
   npm -g i firebase-tools
   ```
6. Set the CLI to use the project you created in step 1:
   ```bash
   firebase use --add
   ```
7. Deploy the Firestore security rules and indexes:
   ```bash
   firebase deploy --only firestore
   ```

### Clojure steps

1. Copy the API keys from your firebase project console to `api-keys.cljs` (replace
   the existing dummy values).
2. Open a command line in this folder.
3. Compile the app and start figwheel's hot-reloading.
   ```bash
   lein do clean, figwheel
   ```
4. Open `http://localhost:3449/` to see the app.

While step 3 is running, any changes you make to the ClojureScript source files
(in `src`) will be re-compiled and reflected in the running page immediately.

## Production version

To compile an optimized version, run:

```bash
lein do clean, with-profile prod compile
```

And then open `resources/public/index.html` in a browser.
