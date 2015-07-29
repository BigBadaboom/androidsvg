# Known Issues #

## Renderer features that do not work on some versions of Android ##

  * **Stroking of underlined or strikethrough text is broken** in versions of Android **prior to 4.2**.  AndroidSVG will not attempt to stroke underlines or strikethroughs when running on Android 4.1 or earlier.

  * There is a bug in **Android 4.3** that has the effect of **breaking the `<clipPath>` feature when using `renderToPicture()`**.  Any SVG files that use a `<clipPath>` will not render correctly. See [Android issue 58737](https://code.google.com/p/android/issues/detail?id=58737).  If you are using `renderToCanvas()`, you will not be affected.