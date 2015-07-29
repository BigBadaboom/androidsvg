# Frequently Asked Questions #


## Rendering issues ##

  * [My SVG doesn't render if I try to do canvas.drawPicture() on the Picture returned by SVG.renderToPicture()](#My_SVG_doesn't_render_if_I_try_to_do_Canvas.drawPicture()_o.md)
  * [The &lt;clipPath&gt; element doesn't work on newer versions of Android](#The_clipPath_element_doesn't_work_on_newer_versions_of_Andr.md)
  * [The &lt;clipPath&gt; element doesn't work on my Android 4.3 device, even with hardware acceleration disabled](#The_clipPath_element_doesn't_work_on_my_Android_4.3_device,.md)

## Scaling issues ##
  * [My SVG won't scale to fit the canvas or my ImageView](#My_SVG_won't_scale_to_fit_the_canvas_or_my_ImageView.md)
  * [My document has a viewBox, but it is still not scaling](#My_document_has_a_viewBox,_but_it_is_still_not_scaling.md)
  * [Dealing with Inkscape files](#Dealing_with_Inkscape_files.md)
  * [My document used to scale okay prior to version 1.2.0, but doesn't now](#My_document_used_to_scale_okay_prior_to_version_1.2.0,_but_doesn.md)
  * [ScaleType.CENTER doesn't work when using an ImageView or SVGImageView](#ScaleType_.CENTER_doesn't_work_when_using_an_ImageView_or_S.md)

---



## My SVG doesn't render if I try to do Canvas.drawPicture() on the Picture returned by SVG.renderToPicture() ##

If the device you are testing on has Ice Cream Sandwich or later (API 14+), then hardware acceleration of the 2D rendering pipeline is enabled by default.

Unfortunately the hardware accelerated graphics pipeline does not support all of the drawing methods provided by `Canvas`.  One of the methods that is not supported is `drawPicture()`.

For more information on which methods are not supported, see the _Unsupported Drawing Operations_ section of the [Hardware Acceleration](http://developer.android.com/guide/topics/graphics/hardware-accel.html) page on the Android developers site.

### Solution ###

The solution is to disable hardware acceleration for the View into which you are drawing the SVG.  If you are using the supplied `SVGImageView` class in your layouts, then this is done for you.  You don't need to do anything.

To switch the view back to software rendering, use the `View.setLayerType()` method.

```
setLayerType(LAYER_TYPE_SOFTWARE, null);
canvas.drawPicture( svg.renderToPicture(canvas.getWidth(), canvas.getHeight()) );
```

Unfortunately there is one little problem with this solution.  The `setLayerType()` method was only added in API 11 (Honeycomb).  This means that you cannot call it directly if you want to support earlier versions of Android.

The way around this is to only call the method on Honeycomb or later.  We list three possible approaches below:

#### (1) The support library approach ####

If you are using the v4 support library in your project (which is also included with the latest v7 appcompat library), you can simply use:

```
import android.support.v4.view.ViewCompat;

ViewCompat.setLayerType(view, ViewCompat.LAYER_TYPE_SOFTWARE, null);
```

This method will perform all the necessary version checking for you.


#### (2) The normal recommended approach ####

If you are targetting API 11 or later, you can use the constants in the `android.os.Build` class to prevent `setLayerType()` being invoked when it is not available on the user's device.

```
import android.os.Build.*;

@TargetApi(VERSION_CODES.HONEYCOMB)
public static void setSoftwareLayerType(View view)
{
   if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
      view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
   } // else: no layer support, so no hardware support to disable
}
```

The `@TargetApi` suppresses warnings about `setLayerType()` not being available if your `minSdkVersion` is set lower than 11.  However `@TargetApi` is only available from API 16 on.  So if your `targetSdkVersion` is lower than that, you will need to remove it (and live with any lint warnings).


#### (3) The old-style reflection approach ####

In almost all cases you will be able to use one of the first two approaches.  We recommend you use one of them if you can.  However some people may be forced for some reason to use an old build environment (before API 11).  If that is your situation, you can use Java reflection to check whether the `setLayerType()` method exists before calling it.

Add the following method to your View class and call it instead of the built-in `setLayerType()` method.

```
/*
 * Use reflection to call setLayerType().
 */
private void  setSoftwareLayerType()
{
   try
   {
      Method  setLayerTypeMethod = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
      int     LAYER_TYPE_SOFTWARE = View.class.getField("LAYER_TYPE_SOFTWARE").getInt(null);
      setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
   }
   catch (NoSuchMethodException e)
   {
      // Android platform too old. No setLayerType.
   }
   catch (Exception e)
   {
      Log.w("MySVGView", "Unexpected failure calling setLayerType", e);
   }
}
```

```
setSoftwareLayerType();
canvas.drawPicture( svg.renderToPicture(canvas.getWidth(), canvas.getHeight()) );
```


_Thanks to Robert Papp for contributing to this answer._



---



## The `clipPath` element doesn't work on newer versions of Android ##

This is caused by the hardware accelerated rendering pipeline not supporting the `Canvas.clipPath()` method, which is used by AndroidSVG.

### Solution ###

The fix is to disable hardware acceleration.  See the answer above for instructions.



---



## The `clipPath` element doesn't work on my Android 4.3 device, even with hardware acceleration disabled ##

There is a bug in Android 4.3 which stops clip paths working.  There is no workaround unfortunately.



---


## My SVG won't scale to fit the canvas or my ImageView ##

In order for SVG documents to be scaled, the document must have a `viewBox` attribute in the root (outermost) `<svg>` element.

See http://www.w3.org/TR/SVG/coords.html#ViewBoxAttribute

For example:

```
<svg xmlns:svg="http://www.w3.org/2000/svg" version="1.1"
   viewBox="0 0 100 100">

  <circle cx="50" cy="50" r="50" fill="red" />

</svg>
```

The purpose of the viewBox attribute is to describe the bounding box of the content.  Without it, the renderer has no idea how much the document needs to be scaled to fit the viewport. The viewport is usually the rectangular extent of your canvas.

Most SVG editors will automatically add the correct viewBox for your file.

### Solution ###

Add a viewBox manually to the file, or use the `setDocumentViewBox()` method to add one at run-time.

So, for example if the interesting part of your SVG file is in the area (0,0) to (300,200), the viewBox attribute should be:

```
<svg viewBox="0 0 300 200" ...etc...>
```

or to set that in code, use:

```
svg.setDocumentViewBox(0, 0, 300, 300);
```



---



## My document has a viewBox, but it is still not scaling ##

If your document specifies a width and height, then those will determine the size of your viewport.  The document will be scaled to those dimensions, not your canvas.

### Solution ###

In your SVG file, set the `width` and `height` attributes to `"100%"`, or remove them entirely (if `width` or `height` are not specified, they default to `100%`).

So your document should look like this:

```
<svg width="100%" height="100%" ...etc...>
```

or to set that in code, use:

```
svg.setDocumentWidth("100%");
svg.setDocumentHeight("100%");
```



---



## Dealing with Inkscape files ##

Inkscape files have hard-coded width and height values but no viewBox.  So they will not automatically scale to your canvas.  To fix this, you need to do a combination of the above two solutions.

### Solution ###

The following solution should work for most Inkscape files.

```
// Set the viewBox attribute using the document's width and height
svg.setDocumentViewBox(0, 0, svg.getDocumentWidth(),
                             svg.getDocumentHeight());

// Now set width and height to 100% so it will scale to fit the canvas
svg.setDocumentWidth("100%");
svg.setDocumentHeight("100%");
```

You may also want to use `setDocumentPreserveAspectRatio()` to tell AndroidSVG what type of scaling to use.  However the default will usually be what you want.



---



## My document used to scale okay prior to version 1.2.0, but doesn't now ##

In previous versions of the library, I was being a bit over-enthusiastic and auto-scaling documents that didn't have a viewBox.  That code worked in some situations, but not others, and made some assumptions that meant that the scaling wouldn't necessarily be correct anyway.

I decided to remove that code and instead provide some API methods that would allow users to have better control over how scaling happens.

My apologies if this change broke your code, but I felt it was important that I brought AndroidSVG into line with other renderers.  These files would not scale if rendered in other renderers either.

### Solution ###

Ensure you have appropriate vaules for `viewBox`, `width` and `height.  See previous questions for more information.



---


## ScaleType.CENTER doesn't work when using an ImageView or SVGImageView ##

`ScaleType.CENTER` doesn't work in these situations.  Use `ScaleType.CENTER_INSIDE` instead.

```
SVGImageView  iv = new SVGImageView(this);
iv.setImageAsset("test.svg");
lv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
layout.addView(iv);
```