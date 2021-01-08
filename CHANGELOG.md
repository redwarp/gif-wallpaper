# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [1.6.0](https://github.com/redwarp/gif-wallpaper/compare/v1.5.4...v1.6.0) (2021-01-08)


### Features

* Rewrite the rendering pipeline to be simpler ([#86](https://github.com/redwarp/gif-wallpaper/issues/86)) ([5736b4e](https://github.com/redwarp/gif-wallpaper/commit/5736b4ea59267baa3751ddfea49f46623f84e19b))
* Switch to the dedicated gif library and remove java code ([296aa6e](https://github.com/redwarp/gif-wallpaper/commit/296aa6ed8a9005821f317d297829bfbeb9cdf669))

### [1.5.4](https://github.com/redwarp/gif-wallpaper/compare/v1.5.3...v1.5.4) (2020-12-29)


### Bug Fixes

* Fix coroutine dispatcher for invalidateSelf ([3efb7a1](https://github.com/redwarp/gif-wallpaper/commit/3efb7a18835c31aa3d1cf8838c3e51a78dffb03a))

### [1.5.3](https://github.com/redwarp/gif-wallpaper/compare/v1.5.2...v1.5.3) (2020-12-20)


### Bug Fixes

* Streamline memory usage by reusing bitmap ([68a032d](https://github.com/redwarp/gif-wallpaper/commit/68a032d511a84f3682cdf7d36edeeb6f5d092aa4))

### [1.5.2](https://github.com/redwarp/gif-wallpaper/compare/v1.5.1...v1.5.2) (2020-12-20)


### Bug Fixes

* Fix delays again, so that proper delay is applied to proper frame ([db6ca44](https://github.com/redwarp/gif-wallpaper/commit/db6ca446234edabb07fd4331b3e16f9ae0d5fb36))

### [1.5.1](https://github.com/redwarp/gif-wallpaper/compare/v1.5.0...v1.5.1) (2020-12-19)


### Bug Fixes

* Fix animation loop by switching back to coroutine. That works ([a0e815b](https://github.com/redwarp/gif-wallpaper/commit/a0e815bc51bad7c1991fa3ee5c1128f9dcced137))
* Fix timing of GIFs by using proper time base for animation. ([18da797](https://github.com/redwarp/gif-wallpaper/commit/18da7973709722b367e2c3770ef78c5f774c0d1c))

## [1.5.0](https://github.com/redwarp/gif-wallpaper/compare/v1.4.1...v1.5.0) (2020-12-13)


### Features

* Add german language to the app. ([26520cd](https://github.com/redwarp/gif-wallpaper/commit/26520cd6be98af3e6bddb76fe9b44cf5f823f77e))

### [1.4.1](https://github.com/redwarp/gif-wallpaper/compare/v1.4.0...v1.4.1) (2020-11-27)


### Bug Fixes

* Fix frame decoding order, it should remove some weird stutter. (The ([890c7e2](https://github.com/redwarp/gif-wallpaper/commit/890c7e2fd13a9a3eea6f89fb86e40792e5f31164))

## [1.4.0](https://github.com/redwarp/gif-wallpaper/compare/v1.3.4...v1.4.0) (2020-11-20)


### Features

* Support russian language ([6a857e9](https://github.com/redwarp/gif-wallpaper/commit/6a857e9548be352dc7532e183e6e7b52e6362ebd))

### [1.3.4](https://github.com/redwarp/gif-wallpaper/compare/v1.3.3...v1.3.4) (2020-09-18)

### [1.3.3](https://github.com/redwarp/gif-wallpaper/compare/v1.3.2...v1.3.3) (2020-08-27)


### Bug Fixes

* Replace coroutines with drawable#scheduleSelf ([d1991ca](https://github.com/redwarp/gif-wallpaper/commit/d1991cacec246b01229de25a0ad5d7fc5f88c266))
