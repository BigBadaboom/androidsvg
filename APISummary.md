# Using the AndroidSVG version 1.2 API #

The API for AndroidSVG is fairly straightforward.  The available methods break down into three types:

  * [Reading in an SVG file](#Reading_in_an_SVG_file.md)
  * [Rendering the file](#Rendering_the_file.md) to either a Canvas or a Picture
  * [Rendering views](#Rendering_views.md)
  * [Resolving references to external objects](#Resolving_references_to_external_objects.md)
  * [SVG file metadata](#SVG_file_metadata.md)
  * [Modifying the SVG document metadata](#Modifying_the_SVG_document_metadata.md),
  * [Library metadata](#Library_metadata.md)

Each type is summarised below.  For more information, see the [Javadoc](https://androidsvg.googlecode.com/hg/doc/index.html).

## Reading in an SVG file ##

AndroidSVG includes methods for reading an SVG from an `InputStream`, a `String`, an Android resource, or from your app's assets folder.

```java


public static SVG getFromInputStream(java.io.InputStream is) throws SVGParseException

public static SVG getFromString(java.lang.String svg) throws SVGParseException

public static SVG getFromResource(android.content.Context context, int resourceId) throws SVGParseException

public static SVG getFromAsset(AssetManager assetManager, String filename) throws SVGParseException, IOException
```

## Rendering the file ##

### `renderToPicture()` ###

```java


public Picture renderToPicture()

public Picture renderToPicture(int widthInPixels, int heightInPixels)
```

There are two ways to render the file.  One way to to generate a `android.graphics.Picture` object.  A Picture is just a recording of all the Canvas 2D drawing primitives used to render the document.  Once you have the Picture instance, you can output it to a canvas using `Canvas.drawPicture(picture)`.

Using this method is slower than rendering directly to a Canvas (see `renderToCanvas()`), however if you need to draw the same document multiple times, the second and later draws will be substantially faster.

### `renderToCanvas()` ###

The other way to render the SVG is directly to a Canvas.  If you are just needing to render the document once, this is the way you will probably want to do it.

```java


public void renderToCanvas(Canvas canvas)

public void renderToCanvas(Canvas canvas, RectF viewPort)
```

### Rendering views ###

SVG documents have the concept of a "view".  A view describes a rectangular portion of the document that can be scaled to fill your drawing area.  Views are defined by `<view>` elements in the file.

The following example defines a view 100 pixels square at the top left of the SVG document.

```
<view id="MyView" viewBox="0 0 100 100"/>
```

AndroidSVG includes methods to render views. There are both Picture and direct-to-Canvas variants.

```java


public Picture renderViewToPicture(String viewId, int widthInPixels, int heightInPixels)

public void renderViewToCanvas(String viewId, Canvas canvas)

public void renderViewToCanvas(String viewId, Canvas canvas, RectF viewPort)

```

An example of how views might be used is to imagine an SVG of an entire chess set. You could then use views to select and render each of the pieces.


### Rendering DPI ###

By default, the DPI value used to convert real world units such as "cm" or "pt" is 96.  This is the value recommended by the CSS spec for consistency with browsers and other renderers.

There may be some occasions when you want to use a different value.  As an example, you may want to render a one inch object at near-actual size on your device.  To achieve that, use the `SVG.setRenderDPI()` call.

```java


public void  setRenderDPI(float dpi)

public float getRenderDPI()

```

To set the render DPI to the DPI value of your device's screen, you could use the following code.

```java


my_svg.setRenderDPI(getResources().getDisplayMetrics().xdpi);

```


## Resolving references to external objects ##

```java


public void registerExternalFileResolver(SVGExternalFileResolver fileResolver)

```

Register a class that resolves references to external fonts and images.  For example, a very simple resolver that just looks in your assets folder might look something like the following:

```java


private SVGExternalFileResolver  myResolver = new SVGExternalFileResolver() {

@Override
public Typeface resolveFont(String fontFamily, int fontWeight, String fontStyle)
{
return Typeface.createFromAsset(getContext().getAssets(), fontFamily + ".ttf");
}

@Override
public Bitmap resolveImage(String filename)
{
try
{
InputStream  istream = getContext().getAssets().open(filename);
return BitmapFactory.decodeStream(istream);
}
catch (IOException e1)
{
return null;
}
}
};

```


## SVG file metadata ##

```java


public String getDocumentTitle()

```

Returns the contents of the `<title>` element in the SVG document.  Title elements can be present throughout the file, but only the value in the root `<svg>` element is accessible through this method.


```java


public String getDocumentDescription()

```

Returns the contents of the `<desc>` element in the SVG document.
Title elements can be present throughout the file, but only the value in the root `<svg>` element is accessible through this method.

```java


public String getDocumentSVGVersion()

```

Returns the SVG version number as provided in the root `<svg>` tag of the document.


```java


public Set<String> getViewList()

```

Returns a list of `id`s for all `<view>` elements in this SVG document.


```java


public float getDocumentWidth()

```

Returns the width of the document as specified in the SVG file.


```java


public float getDocumentHeight()

```

Returns the height of the document as specified in the SVG file.


```java


public android.graphics.RectF getDocumentViewBox()

```

Returns the viewBox of the document as specified in the SVG file.


```java


public PreserveAspectRatio getDocumentPreserveAspectRatio()

```

Returns the "preserveAspectRatio attribute of the document as specified in the SVG file.


```java


public float getDocumentAspectRatio()

```

Returns the aspect ratio (width/height) of the SVG document.

If the width or height of the document are listed with a physical unit such as "cm", then the dpi parameter will be used to convert that value to pixels. It is safe to pass a value of 0 for dpi if you know that physical units are not being used.

If the width or height cannot be determined, -1 will be returned.


## Modifying the SVG document metadata ##

A limited degree of modification is supported once you have loaded the SVG file. Some methods have been added in release 1.2 to allow you to modify the root `<svg>` element in order to alter the way it is rendered. More specifically, how it is scaled and positioned.

```java


public void  setDocumentWidth(float pixels)

public void  setDocumentWidth(String value)

```

Sets the width of the document as a pixel value, or a length value such as "100px", "50%", or "5cm".

```java


public void  setDocumentHeight(float pixels)

public void  setDocumentHeight(String value)

```

Sets the height of the document as a pixel value, or a length value such as "100px", "50%", or "5cm".

```java


public void  setDocumentViewBox(float minX, float minY, float width, float height)

```

Sets the height of the document as a pixel value, or a length value such as "100px", "50%", or "5cm".


```java


public void setDocumentPreserveAspectRatio(PreserveAspectRatio preserveAspectRatio)

```

Change the document positioning by altering the "preserveAspectRatio" attribute of the root `<svg>` element.


### The PreserveAspectRatio class ###

The PreserveAspectRatio class represents the `preserveAspectRatio` attribute of `<svg>` elements.

To create an instance you can call the constructor with an alignment and scale value.

```java


PreservAspectRatio p = new PreserveAspectRatio(PreserveAspectRatio.Alignment.XMaxYMax,
PreserveAspectRatio.Scale.Slice);
my_svg.setDocumentPreserveAspectRatio(p);

```

Alternatively, you can use one of the predefined constants which represent some of the more useful combinations of alignment and scale.

```java


PreservAspectRatio.UNSCALED
PreservAspectRatio.STRETCH
PreservAspectRatio.LETTERBOX
PreservAspectRatio.START
PreservAspectRatio.END
PreservAspectRatio.TOP
PreservAspectRatio.BOTTOM
PreservAspectRatio.FULLSCREEN
PreservAspectRatio.FULLSCREEN_START

```

The definitions of each of these constants can be found in the [Javadoc](https://androidsvg.googlecode.com/hg/doc/reference/com/caverock/androidsvg/PreserveAspectRatio.html).

## Library metadata ##

```java

public String getVersion()
```

Returns the version number of this library.