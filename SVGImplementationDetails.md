# SVG implementation details #

AndroidSVG supports almost all of the graphical features defined in the SVG 1.1 and SVG 1.2 Tiny specifications.  The main missing features are
animation, filters and SVG fonts.  AndroidSVG is intended only as a static renderer, so animation will probably never be added. However filters and embedded fonts may be supported in the future.

A lot of effort has been put into making AndroidSVG's rendering accurate.  For example, AndroidSVG correctly renders the [SVG Acid Test](http://www.codedread.com/acid/acid1.html).

![http://i.imgur.com/ZaOi6rO.png](http://i.imgur.com/ZaOi6rO.png)

SVG Acid test: _Note that this file has been slightly modified in the following ways:
  1. an external CSS style sheet has been inlined, and
  1. one CSS selector type that is not currently supported was converted to a style attribute._

The modified file is available on the [downloads page](http://code.google.com/p/androidsvg/downloads/list).


## Elements ##

|**SVG Element**      | **Name**                     | **Release** | **Implementation notes** |
|:--------------------|:-----------------------------|:------------|:-------------------------|
|`<circle>`           | Circle                       | 1.0         | Fully supported.         |
|`<clipPath>`         | Clipping paths               | 1.0         | Fully supported except that `<textPath>` elements cannot be used in clip paths. |
|`<defs>`             | Definitions                  | 1.0         | Fully supported.         |
|`<desc>`             | Description                  | 1.0         | Supported for the root `<svg>` element only. |
|`<ellipse>`          | Ellipse                      | 1.0         | Fully supported.         |
|`<g>`                | Group                        | 1.0         | Fully supported.         |
|`<image>`            | Embed an image               | 1.0         | Fully supported for bitmaps only (including data URLs). Embedding of external SVG documents is not supported. |
|`<line>`             | Line                         | 1.0         | Fully supported.         |
|`<linearGradient>`   | Linear gradient              | 1.0         | Fully supported.         |
|`<marker>`           | Marker                       | 1.0         | Fully supported.         |
|`<mask>`             | Mask                         | 1.0         | Fully supported when using `renderToCanvas()`. |
|`<path>`             | Path                         | 1.0         | Fully supported.         |
|`<pattern>`          | Pattern                      | 1.0         | Supported for fills only. Patterns do not work for `<text>` of `<textPath>` elements at present. Better support for patterns is coming in a future version. |
|`<polygon>`          | Polygon                      | 1.0         | Fully supported.         |
|`<polyline>`         | Polyline                     | 1.0         | Fully supported.         |
|`<radialGradient>`   | Radial gradient              | 1.0         | Fully supported except for `fx` and `fy` attributes. SVG radial gradients allow you to specify a focus point (fx,fy) that is distinct from the centre point (cx,cy), but radial gradients in the Android 2D drawing library do not support this feature. |
|`<rect>`             | Rectangle                    | 1.0         | Fully supported.         |
|`<solidColor>`       | Solid colour references      | 1.2         | Fully supported          |
|`<stop>`             | Gradient stop                | 1.0         | Fully supported.         |
|`<style>`            | CSS stylesheet               | 1.1         | Supported with some limitations. Seel below.|
|`<svg>`              | SVG                          | 1.0         | Fully supported.         |
|`<switch>`           | Conditional processing       | 1.0         | Fully supported except that AndroidSVG does not support extensions so `requiredExtensions` is not supported, but the others are (including those defined in SVG 1.2 Tiny). |
|`<symbol>`           | Symbol                       | 1.0         | Fully supported.         |
|`<text>`             | Text                         | 1.0         | Moderate support. The most common features are supported but not more advanced features like fonts, glyphs and individual character transforms.|
|`<textPath>`         | Text on a path               | 1.0         | Basic support. `<textPath>` elements can not be pattern filled or used in clipping paths. |
|`<title>`            | Title                        | 1.0         | Supported for the root `<svg>` element only. |
|`<tref>`             | Text reference               | 1.0         | Fully supported.         |
|`<tspan>`            | Text span                    | 1.0         | Supported (see `<text>`). |
|`<use>`              | Re-use a predefined object   | 1.0         | Fully supported.         |
|`<view>`             | Document view                | 1.0         | Fully supported.         |


## Style attributes ##

|**Attribute**             | **Release** | **Implementation notes** |
|:-------------------------|:------------|:-------------------------|
|`clip`                    | 1.0         | Fully supported          |
|`clip-path`               | 1.0         | Fully supported          |
|`clip-rule`               | 1.0         | Fully supported          |
|`display`                 | 1.0         | Fully supported          |
|`fill`                    | 1.0         | Fully supported          |
|`fill-rule`               | 1.0         | Fully supported          |
|`fill-opacity`            | 1.0         | Fully supported          |
|`color`                   | 1.0         | Fully supported          |
|`font`                    | 1.0         | SVG fonts not are supported but you can load external font files using the `SVGExternalFileResolver` class.. |
|`font-family`             | 1.0         | Fully supported except for generic font families like serif, fantasy etc. (see [issue 1](https://code.google.com/p/androidsvg/issues/detail?id=1)) |
|`font-size`               | 1.0         | Fully supported          |
|`font-weight`             | 1.0         | Fully supported          |
|`font-style`              | 1.0         | Fully supported          |
|`marker`                  | 1.0         | Fully supported          |
|`marker-start`            | 1.0         | Fully supported          |
|`marker-mid`              | 1.0         | Fully supported          |
|`marker-end`              | 1.0         | Fully supported          |
|`mask`                    | 1.0         | Fully supported, but only when using `renderToCanvas()`. Note that masks are (currently) slow and quite memory intensive. Use with care - especially on low spec devices. |
|`opacity`                 | 1.0         | Fully supported          |
|`overflow`                | 1.0         | Fully supported          |
|`stop-color`              | 1.0         | Fully supported          |
|`stop-opacity`            | 1.0         | Fully supported          |
|`stroke`                  | 1.0         | All stroke styles are supported except for patterns |
|`stroke-opacity`          | 1.0         | Fully supported          |
|`stroke-width`            | 1.0         | Fully supported          |
|`stroke-linecap`          | 1.0         | Fully supported          |
|`stroke-linejoin`         | 1.0         | Fully supported          |
|`stroke-miterlimit`       | 1.0         | Fully supported          |
|`stroke-dasharray`        | 1.0         | Fully supported          |
|`stroke-dashoffset`       | 1.0         | Fully supported          |
|`text-anchor`             | 1.0         | Fully supported          |
|`text-decoration`         | 1.0         | Fully supported. Due to an Android bug, Underlines and strikethroughs will not be stroked on pre Jellybean 4.2 devices. |
|`vector-effect`           | 1.2         | Fully supported          |
|`viewport-fill`           | 1.2         | Fully supported          |
|`viewport-fill-opacity`   | 1.2         | Fully supported          |
|`visibility`              | 1.0         | Fully supported          |


## CSS support ##

CSS support via the `<style>` element was added in version 1.1 of AndroidSVG.  Most features of CSS 2.1 are implemented with a few exceptions.

### The `<style>` element ###

The `<style>` element attributes `type` and `media` are supported.

### Selectors ###

Almost all CSS2.1 selectors are supported except for most pseudo and attribute selectors.

  * Simple selectors like `E`, `E F`, `*`, `E.class`, `E#id` all work as expected.
  * The `E > F` and `E + F` combinators are supported.
  * `E:first-child` is supported, but other pseudo selectors are ignored because they don't make sense in the Android context.
  * `E[id="foo"]` and `E[class="foo"]` are supported, but matching on other attributes will not work.
  * Pseudo elements like `:before` and `:after` are not supported.

### "At" rules ###

`@media` is supported, but other rules like `@charset`, `@import` are not.