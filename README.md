# Overview

Phone Art is a simple drawing app I built to develop my Android skills and learn more about drawing input, custom UI, and saving app data locally. Users can draw on the canvas with a finger or stylus, choose from a color palette, adjust brush size, erase, undo the last stroke, and clear the canvas. The app automatically saves the current drawing and also lets users save named drawings, reopen them, or delete them from **My art**.

My purpose was to practice building an interactive Android app and understand how touch input, drawing, and local persistence work together.

# Software Demo Video (https://youtu.be/EiOiiPrQcA8)

# Development Environment

I used Android Studio and Gradle to develop and build the app. I used the Android emulator to try launching it.

The app is written in Kotlin with Jetpack Compose. Compose provides the UI, canvas drawing, and pointer input. Kotlin coroutines run drawing file reads and writes off the UI thread. The app uses Android’s built-in JSON APIs and private app storage for local saves.

# Useful Websites

* [Jetpack Compose graphics and drawing](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
* [Pointer input in Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input)
* [Kotlin coroutines overview](https://kotlinlang.org/docs/coroutines-overview.html)

# Future Work

* Create and link the required demonstration video.
* Add PNG export so artwork can be viewed and shared outside the app.
* Improve drawing performance for long strokes and large artwork files.
