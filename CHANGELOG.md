# Changelog

All notable changes to this project will be documented in this file.

## [2.0.4] - 2022-09-27

### Bug Fixes

- Cleaner splash screen
- Privacy policy now opens the website
- Implement missing markdown elements
- Color picker displays selected color
- WallpaperObserver was incorrectly set and would run in preview mode
- Check icon was in the wrong folder

### Miscellaneous Tasks

- Update the ic_color_lens_off icon
- Update phone launch graphic
- Update pre-commits
- Setup website to host privacy policy ([#264](https://github.com/redwarp/gif-wallpaper/issues/264))
- Set title to homepage
- Grammar and spell checking in about page
- Move privacy policy to website dir
- Bump coil-compose from 2.2.0 to 2.2.1 ([#265](https://github.com/redwarp/gif-wallpaper/issues/265))
- Bump dependencies
- Bump spotless-plugin-gradle from 6.10.0 to 6.11.0 ([#268](https://github.com/redwarp/gif-wallpaper/issues/268))
- Bump gradle from 7.2.2 to 7.3.0 ([#269](https://github.com/redwarp/gif-wallpaper/issues/269))
- Bump decoder from 1.4.0 to 1.4.4 ([#270](https://github.com/redwarp/gif-wallpaper/issues/270))
- Bump android-drawable from 1.4.0 to 1.4.4 ([#271](https://github.com/redwarp/gif-wallpaper/issues/271))
- Update privacy policy ([#272](https://github.com/redwarp/gif-wallpaper/issues/272))

### Refactor

- Simplify markdown composable creation
- Extract string literal to const

## [2.0.3] - 2022-09-02

### Bug Fixes

- Update compose to 1.2 and fix insets
- Gestures ignore system padding
- Add a border to colors in color picker
- Use repeatOnLifecycle in service
- Disk read violation

### Miscellaneous Tasks

- Remove useless manual release
- Move preview at the end of class
- Update gradle to 7.5.1
- Update kotlin and dependencies
- Replace deprecated -Xopt-in with -opt-in
- Compile and target with sdk 33
- Update fastlane
- Update screenshots
- Set project to use java home for gradle
- Add dependencyUpdates plugin to the markdown module
- Update rust dependencies
- Setup StrictMode in debug mode

### Refactor

- Remove useless code
- Delete more useless code
- Remove useless deps and tests

## [2.0.2] - 2022-08-23

### Bug Fixes

- Fix active wallpaper detection for Android 13

### Miscellaneous Tasks

- Add monochrome icon
- Update rust deps
- Create manual release flow

## [2.0.1] - 2022-07-26

### Bug Fixes

- Fix concurrency issue with the GifDrawable

### Miscellaneous Tasks

- Fix README pointing to non existing image
- Bump kotlinx-coroutines-core from 1.6.3 to 1.6.4 ([#246](https://github.com/redwarp/gif-wallpaper/issues/246))
- Update dependabot.yml
- Bump kotlinx-coroutines-android from 1.6.3 to 1.6.4 ([#245](https://github.com/redwarp/gif-wallpaper/issues/245))
- Update pre-commit hooks
- Bump rust deps to address security issue
- Update gradle to 7.5
- Update spotless for hooks

## [2.0.0] - 2022-07-09

### Bug Fixes

- Compat version of WallpaperColors for Api pre 31
- Simplify SurfaceDrawableRender to use main looper

### Features

- ⚠ [**breaking**] Rewrite the app with Jetpack compose ([#242](https://github.com/redwarp/gif-wallpaper/issues/242))

### Miscellaneous Tasks

- Add linux to gemfile lock for fastlane on github
- Update coroutine deps

## [1.13.4] - 2022-06-26

### Bug Fixes

- Remove use of global scope, declare scope in app.
- Target sdk 32
- Update russian translations
- Fix for Android 21

## [1.13.3] - 2022-06-17

### Bug Fixes

- Fix for inset code, use proper compat code

### Refactor

- Move activities and fragment to ui package

## [1.13.2] - 2022-06-13

### Bug Fixes

- Update deprecated inset code
- Update GifDrawable and decoder to 1.2.0

### Miscellaneous Tasks

- Update broken links and badges in README ([#215](https://github.com/redwarp/gif-wallpaper/issues/215))
- Fix link in readme ([#233](https://github.com/redwarp/gif-wallpaper/issues/233))
- Bump gradle from 7.1.2 to 7.2.1 ([#230](https://github.com/redwarp/gif-wallpaper/issues/230))
- Bump se.ascp.gradle.gradle-versions-filter from 0.1.10 to 0.1.16 ([#228](https://github.com/redwarp/gif-wallpaper/issues/228))
- Bump com.diffplug.spotless from 6.4.1 to 6.7.0 ([#234](https://github.com/redwarp/gif-wallpaper/issues/234))
- Bump kotlin-gradle-plugin from 1.6.10 to 1.6.21 ([#216](https://github.com/redwarp/gif-wallpaper/issues/216))
- Bump dependencies and kotlin version
- Update pre-commit hooks
- Bump gradle to 7.4.2

## [1.13.1] - 2022-03-31

### Bug Fixes

- Fix crash in lifecycle affecting Vivo v21 ([#206](https://github.com/redwarp/gif-wallpaper/issues/206))

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 6.4.0 to 6.4.1 ([#205](https://github.com/redwarp/gif-wallpaper/issues/205))

## [1.13.0] - 2022-03-31

### Features

- Add simplified chinese language support

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 6.3.0 to 6.4.0 ([#204](https://github.com/redwarp/gif-wallpaper/issues/204))
- Bump gradle from 7.1.1 to 7.1.2 ([#202](https://github.com/redwarp/gif-wallpaper/issues/202))
- Update fastlane
- Zh-Hans -> zh-CN
- Localized feature graphic for zh

## [1.12.3] - 2022-02-18

### Bug Fixes

- Temporary reset gifdecoder lib to 1.0.0

## [1.12.2] - 2022-02-17

### Bug Fixes

- Gif pausing in the service

## [1.12.1] - 2022-02-16

### Bug Fixes

- Update the renderer to release surface

### Miscellaneous Tasks

- Update pre-commit hooks
- Bump gifdecoder lib

## [1.12.0] - 2022-02-16

### Features

- Add Italian language

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 6.2.2 to 6.3.0 ([#199](https://github.com/redwarp/gif-wallpaper/issues/199))

## [1.11.5] - 2022-02-10

### Bug Fixes

- Update dependencies
- Use gif decoder 1.0.0 to solve a concurrency issue

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 6.0.0 to 6.2.2 ([#198](https://github.com/redwarp/gif-wallpaper/issues/198))
- Bump gradle from 7.0.3 to 7.1.1 ([#197](https://github.com/redwarp/gif-wallpaper/issues/197))
- Bump robolectric from 4.7 to 4.7.3 ([#180](https://github.com/redwarp/gif-wallpaper/issues/180))
- Replace versions with versions-filter
- Update gradle to 7.4

## [1.11.4] - 2021-12-05

### Bug Fixes

- Replace vector drawable by bitmap for wallpaper thumbnail to fix samsung issues

## [1.11.3] - 2021-12-01

### Bug Fixes

- WallpaperService color computation optimization
- Hoping to solve samsung s21 crashes

### Miscellaneous Tasks

- Bump robolectric from 4.6.1 to 4.7 ([#175](https://github.com/redwarp/gif-wallpaper/issues/175))

## [1.11.2] - 2021-11-10

### Bug Fixes

- Bump decoder library
- Fix crash when cancelling picking a gif

### Miscellaneous Tasks

- Minor cleanup in GifWallpaperService
- Bump com.diffplug.spotless from 5.17.1 to 6.0.0 ([#174](https://github.com/redwarp/gif-wallpaper/issues/174))

## [1.11.1] - 2021-11-02

### Bug Fixes

- Bump gifdecoder lib to 0.8.0
- Change gif picker so that third party apps can be used

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 5.15.1 to 5.15.2 ([#161](https://github.com/redwarp/gif-wallpaper/issues/161))
- Bump com.diffplug.spotless from 5.15.2 to 5.16.0 ([#163](https://github.com/redwarp/gif-wallpaper/issues/163))
- Bump gradle to 7.2
- Un-git idea file
- Update pre-commit hooks
- Bump decoder from 0.8.0 to 0.8.1 ([#168](https://github.com/redwarp/gif-wallpaper/issues/168))
- Bump com.diffplug.spotless from 5.16.0 to 5.17.0 ([#169](https://github.com/redwarp/gif-wallpaper/issues/169))
- Bump android-drawable from 0.8.0 to 0.8.1 ([#166](https://github.com/redwarp/gif-wallpaper/issues/166))
- Bump gradle from 7.0.2 to 7.0.3 ([#167](https://github.com/redwarp/gif-wallpaper/issues/167))
- Handling idea useless files
- Bump com.diffplug.spotless from 5.17.0 to 5.17.1 ([#170](https://github.com/redwarp/gif-wallpaper/issues/170))

## [1.11.0] - 2021-09-24

### Bug Fixes

- Disable battery saving by default as it fails for some people

### Features

- Target Android 12

### Miscellaneous Tasks

- Bump gradle from 7.0.0 to 7.0.1 ([#148](https://github.com/redwarp/gif-wallpaper/issues/148))
- Bump kotlin-gradle-plugin from 1.5.21 to 1.5.30 ([#150](https://github.com/redwarp/gif-wallpaper/issues/150))
- Bump com.diffplug.spotless from 5.14.2 to 5.14.3 ([#149](https://github.com/redwarp/gif-wallpaper/issues/149))
- Bump gradle from 7.0.1 to 7.0.2 ([#153](https://github.com/redwarp/gif-wallpaper/issues/153))
- Bump kotlin-gradle-plugin from 1.5.30 to 1.5.31 ([#157](https://github.com/redwarp/gif-wallpaper/issues/157))
- Bump kotlinx-coroutines-android from 1.5.1 to 1.5.2 ([#154](https://github.com/redwarp/gif-wallpaper/issues/154))
- Bump com.diffplug.spotless from 5.14.3 to 5.15.1 ([#158](https://github.com/redwarp/gif-wallpaper/issues/158))
- Bump decoder from 0.7.2 to 0.7.3 ([#159](https://github.com/redwarp/gif-wallpaper/issues/159))
- Bump kotlinx-coroutines-core from 1.5.1 to 1.5.2 ([#155](https://github.com/redwarp/gif-wallpaper/issues/155))
- Bump android-drawable from 0.7.2 to 0.7.3 ([#160](https://github.com/redwarp/gif-wallpaper/issues/160))
- Pin version for r0adkll scripts

## [1.10.2] - 2021-08-12

### Bug Fixes

- Update decoder lib to avoid index out of bounds in Gif decoding
- Replace SurfaceView with ImageView, might solve random crashes

## [1.10.1] - 2021-08-09

### Bug Fixes

- Cleanup SurfaceDrawableRenderer
- Update forgotten app icons

### Miscellaneous Tasks

- Add Waneella to readme

## [1.10.0] - 2021-08-07

### Bug Fixes

- Bump gif decoder lib and make sure gifs will loop

### Features

- Split battery saving and thermal throttling

### Miscellaneous Tasks

- Bump kotlin-gradle-plugin from 1.5.0 to 1.5.10 ([#129](https://github.com/redwarp/gif-wallpaper/issues/129))
- Bump com.diffplug.spotless from 5.12.4 to 5.12.5 ([#124](https://github.com/redwarp/gif-wallpaper/issues/124))
- Bump com.github.ben-manes.versions from 0.38.0 to 0.39.0 ([#130](https://github.com/redwarp/gif-wallpaper/issues/130))
- Bump com.diffplug.spotless from 5.12.5 to 5.13.0 ([#132](https://github.com/redwarp/gif-wallpaper/issues/132))
- Bump gradle from 4.2.1 to 4.2.2 ([#135](https://github.com/redwarp/gif-wallpaper/issues/135))
- Bump kotlin-gradle-plugin from 1.5.10 to 1.5.20 ([#134](https://github.com/redwarp/gif-wallpaper/issues/134))
- Bump robolectric from 4.5.1 to 4.6.1 ([#136](https://github.com/redwarp/gif-wallpaper/issues/136))
- Bump com.diffplug.spotless from 5.13.0 to 5.14.1 ([#137](https://github.com/redwarp/gif-wallpaper/issues/137))
- Bump libraries
- Update misc.xml
- Bump deps in update_fastlane script
- Bump addressable from 2.7.0 to 2.8.0 ([#143](https://github.com/redwarp/gif-wallpaper/issues/143))
- Bump fastlane and its deps
- Add script to pull translations ([#144](https://github.com/redwarp/gif-wallpaper/issues/144))
- Pull translations and update fastlane
- Bump com.diffplug.spotless from 5.14.1 to 5.14.2 ([#145](https://github.com/redwarp/gif-wallpaper/issues/145))
- Bump gradle and deps, fix jetifier issue with bouncycastle
- Bump the Android Gradle Plugin from 4.2.2 to 7.0.0 ([#146](https://github.com/redwarp/gif-wallpaper/issues/146))
- Idea file change for arctic fox
- Fetch translations

## [1.9.2] - 2021-05-24

### Bug Fixes

- Update privacy policy and about doc
- Crash for Android 12
- Better TextActivity layout for long text

### Miscellaneous Tasks

- New boring privacy policy
- Bump libraries
- Workflow will keep artifact

## [1.9.1] - 2021-05-15

### Bug Fixes

- Update to kotlin 1.5.0
- Set power save mode enabled by default

### Miscellaneous Tasks

- Bump gradle from 4.1.3 to 4.2.0 ([#123](https://github.com/redwarp/gif-wallpaper/issues/123))
- Config for AS 4.2.1
- Update license

## [1.9.0] - 2021-05-01

### Bug Fixes

- Bump navigation libraries
- Update translations from POEditor
- Bigger activate button
- Cleanup deprecated code

### Features

- Add power management option ([#121](https://github.com/redwarp/gif-wallpaper/issues/121))

### Miscellaneous Tasks

- Update pre-commit hooks

## [1.8.1] - 2021-04-29

### Bug Fixes

- Update russian translations

### Miscellaneous Tasks

- Add 1 frame rocket gif for unified screenshots
- Add link for spanish contributor
- Bump com.diffplug.spotless from 5.12.1 to 5.12.4 ([#116](https://github.com/redwarp/gif-wallpaper/issues/116))
- Setup png optimize pre-commit hook.
- Upgrade to GitHub-native Dependabot ([#119](https://github.com/redwarp/gif-wallpaper/issues/119))
- Update pre-commit for png

## [1.8.0] - 2021-04-18

### Bug Fixes

- Invalidate drawable on color change
- Cleanup wallpaper colors recalculation

### Features

- Replace livedata with flows ([#114](https://github.com/redwarp/gif-wallpaper/issues/114))
- Add spanish support

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 5.11.1 to 5.12.0 ([#111](https://github.com/redwarp/gif-wallpaper/issues/111))
- Bump com.diffplug.spotless from 5.12.0 to 5.12.1 ([#112](https://github.com/redwarp/gif-wallpaper/issues/112))
- Update README for link to POEditor
- Prepare spanish localization
- Add spanish contributor

## [1.7.4] - 2021-04-07

### Bug Fixes

- Update theme to pastel

## [1.7.3] - 2021-04-03

### Bug Fixes

- Thread safe-ish-ify the SurfaceDrawableRenderer
- Overflow icon will change color properly

## [1.7.2] - 2021-03-31

### Bug Fixes

- Update gifdecoder to ditch netty

## [1.7.1] - 2021-03-29

### Bug Fixes

- Set Proguard config for netty

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 5.10.2 to 5.11.1 ([#108](https://github.com/redwarp/gif-wallpaper/issues/108))

## [1.7.0] - 2021-03-27

### Features

- Use random file instead of memory for big files ([#109](https://github.com/redwarp/gif-wallpaper/issues/109))

### Miscellaneous Tasks

- Bump com.github.ben-manes.versions from 0.36.0 to 0.38.0 ([#101](https://github.com/redwarp/gif-wallpaper/issues/101))
- Bump kotlin_version from 1.4.31 to 1.4.32 ([#105](https://github.com/redwarp/gif-wallpaper/issues/105))

## [1.6.2] - 2021-02-26

### Bug Fixes

- Prepare to ditch jcenter and update minor deps

### Miscellaneous Tasks

- Update release script to use releaseFiles instead of releaseFile
- Bump gradle from 4.1.1 to 4.1.2 ([#87](https://github.com/redwarp/gif-wallpaper/issues/87))
- Bump robolectric from 4.4 to 4.5 ([#88](https://github.com/redwarp/gif-wallpaper/issues/88))
- Bump robolectric from 4.5 to 4.5.1 ([#89](https://github.com/redwarp/gif-wallpaper/issues/89))
- Bump core from 4.6.1 to 4.6.2 ([#91](https://github.com/redwarp/gif-wallpaper/issues/91))
- Bump kotlin_version from 1.4.21 to 1.4.30 ([#90](https://github.com/redwarp/gif-wallpaper/issues/90))
- Bump com.diffplug.spotless from 5.9.0 to 5.10.0 ([#92](https://github.com/redwarp/gif-wallpaper/issues/92))
- Added examples to show in README.md ([#95](https://github.com/redwarp/gif-wallpaper/issues/95))
- Bump junit from 4.13.1 to 4.13.2 ([#94](https://github.com/redwarp/gif-wallpaper/issues/94))
- Bump com.diffplug.spotless from 5.10.0 to 5.10.1 ([#93](https://github.com/redwarp/gif-wallpaper/issues/93))
- Bump com.diffplug.spotless from 5.10.1 to 5.10.2 ([#96](https://github.com/redwarp/gif-wallpaper/issues/96))

## [1.6.1] - 2021-01-09

### Bug Fixes

- Remove deprecated kotlin android extension
- Prevent crash if locking canvas returns null
- Catch throwable instead of exception, hoping to mitigate OutOfMemory errors. Probably a shot in the dark to be honest

### Miscellaneous Tasks

- Update gradle to 6.8

## [1.6.0] - 2021-01-08

### Features

- Switch to the dedicated gif library and remove java code
- Rewrite the rendering pipeline to be simpler ([#86](https://github.com/redwarp/gif-wallpaper/issues/86))

### Miscellaneous Tasks

- Bump com.diffplug.spotless from 5.8.2 to 5.9.0 ([#84](https://github.com/redwarp/gif-wallpaper/issues/84))
- Fix fastlane job to account for master to main renaming

## [1.5.4] - 2020-12-29

### Bug Fixes

- Fix coroutine dispatcher for invalidateSelf

### Miscellaneous Tasks

- Bump core from 4.6.0 to 4.6.1 ([#82](https://github.com/redwarp/gif-wallpaper/issues/82))

## [1.5.3] - 2020-12-20

### Bug Fixes

- Streamline memory usage by reusing bitmap

## [1.5.2] - 2020-12-20

### Bug Fixes

- Fix delays again, so that proper delay is applied to proper frame

## [1.5.1] - 2020-12-19

### Bug Fixes

- Fix timing of GIFs by using proper time base for animation.
- Fix animation loop by switching back to coroutine. That works

### Miscellaneous Tasks

- Code cleanup

## [1.5.0] - 2020-12-13

### Features

- Add german language to the app.

### Miscellaneous Tasks

- Add russian feature graphic
- Automate fastlane when a file in the fastlane directory is changed (hopefully)
- Bump navigation-fragment-ktx from 2.3.1 to 2.3.2 ([#79](https://github.com/redwarp/gif-wallpaper/issues/79))
- Bump navigation-ui-ktx from 2.3.1 to 2.3.2 ([#78](https://github.com/redwarp/gif-wallpaper/issues/78))
- Bump kotlin_version from 1.4.20 to 1.4.21 ([#80](https://github.com/redwarp/gif-wallpaper/issues/80))
- Prepare for german translation
- Rename file
- More preparation for german translation
- Update fastlane files for german

## [1.4.1] - 2020-11-27

### Bug Fixes

- Fix frame decoding order, it should remove some weird stutter. (The delay of the previous frame would be applied to the current one)

### Documentation

- Add contributors

### Miscellaneous Tasks

- Setup a quick tool to update fastlane from store description json files
- Properly setup fastlane to update store listing
- Bump kotlinx-coroutines-core from 1.4.1 to 1.4.2 ([#75](https://github.com/redwarp/gif-wallpaper/issues/75))
- Update coroutines-android lib

### Localize

- Setup store listing strings

## [1.4.0] - 2020-11-20

### Features

- Support russian language

### Miscellaneous Tasks

- Add badges
- Bump core from 4.5.1 to 4.6.0 ([#45](https://github.com/redwarp/gif-wallpaper/issues/45))
- Bump com.diffplug.spotless from 5.5.1 to 5.5.2 ([#44](https://github.com/redwarp/gif-wallpaper/issues/44))
- Bump core-ktx from 1.3.1 to 1.3.2 ([#50](https://github.com/redwarp/gif-wallpaper/issues/50))
- Bump com.diffplug.spotless from 5.5.2 to 5.6.1 ([#46](https://github.com/redwarp/gif-wallpaper/issues/46))
- Bump constraintlayout from 2.0.1 to 2.0.2 ([#52](https://github.com/redwarp/gif-wallpaper/issues/52))
- Bump gradle from 4.0.1 to 4.0.2 ([#53](https://github.com/redwarp/gif-wallpaper/issues/53))
- Bump junit from 4.13 to 4.13.1 ([#54](https://github.com/redwarp/gif-wallpaper/issues/54))
- Bump gradle from 4.0.2 to 4.1.0 ([#56](https://github.com/redwarp/gif-wallpaper/issues/56))
- Bump navigation-ui-ktx from 2.3.0 to 2.3.1 ([#57](https://github.com/redwarp/gif-wallpaper/issues/57))
- Bump navigation-fragment-ktx from 2.3.0 to 2.3.1 ([#58](https://github.com/redwarp/gif-wallpaper/issues/58))
- Bump com.diffplug.spotless from 5.6.1 to 5.7.0 ([#59](https://github.com/redwarp/gif-wallpaper/issues/59))
- Bump kotlinx-coroutines-core from 1.3.9 to 1.4.0 ([#61](https://github.com/redwarp/gif-wallpaper/issues/61))
- Bump kotlinx-coroutines-android from 1.3.9 to 1.4.0 ([#60](https://github.com/redwarp/gif-wallpaper/issues/60))
- Bump constraintlayout from 2.0.2 to 2.0.3 ([#62](https://github.com/redwarp/gif-wallpaper/issues/62))
- Bump constraintlayout from 2.0.3 to 2.0.4 ([#63](https://github.com/redwarp/gif-wallpaper/issues/63))
- Bump com.github.ben-manes.versions from 0.33.0 to 0.36.0 ([#67](https://github.com/redwarp/gif-wallpaper/issues/67))
- Bump kotlinx-coroutines-core from 1.4.0 to 1.4.1 ([#66](https://github.com/redwarp/gif-wallpaper/issues/66))
- Bump kotlinx-coroutines-android from 1.4.0 to 1.4.1 ([#65](https://github.com/redwarp/gif-wallpaper/issues/65))
- Bump gradle from 4.1.0 to 4.1.1 ([#68](https://github.com/redwarp/gif-wallpaper/issues/68))
- Bump com.diffplug.spotless from 5.7.0 to 5.8.1 ([#69](https://github.com/redwarp/gif-wallpaper/issues/69))
- Bump com.diffplug.spotless from 5.8.1 to 5.8.2 ([#70](https://github.com/redwarp/gif-wallpaper/issues/70))
- Update Android studio files, and setup empty russian language file
- Bump kotlin_version from 1.4.10 to 1.4.20 ([#73](https://github.com/redwarp/gif-wallpaper/issues/73))

### Localize

- Add russian translations

## [1.3.4] - 2020-09-18

### Miscellaneous Tasks

- Bump material from 1.2.0 to 1.2.1 ([#36](https://github.com/redwarp/gif-wallpaper/issues/36))
- Bump com.github.ben-manes.versions from 0.29.0 to 0.30.0 ([#37](https://github.com/redwarp/gif-wallpaper/issues/37))
- Bump com.github.ben-manes.versions from 0.30.0 to 0.31.0 ([#39](https://github.com/redwarp/gif-wallpaper/issues/39))
- Bump com.diffplug.spotless from 5.3.0 to 5.4.0 ([#38](https://github.com/redwarp/gif-wallpaper/issues/38))
- Bump com.github.ben-manes.versions from 0.31.0 to 0.33.0 ([#41](https://github.com/redwarp/gif-wallpaper/issues/41))
- Bump com.diffplug.spotless from 5.4.0 to 5.5.1 ([#40](https://github.com/redwarp/gif-wallpaper/issues/40))
- Bump kotlin_version from 1.4.0 to 1.4.10 ([#42](https://github.com/redwarp/gif-wallpaper/issues/42))
- Update README.md ([#43](https://github.com/redwarp/gif-wallpaper/issues/43))

## [1.3.3] - 2020-08-27

### Bug Fixes

- Replace coroutines with drawable#scheduleSelf

### Miscellaneous Tasks

- Embrace conventional commits ([#31](https://github.com/redwarp/gif-wallpaper/issues/31))
- Update verify workflow
- Cleanup java code ([#33](https://github.com/redwarp/gif-wallpaper/issues/33))

