# Using the AndroidSVG v1.1.182 API #

The API for AndroidSVG is fairly straightforward.  The available methods break down into three types:

  * [Reading in an SVG file](#Reading_in_an_SVG_file.md),
  * [Rendering the file](#Rendering_the_file.md) to either a Canvas or a Picture,
  * [A few support / utility methods](#Support_methods.md)

Each type is summarised below.  For more information, see the [Javadoc](https://androidsvg.googlecode.com/hg/doc/index.html).

## Reading in an SVG file ##

AndroidSVG includes methods for reading an SVG from an `InputStream`, a `String`, an Android resource, or from your app's assets folder.

```java

public static SVG getFromInputStream(java.io.InputStream is) throws SVGParseException

public static SVG getFromString(java.lang.String svg) throws SVGParseException

public static SVG getFromResource(android.content.Context context, int resourceId)
throws SVGParseException

public static SVG getFromAsset(AssetManager assetManager, String filename)
throws SVGParseException, IOException
```

## Rendering the file ##

### `renderToPicture()` ###

```java

public Picture renderToPicture()

public Picture renderToPicture(int widthInPixels, int heightInPixels)

public Picture renderToPicture(int widthInPixels, int heightInPixels, float defaultDPI,
SVG.AspectRatioAlignment alignment, SVG.AspectRatioScale scale)
```

There are two ways to render the file.  One way to to generate a `android.graphics.Picture` object.  A Picture is just a recording of all the Canvas 2D drawing primitives used to render the document.  Once you have the Picture instance, you can output it to a canvas using `Canvas.drawPicture(picture)`.

Using this method is slower than rendering directly to a Canvas (see `renderToCanvas()`), however if you need to draw the same document multiple times, the second and later draws will be substantially faster.

### `renderToCanvas()` ###

The other way to render the SVG is directly to a Canvas.  If you are just needing to render the document once, this is the way you will probably want to do it.

```java

public void renderToCanvas(Canvas canvas)

public void renderToCanvas(Canvas canvas, RectF viewPort)

public void renderToCanvas(Canvas canvas, RectF viewPort, float defaultDPI,
SVG.AspectRatioAlignment alignment, SVG.AspectRatioScale scale)
```

### Rendering Views ###

SVG documents have the concept of a "view".  A view describes a rectangular portion of the document that can be scaled to fill your drawing area.  Views are defined by `<view>` elements in the file.

The following example defines a view 100 pixels square at the top left of the SVG document.

```
  <view id="MyView" viewBox="0 0 100 100"/>
```

AndroidSVG includes methods to render views. There are both Picture and direc-to-Canvas variants.

```java

public Picture renderViewToPicture(String viewId, int widthInPixels, int heightInPixels)

public Picture renderViewToPicture(String viewId, int widthInPixels, int heightInPixels, float defaultDPI)


public void renderViewToCanvas(String viewId, Canvas canvas)

public void renderViewToCanvas(String viewId, Canvas canvas, RectF viewPort)

public void renderViewToCanvas(String viewId, Canvas canvas, RectF viewPort, float defaultDPI)
```

An example of how views might be used is to imagine an SVG of an entire chess set. You could then use views to select and render each of the pieces.


## Support methods ##

### Resolving references to external objects ###

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


### SVG file metadata ###

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

public float getDocumentWidth(float dpi)
```

Returns the width of the document as specified in the SVG file.

```java

public float getDocumentHeight(float dpi)
```

Returns the height of the document as specified in the SVG file.

### Library metadata ###

```java

public String getVersion()
```

Returns the version number of this library.