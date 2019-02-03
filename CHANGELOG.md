# Change Log

## [Unreleased]
- (no changes yet)

## [0.8.0]
- Fixes to Firestore implementation, docs, and example
- Support for phone auth
- Remove explicit dependency on reagent, to avoid conflicts
- Make some private vars public since they were used cross-namespace, and had triggered
  new ClojureScript compiler warnings
- Update dependencies, including to Clojure 1.10 and Firebase 5.7

## [0.7.0]
Another volunteer-driven release. Thanks Mathew and Shen!
- Support for anonymous sign-in and custom authentication
- Doc cleanups

## [0.6.0]
Many thanks to the project contributors. I did nothing this release but integrate the
work from wonderful volunteers.
- Support for Firestore.
- Support for email authentication and registration
- Return new key created by Firebase Push
- Code reorganization and cleanups

## [0.5.0]
- Update Clojure and Firebase dependencies

## [0.4.0]
- Dependency change: Replace Sodium library with Iron
- Make :-on handler more error-resistant

## [0.3.0]
- Add account linking
- Update dependencies
- Add connection state monitoring
- Use Firebase 4.4.0

## [0.2.0]
- Add Facebook, Twitter, and Github authg

## [0.1.0]
- Initial project: Wrapper around Firebase
- Support basic read/write/watch
- Google auth

[Unreleased]: https://github.com/deg/re-frame-firebase/compare/c4ee44a...HEAD
[0.8.0]: https://github.com/deg/re-frame-firebase/compare/684812e...c4ee44a
[0.7.0]: https://github.com/deg/re-frame-firebase/compare/46f5630...684812e
[0.6.0]: https://github.com/deg/re-frame-firebase/compare/7192dfc...46f5630
[0.5.0]: https://github.com/deg/re-frame-firebase/compare/0c4cb21...7192dfc
[0.4.0]: https://github.com/deg/re-frame-firebase/compare/41e6695...0c4cb21
[0.3.0]: https://github.com/deg/re-frame-firebase/compare/90f163f...41e6695
[0.2.0]: https://github.com/deg/re-frame-firebase/compare/4804b1f...90f163f
[0.1.0]: https://github.com/deg/re-frame-firebase/compare/b2f1711...4804b1f
