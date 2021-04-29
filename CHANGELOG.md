# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

### [1.8.1](https://github.com/redwarp/gif-wallpaper/compare/v1.8.0...v1.8.1) (2021-04-29)


### Bug Fixes

* Update russian translations ([5f898a0](https://github.com/redwarp/gif-wallpaper/commit/5f898a0b9940cd94f302b4659d047b6f28753d3d))

## [1.8.0](https://github.com/redwarp/gif-wallpaper/compare/v1.7.4...v1.8.0) (2021-04-18)


### Features

* Add spanish support ([452cd08](https://github.com/redwarp/gif-wallpaper/commit/452cd08d6f6fed614f8b560f57000c548a5ed22a))
* Replace livedata with flows ([#114](https://github.com/redwarp/gif-wallpaper/issues/114)) ([fc8e63c](https://github.com/redwarp/gif-wallpaper/commit/fc8e63cdf7a18cae1d8ac539e2dcc68e86e392ce))


### Bug Fixes

* Cleanup wallpaper colors recalculation ([3765384](https://github.com/redwarp/gif-wallpaper/commit/376538441b81d234eadf63cc02e46f13a1811d40))
* Invalidate drawable on color change ([23b8884](https://github.com/redwarp/gif-wallpaper/commit/23b8884954a5b38deb9b1185e657f22a810e2a3f))

### [1.7.4](https://github.com/redwarp/gif-wallpaper/compare/v1.7.3...v1.7.4) (2021-04-07)


### Bug Fixes

* Update theme to pastel ([9109d66](https://github.com/redwarp/gif-wallpaper/commit/9109d66e14e58ab6076d4a6ef0b66a3fb97e8844))

### [1.7.3](https://github.com/redwarp/gif-wallpaper/compare/v1.7.2...v1.7.3) (2021-04-03)


### Bug Fixes

* Overflow icon will change color properly ([8324039](https://github.com/redwarp/gif-wallpaper/commit/832403915e3b27309f55eb022e147b1e9b402530))
* Thread safe-ish-ify the SurfaceDrawableRenderer ([55a9fe4](https://github.com/redwarp/gif-wallpaper/commit/55a9fe41c4ea4b09a744e7362a279b34d78b2457))

### [1.7.2](https://github.com/redwarp/gif-wallpaper/compare/v1.7.1...v1.7.2) (2021-03-31)


### Bug Fixes

* Update gifdecoder to ditch netty ([4868e91](https://github.com/redwarp/gif-wallpaper/commit/4868e915ff40767b04275e9e3d8b42ab536f61ab))

### [1.7.1](https://github.com/redwarp/gif-wallpaper/compare/v1.7.0...v1.7.1) (2021-03-29)


### Bug Fixes

* Set Proguard config for netty ([046d5ac](https://github.com/redwarp/gif-wallpaper/commit/046d5acae076b14f1de51b13248cc5a1d088b8fa))

## [1.7.0](https://github.com/redwarp/gif-wallpaper/compare/v1.6.2...v1.7.0) (2021-03-27)


### Features

* Use random file instead of memory for big files ([#109](https://github.com/redwarp/gif-wallpaper/issues/109)) ([c8ff6e8](https://github.com/redwarp/gif-wallpaper/commit/c8ff6e8a7d4eb20896e9f9c7ba9128b0103fd480))

### [1.6.2](https://github.com/redwarp/gif-wallpaper/compare/v1.6.1...v1.6.2) (2021-02-26)


### Bug Fixes

* Prepare to ditch jcenter and update minor deps ([b841b50](https://github.com/redwarp/gif-wallpaper/commit/b841b50b32d369b44a9077dd4908ce2e5d2f2c69))

### [1.6.1](https://github.com/redwarp/gif-wallpaper/compare/v1.6.0...v1.6.1) (2021-01-09)


### Bug Fixes

* Catch throwable instead of exception, hoping to mitigate OutOfMemory errors. Probably a shot in the dark to be honest ([3aad711](https://github.com/redwarp/gif-wallpaper/commit/3aad711ae15f06320af2a14a1fc46c705007055c))
* Prevent crash if locking canvas returns null ([87fe8fa](https://github.com/redwarp/gif-wallpaper/commit/87fe8fa1a21d912e1153fad9ba591d8b35265673))
* Remove deprecated kotlin android extension ([0d0539f](https://github.com/redwarp/gif-wallpaper/commit/0d0539fa6dee3302abb0d0002e194ae4349a84bb))

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
