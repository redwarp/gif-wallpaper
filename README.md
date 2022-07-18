[![GitHub license](https://img.shields.io/github/license/redwarp/gif-wallpaper)](https://github.com/redwarp/gif-wallpaper/blob/main/LICENSE)

[![GitHub top language](https://img.shields.io/github/languages/top/redwarp/gif-wallpaper)](https://github.com/redwarp/gif-wallpaper/search?l=kotlin) [![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/redwarp/gif-wallpaper)](https://github.com/redwarp/gif-wallpaper/releases) [![F-Droid (including pre-releases)](https://img.shields.io/f-droid/v/net.redwarp.gifwallpaper)](https://f-droid.org/packages/net.redwarp.gifwallpaper)

# gif-wallpaper

Playing with GIFs, animatable and drawable

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height=50>](https://f-droid.org/packages/net.redwarp.gifwallpaper)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height=50>](https://play.google.com/store/apps/details?id=net.redwarp.gifwallpaper)

## Inspiration

This app was created because I wanted to display as a wallpaper the animated ninja turtles of
Alex Redfish. [Check their art](https://www.artstation.com/artwork/5wm5W), it is dope.

## Other cool gifs

* [JN3008](https://jn3008.tumblr.com/)
* [Mad Max](https://www.behance.net/gallery/26428843/MAD-MAX-Fury-Road)
* [SeerLight 🌙✨](https://twitter.com/seerlight)
* [SlimJim](http://www.slimjimstudios.com/#/la-gifathon/)
* [Waneella](https://twitter.com/waneella_/)

## Contributors

* [Poussinou](https://github.com/Poussinou) - F-Droid and Google Play Store links
* Mister klaid - Russian translations
* [YesIanYeha](https://github.com/Preyesianyeha) - Spanish translations
* mzzg - Simplified Chinese translations

## Translations

You want to help with translations? You find your language missing and want to help?
That is awesome!
I use the website **POEditor** to handle translations. They are free for open source project and easy to use, and provide synchronisation with GitHub. So follow [this link to join the translations project](https://poeditor.com/join/project?hash=QaDkuFZTp2).

### Steps when adding a new language

* Edit the [`config.json`](fetch-translations/config.json) file in the `fetch-translations` folder to add the new language, and map the POEditor values with Android's value folder, and fastlane supported language list.
* Run `cargo run --bin fetch-translations`, verify that a new strings.xml files was created in the Android project.
* Run `cargo run --bin update-fastlane`, verify that new metadata files were added in the fastlane folder.
* Edit the [`app/build.gradle`](app/build.gradle) file and add to the res config the new language code.
* Run the app in an emulator or device set on the chosen language, verify it shows properly, and take the 5 screenshots needed for fastlane. At some point, I should automate that with Picasso, but we are not there yet.

## Release

Writing that here to remember all the steps.

### Step for creating a new release

* Make sure you are on the `main` branch, and that the git repo is clean
* Run `cargo run --bin release-process`. It will calculate the next version based on conventional commits, generate changelogs, create and tag a new commit with the version.
* Verify the new commit looks good, changelogs as well.
* Run `git push --follow-tags`
* On github, check the [tags](https://github.com/redwarp/gif-wallpaper/tags) page, find your new tag there, and create a release from the tag.
