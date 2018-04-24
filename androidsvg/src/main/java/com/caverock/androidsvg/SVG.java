/*
   Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.caverock.androidsvg;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;

import com.caverock.androidsvg.CSSParser.Ruleset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AndroidSVG is a library for reading, parsing and rendering SVG documents on Android devices.
 * <p>
 * All interaction with AndroidSVG is via this class.
 * <p>
 * Typically, you will call one of the SVG loading and parsing classes then call the renderer,
 * passing it a canvas to draw upon.
 *
 * <h4>Usage summary</h4>
 *
 * <ul>
 * <li>Use one of the static {@code getFromX()} methods to read and parse the SVG file.  They will
 * return an instance of this class.
 * <li>Call one of the {@code renderToX()} methods to render the document.
 * </ul>
 *
 * <h4>Usage example</h4>
 *
 * <pre>
 * {@code
 * SVG.registerExternalFileResolver(myResolver);
 *
 * SVG  svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
 *
 * Bitmap  newBM = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
 * Canvas  bmcanvas = new Canvas(newBM);
 * bmcanvas.drawRGB(255, 255, 255);  // Clear background to white
 *
 * svg.renderToCanvas(bmcanvas);
 * }
 * </pre>
 *
 * For more detailed information on how to use this library, see the documentation at {@code http://code.google.com/p/androidsvg/}
 */

public class SVG
{
   private static final String  TAG = "AndroidSVG";

   private static final String  VERSION = "1.2.3-beta-1";

   private static final int     DEFAULT_PICTURE_WIDTH = 512;
   private static final int     DEFAULT_PICTURE_HEIGHT = 512;

   private static final double  SQRT2 = 1.414213562373095;

   // Resolver
   private static SVGExternalFileResolver  externalFileResolver = null;

   // Parser configuration
   private static boolean  enableInternalEntities = true;

   // The root svg element
   private Svg     rootElement = null;

   // Metadata
   private String  title = "";
   private String  desc = "";

   // DPI to use for rendering
   private float   renderDPI = 96f;   // default is 96

   // CSS rules
   private Ruleset  cssRules = new Ruleset();

   // Map from id attribute to element
   private Map<String, SvgElementBase> idToElementMap = new HashMap<>();


   enum Unit
   {
      px,
      em,
      ex,
      in,
      cm,
      mm,
      pt,
      pc,
      percent
   }


   @SuppressWarnings("unused")
   enum GradientSpread
   {
      pad,
      reflect,
      repeat
   }


   /* package private */
   SVG()
   {
   }


   /**
    * Read and parse an SVG from the given {@code InputStream}.
    *
    * @param is the input stream from which to read the file.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromInputStream(InputStream is) throws SVGParseException
   {
      SVGParser  parser = new SVGParser();
      return parser.parse(is, enableInternalEntities);
   }


   /**
    * Read and parse an SVG from the given {@code String}.
    *
    * @param svg the String instance containing the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public static SVG  getFromString(String svg) throws SVGParseException
   {
      SVGParser  parser = new SVGParser();
      return parser.parse(new ByteArrayInputStream(svg.getBytes()), enableInternalEntities);
   }


   /**
    * Read and parse an SVG from the given resource location.
    *
    * @param context the Android context of the resource.
    * @param resourceId the resource identifier of the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromResource(Context context, int resourceId) throws SVGParseException
   {
      return getFromResource(context.getResources(), resourceId);
   }


   /**
    * Read and parse an SVG from the given resource location.
    *
    * @param resources the set of Resources in which to locate the file.
    * @param resourceId the resource identifier of the SVG document.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromResource(Resources resources, int resourceId) throws SVGParseException
   {
      SVGParser    parser = new SVGParser();
      InputStream  is = resources.openRawResource(resourceId);
      try {
         return parser.parse(is, enableInternalEntities);
      } finally {
         try {
            is.close();
         } catch (IOException e) {
            // Do nothing
         }
      }
   }


   /**
    * Read and parse an SVG from the assets folder.
    *
    * @param assetManager the AssetManager instance to use when reading the file.
    * @param filename the filename of the SVG document within assets.
    * @return an SVG instance on which you can call one of the render methods.
    * @throws SVGParseException if there is an error parsing the document.
    * @throws IOException if there is some IO error while reading the file.
    */
   @SuppressWarnings("WeakerAccess")
   public static SVG  getFromAsset(AssetManager assetManager, String filename) throws SVGParseException, IOException
   {
      SVGParser    parser = new SVGParser();
      InputStream  is = assetManager.open(filename);
      try {
         return parser.parse(is, enableInternalEntities);
      } finally {
         try {
            is.close();
         } catch (IOException e) {
            // Do nothing
         }
      }
   }


   //===============================================================================


   /**
    * Tells the parser whether to allow the expansion of internal entities.
    * An example of document containing an internal entities is:
    *
    * {@code
    * <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.0//EN" "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd" [
    *   <!ENTITY hello "Hello World!">
    * ]>
    * <svg>
    *    <text>&hello;</text>
    * </svg>
    * }
    *
    * Entities are useful in some circumstances, but SVG files that use them are quite rare.  Note
    * also that enabling entity expansion makes you vulnerable to the
    * <a href="https://en.wikipedia.org/wiki/Billion_laughs_attack">Billion Laughs Attack</a>
    *
    * Entity expansion is enabled by default.
    *
    * @param enable Set true if you want to enable entity expansion by the parser.
    * @since 1.3
    */
   public static void  setInternalEntitiesEnabled(boolean enable)
   {
      enableInternalEntities = enable;
   }

   /**
    * @return true if internal entity expansion is enabled in the parser
    * @since 1.3
    */
   public static boolean  isInternalEntitiesEnabled()
   {
      return enableInternalEntities;
   }


   /**
    * Register an {@link SVGExternalFileResolver} instance that the renderer should use when resolving
    * external references such as images, fonts, and CSS stylesheets.
    *
    * @param fileResolver the resolver to use.
    */
   public static void  registerExternalFileResolver(SVGExternalFileResolver fileResolver)
   {
      externalFileResolver = fileResolver;
   }


   /**
    * De-register the current {@link SVGExternalFileResolver} instance.
    */
   public static void  deregisterExternalFileResolver()
   {
      externalFileResolver = null;
   }


   /**
    * Set the DPI (dots-per-inch) value to use when rendering.  The DPI setting is used in the
    * conversion of "physical" units - such an "pt" or "cm" - to pixel values.  The default DPI is 96.
    * <p>
    * You should not normally need to alter the DPI from the default of 96 as recommended by the SVG
    * and CSS specifications.
    *
    * @param dpi the DPI value that the renderer should use.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setRenderDPI(float dpi)
   {
      this.renderDPI = dpi;
   }


   /**
    * Get the current render DPI setting.
    * @return the DPI value
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getRenderDPI()
   {
      return renderDPI;
   }


   //===============================================================================
   // SVG document rendering to a Picture object (indirect rendering)


   /**
    * Renders this SVG document to a Picture object.
    * <p>
    * An attempt will be made to determine a suitable initial viewport from the contents of the SVG file.
    * If an appropriate viewport can't be determined, a default viewport of 512x512 will be used.
    *
    * @return a Picture object suitable for later rendering using {@code Canvas.drawPicture()}
    */
   @SuppressWarnings("WeakerAccess")
   public Picture  renderToPicture()
   {
      // Determine the initial viewport. See SVG spec section 7.2.
      Length  width = rootElement.width;
      if (width != null)
      {
         float w = width.floatValue(this.renderDPI);
         float h;
         Box  rootViewBox = rootElement.viewBox;

         if (rootViewBox != null) {
            h = w * rootViewBox.height / rootViewBox.width;
         } else {
            Length  height = rootElement.height;
            if (height != null) {
               h = height.floatValue(this.renderDPI);
            } else {
               h = w;
            }
         }
         return renderToPicture( (int) Math.ceil(w), (int) Math.ceil(h) );
      }
      else
      {
         return renderToPicture(DEFAULT_PICTURE_WIDTH, DEFAULT_PICTURE_HEIGHT);
      }
   }


   /**
    * Renders this SVG document to a Picture object.
    *
    * @param widthInPixels the width of the initial viewport
    * @param heightInPixels the height of the initial viewport
    * @return a Picture object suitable for later rendering using {@code Canvas.drawPicture()}
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderToPicture(int widthInPixels, int heightInPixels)
   {
      Picture  picture = new Picture();
      Canvas   canvas = picture.beginRecording(widthInPixels, heightInPixels);
      Box      viewPort = new Box(0f, 0f, (float) widthInPixels, (float) heightInPixels);

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, this.renderDPI);

      renderer.renderDocument(this, viewPort, null, null);

      picture.endRecording();
      return picture;
   }


   /**
    * Renders this SVG document to a Picture object using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG document that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    *
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param widthInPixels the width of the initial viewport
    * @param heightInPixels the height of the initial viewport
    * @return a Picture object suitable for later rendering using {@code Canvas.drawPicture()}, or null if the viewId was not found.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Picture  renderViewToPicture(String viewId, int widthInPixels, int heightInPixels)
   {
      SvgObject  obj = this.getElementById(viewId);
      if (obj == null)
         return null;
      if (!(obj instanceof SVG.View))
         return null;

      SVG.View  view = (SVG.View) obj;

      if (view.viewBox == null) {
         Log.w(TAG, "View element is missing a viewBox attribute.");
         return null;
      }

      Picture  picture = new Picture();
      Canvas   canvas = picture.beginRecording(widthInPixels, heightInPixels);
      Box      viewPort = new Box(0f, 0f, (float) widthInPixels, (float) heightInPixels);

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, this.renderDPI);

      renderer.renderDocument(this, viewPort, view.viewBox, view.preserveAspectRatio);

      picture.endRecording();
      return picture;
   }


   //===============================================================================
   // SVG document rendering to a canvas object (direct rendering)


   /**
    * Renders this SVG document to a Canvas object.  The full width and height of the canvas
    * will be used as the viewport into which the document will be rendered.
    *
    * @param canvas the canvas to which the document should be rendered.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderToCanvas(Canvas canvas)
   {
      renderToCanvas(canvas, null);
   }


   /**
    * Renders this SVG document to a Canvas object.
    *
    * @param canvas the canvas to which the document should be rendered.
    * @param viewPort the bounds of the area on the canvas you want the SVG rendered, or null for the whole canvas.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderToCanvas(Canvas canvas, RectF viewPort)
   {
      Box  canvasViewPort;

      if (viewPort != null) {
         canvasViewPort = Box.fromLimits(viewPort.left, viewPort.top, viewPort.right, viewPort.bottom);
      } else {
         canvasViewPort = new Box(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight());
      }

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, this.renderDPI);

      renderer.renderDocument(this, canvasViewPort, null, null);
   }


   /**
    * Renders this SVG document to a Canvas using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG documents that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    * <p>
    * If the {@code <view>} could not be found, nothing will be drawn.
    *
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param canvas the canvas to which the document should be rendered.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderViewToCanvas(String viewId, Canvas canvas)
   {
      renderViewToCanvas(viewId, canvas, null);
   }


   /**
    * Renders this SVG document to a Canvas using the specified view defined in the document.
    * <p>
    * A View is an special element in a SVG documents that describes a rectangular area in the document.
    * Calling this method with a {@code viewId} will result in the specified view being positioned and scaled
    * to the viewport.  In other words, use {@link #renderToPicture()} to render the whole document, or use this
    * method instead to render just a part of it.
    * <p>
    * If the {@code <view>} could not be found, nothing will be drawn.
    *
    * @param viewId the id of a view element in the document that defines which section of the document is to be visible.
    * @param canvas the canvas to which the document should be rendered.
    * @param viewPort the bounds of the area on the canvas you want the SVG rendered, or null for the whole canvas.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  renderViewToCanvas(String viewId, Canvas canvas, RectF viewPort)
   {
      SvgObject  obj = this.getElementById(viewId);
      if (obj == null)
         return;
      if (!(obj instanceof SVG.View))
         return;

      SVG.View  view = (SVG.View) obj;

      if (view.viewBox == null) {
         Log.w(TAG, "View element is missing a viewBox attribute.");
         return;
      }

      Box  canvasViewPort;

      if (viewPort != null) {
         canvasViewPort = Box.fromLimits(viewPort.left, viewPort.top, viewPort.right, viewPort.bottom);
      } else {
         canvasViewPort = new Box(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight());
      }

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, this.renderDPI);

      renderer.renderDocument(this, canvasViewPort, view.viewBox, view.preserveAspectRatio);
   }


   //===============================================================================
   // Other document utility API functions


   /**
    * Returns the version number of this library.
    *
    * @return the version number in string format
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public static String  getVersion()
   {
      return VERSION;
   }


   /**
    * Returns the contents of the {@code <title>} element in the SVG document.
    *
    * @return title contents if available, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentTitle()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      return title;
   }


   /**
    * Returns the contents of the {@code <desc>} element in the SVG document.
    *
    * @return desc contents if available, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentDescription()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      return desc;
   }


   /**
    * Returns the SVG version number as provided in the root {@code <svg>} tag of the document.
    *
    * @return the version string if declared, otherwise an empty string.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public String getDocumentSVGVersion()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      return rootElement.version;
   }


   /**
    * Returns a list of ids for all {@code <view>} elements in this SVG document.
    * <p>
    * The returned view ids could be used when calling and of the {@code renderViewToX()} methods.
    *
    * @return the list of id strings.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public Set<String> getViewList()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      List<SvgObject>  viewElems = getElementsByTagName(View.class);

      Set<String>  viewIds = new HashSet<>(viewElems.size());
      for (SvgObject elem: viewElems)
      {
         View  view = (View) elem;
         if (view.id != null)
            viewIds.add(view.id);
         else
            Log.w("AndroidSVG", "getViewList(): found a <view> without an id attribute");
      }
      return viewIds;
   }


   /**
    * Returns the width of the document as specified in the SVG file.
    * <p>
    * If the width in the document is specified in pixels, that value will be returned.
    * If the value is listed with a physical unit such as "cm", then the current
    * {@code RenderDPI} value will be used to convert that value to pixels. If the width
    * is missing, or in a form which can't be converted to pixels, such as "100%" for
    * example, -1 will be returned.
    *
    * @return the width in pixels, or -1 if there is no width available.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentWidth()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      return getDocumentDimensions(this.renderDPI).width;
   }


   /**
    * Change the width of the document by altering the "width" attribute
    * of the root {@code <svg>} element.
    *
    * @param pixels The new value of width in pixels.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentWidth(float pixels)
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.width = new Length(pixels);
   }


   /**
    * Change the width of the document by altering the "width" attribute
    * of the root {@code <svg>} element.
    *
    * @param value A valid SVG 'length' attribute, such as "100px" or "10cm".
    * @throws SVGParseException if {@code value} cannot be parsed successfully.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentWidth(String value) throws SVGParseException
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.width = SVGParser.parseLength(value);
   }


   /**
    * Returns the height of the document as specified in the SVG file.
    * <p>
    * If the height in the document is specified in pixels, that value will be returned.
    * If the value is listed with a physical unit such as "cm", then the current
    * {@code RenderDPI} value will be used to convert that value to pixels. If the height
    * is missing, or in a form which can't be converted to pixels, such as "100%" for
    * example, -1 will be returned.
    *
    * @return the height in pixels, or -1 if there is no height available.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentHeight()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      return getDocumentDimensions(this.renderDPI).height;
   }


   /**
    * Change the height of the document by altering the "height" attribute
    * of the root {@code <svg>} element.
    *
    * @param pixels The new value of height in pixels.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentHeight(float pixels)
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.height = new Length(pixels);
   }


   /**
    * Change the height of the document by altering the "height" attribute
    * of the root {@code <svg>} element.
    *
    * @param value A valid SVG 'length' attribute, such as "100px" or "10cm".
    * @throws SVGParseException if {@code value} cannot be parsed successfully.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentHeight(String value) throws SVGParseException
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.height = SVGParser.parseLength(value);
   }


   /**
    * Change the document view box by altering the "viewBox" attribute
    * of the root {@code <svg>} element.
    * <p>
    * The viewBox generally describes the bounding box dimensions of the
    * document contents.  A valid viewBox is necessary if you want the
    * document scaled to fit the canvas or viewport the document is to be
    * rendered into.
    * <p>
    * By setting a viewBox that describes only a portion of the document,
    * you can reproduce the effect of image sprites.
    *
    * @param minX the left coordinate of the viewBox in pixels
    * @param minY the top coordinate of the viewBox in pixels.
    * @param width the width of the viewBox in pixels
    * @param height the height of the viewBox in pixels
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentViewBox(float minX, float minY, float width, float height)
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.viewBox = new Box(minX, minY, width, height);
   }


   /**
    * Returns the viewBox attribute of the current SVG document.
    *
    * @return the document's viewBox attribute as a {@code android.graphics.RectF} object, or null if not set.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public RectF  getDocumentViewBox()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      if (this.rootElement.viewBox == null)
         return null;

      return this.rootElement.viewBox.toRectF();
   }


   /**
    * Change the document positioning by altering the "preserveAspectRatio"
    * attribute of the root {@code <svg>} element.  See the
    * documentation for {@link PreserveAspectRatio} for more information
    * on how positioning works.
    *
    * @param preserveAspectRatio the new {@code preserveAspectRatio} setting for the root {@code <svg>} element.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public void  setDocumentPreserveAspectRatio(PreserveAspectRatio preserveAspectRatio)
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      this.rootElement.preserveAspectRatio = preserveAspectRatio;
   }


   /**
    * Return the "preserveAspectRatio" attribute of the root {@code <svg>}
    * element in the form of an {@link PreserveAspectRatio} object.
    *
    * @return the preserveAspectRatio setting of the document's root {@code <svg>} element.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public PreserveAspectRatio  getDocumentPreserveAspectRatio()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      if (this.rootElement.preserveAspectRatio == null)
         return null;

      return this.rootElement.preserveAspectRatio;
   }


   /**
    * Returns the aspect ratio of the document as a width/height fraction.
    * <p>
    * If the width or height of the document are listed with a physical unit such as "cm",
    * then the current {@code renderDPI} setting will be used to convert that value to pixels.
    * <p>
    * If the width or height cannot be determined, -1 will be returned.
    *
    * @return the aspect ratio as a width/height fraction, or -1 if the ratio cannot be determined.
    * @throws IllegalArgumentException if there is no current SVG document loaded.
    */
   @SuppressWarnings({"WeakerAccess", "unused"})
   public float  getDocumentAspectRatio()
   {
      if (this.rootElement == null)
         throw new IllegalArgumentException("SVG document is empty");

      Length  w = this.rootElement.width;
      Length  h = this.rootElement.height;

      // If width and height are both specified and are not percentages, aspect ratio is calculated from these (SVG1.1 sect 7.12)
      if (w != null && h != null && w.unit!=Unit.percent && h.unit!=Unit.percent)
      {
         if (w.isZero() || h.isZero())
            return -1f;
         return w.floatValue(this.renderDPI) / h.floatValue(this.renderDPI);
      }

      // Otherwise, get the ratio from the viewBox
      if (this.rootElement.viewBox != null && this.rootElement.viewBox.width != 0f && this.rootElement.viewBox.height != 0f) {
         return this.rootElement.viewBox.width / this.rootElement.viewBox.height;
      }

      // Could not determine aspect ratio
      return -1f;
   }



   //===============================================================================


   public SVG.Svg  getRootElement()
   {
      return rootElement;
   }


   void setRootElement(SVG.Svg rootElement)
   {
      this.rootElement = rootElement;
   }


   SvgObject  resolveIRI(String iri)
   {
      if (iri == null)
         return null;

      iri = cssQuotedString(iri);
      if (iri.length() > 1 && iri.startsWith("#"))
      {
         return getElementById(iri.substring(1));
      }
      return null;
   }


   private String  cssQuotedString(String str)
   {
      if (str.startsWith("\"") && str.endsWith("\""))
      {
         // Remove quotes and replace escaped double-quote
         str = str.substring(1, str.length()-1).replace("\\\"", "\"");
      }
      else if (str.startsWith("'") && str.endsWith("'"))
      {
         // Remove quotes and replace escaped single-quote
         str = str.substring(1, str.length()-1).replace("\\'", "'");
      }
      // Remove escaped newline. Replace escape seq representing newline
      return str.replace("\\\n", "").replace("\\A", "\n");
   }


   private Box  getDocumentDimensions(float dpi)
   {
      Length  w = this.rootElement.width;
      Length  h = this.rootElement.height;

      if (w == null || w.isZero() || w.unit==Unit.percent || w.unit==Unit.em || w.unit==Unit.ex)
         return new Box(-1,-1,-1,-1);

      float  wOut = w.floatValue(dpi);
      float  hOut;

      if (h != null) {
         if (h.isZero() || h.unit==Unit.percent || h.unit==Unit.em || h.unit==Unit.ex) {
            return new Box(-1,-1,-1,-1);
         }
         hOut = h.floatValue(dpi);
      } else {
         // height is not specified. SVG spec says this is okay. If there is a viewBox, we use
         // that to calculate the height. Otherwise we set height equal to width.
         if (this.rootElement.viewBox != null) {
            hOut = (wOut * this.rootElement.viewBox.height) / this.rootElement.viewBox.width;
         } else {
            hOut = wOut;
         }
      }
      return new Box(0,0, wOut,hOut);
   }


   //===============================================================================
   // CSS support methods


   void  addCSSRules(Ruleset ruleset)
   {
      this.cssRules.addAll(ruleset);
   }


   List<CSSParser.Rule>  getCSSRules()
   {
      return this.cssRules.getRules();
   }


   boolean  hasCSSRules()
   {
      return !this.cssRules.isEmpty();
   }


   //===============================================================================
   // Object sub-types used in the SVG object tree


   static class  Box implements Cloneable
   {
      float  minX, minY, width, height;

      Box(float minX, float minY, float width, float height)
      {
         this.minX = minX;
         this.minY = minY;
         this.width = width;
         this.height = height;
      }

      Box(Box copy)
      {
         this.minX = copy.minX;
         this.minY = copy.minY;
         this.width = copy.width;
         this.height = copy.height;
      }

      static Box  fromLimits(float minX, float minY, float maxX, float maxY)
      {
         return new Box(minX, minY, maxX-minX, maxY-minY);
      }

      RectF  toRectF()
      {
         return new RectF(minX, minY, maxX(), maxY());
      }

      float  maxX() { return minX + width; }
      float  maxY() { return minY + height; }

      void  union(Box other)
      {
         if (other.minX < minX) minX = other.minX;
         if (other.minY < minY) minY = other.minY;
         if (other.maxX() > maxX()) width = other.maxX() - minX;
         if (other.maxY() > maxY()) height = other.maxY() - minY;
      }

      public String toString() { return "["+minX+" "+minY+" "+width+" "+height+"]"; }
   }


   static final long SPECIFIED_FILL                  = 1;
   static final long SPECIFIED_FILL_RULE             = (1<<1);
   static final long SPECIFIED_FILL_OPACITY          = (1<<2);
   static final long SPECIFIED_STROKE                = (1<<3);
   static final long SPECIFIED_STROKE_OPACITY        = (1<<4);
   static final long SPECIFIED_STROKE_WIDTH          = (1<<5);
   static final long SPECIFIED_STROKE_LINECAP        = (1<<6);
   static final long SPECIFIED_STROKE_LINEJOIN       = (1<<7);
   static final long SPECIFIED_STROKE_MITERLIMIT     = (1<<8);
   static final long SPECIFIED_STROKE_DASHARRAY      = (1<<9);
   static final long SPECIFIED_STROKE_DASHOFFSET     = (1<<10);
   static final long SPECIFIED_OPACITY               = (1<<11);
   static final long SPECIFIED_COLOR                 = (1<<12);
   static final long SPECIFIED_FONT_FAMILY           = (1<<13);
   static final long SPECIFIED_FONT_SIZE             = (1<<14);
   static final long SPECIFIED_FONT_WEIGHT           = (1<<15);
   static final long SPECIFIED_FONT_STYLE            = (1<<16);
   static final long SPECIFIED_TEXT_DECORATION       = (1<<17);
   static final long SPECIFIED_TEXT_ANCHOR           = (1<<18);
   static final long SPECIFIED_OVERFLOW              = (1<<19);
   static final long SPECIFIED_CLIP                  = (1<<20);
   static final long SPECIFIED_MARKER_START          = (1<<21);
   static final long SPECIFIED_MARKER_MID            = (1<<22);
   static final long SPECIFIED_MARKER_END            = (1<<23);
   static final long SPECIFIED_DISPLAY               = (1<<24);
   static final long SPECIFIED_VISIBILITY            = (1<<25);
   static final long SPECIFIED_STOP_COLOR            = (1<<26);
   static final long SPECIFIED_STOP_OPACITY          = (1<<27);
   static final long SPECIFIED_CLIP_PATH             = (1<<28);
   static final long SPECIFIED_CLIP_RULE             = (1<<29);
   static final long SPECIFIED_MASK                  = (1<<30);
   static final long SPECIFIED_SOLID_COLOR           = (1L<<31);
   static final long SPECIFIED_SOLID_OPACITY         = (1L<<32);
   static final long SPECIFIED_VIEWPORT_FILL         = (1L<<33);
   static final long SPECIFIED_VIEWPORT_FILL_OPACITY = (1L<<34);
   static final long SPECIFIED_VECTOR_EFFECT         = (1L<<35);
   static final long SPECIFIED_DIRECTION             = (1L<<36);
   static final long SPECIFIED_IMAGE_RENDERING       = (1L<<37);

   private static final long SPECIFIED_ALL = 0xffffffff;

   /*
   protected static final long SPECIFIED_NON_INHERITING = SPECIFIED_DISPLAY | SPECIFIED_OVERFLOW | SPECIFIED_CLIP
                                                          | SPECIFIED_CLIP_PATH | SPECIFIED_OPACITY | SPECIFIED_STOP_COLOR
                                                          | SPECIFIED_STOP_OPACITY | SPECIFIED_MASK | SPECIFIED_SOLID_COLOR
                                                          | SPECIFIED_SOLID_OPACITY | SPECIFIED_VIEWPORT_FILL
                                                          | SPECIFIED_VIEWPORT_FILL_OPACITY | SPECIFIED_VECTOR_EFFECT;
   */

   protected static class  Style implements Cloneable
   {
      // Which properties have been explicitly specified by this element
      long       specifiedFlags = 0;

      SvgPaint   fill;
      FillRule   fillRule;
      Float      fillOpacity;

      SvgPaint   stroke;
      Float      strokeOpacity;
      Length     strokeWidth;
      LineCap    strokeLineCap;
      LineJoin   strokeLineJoin;
      Float      strokeMiterLimit;
      Length[]   strokeDashArray;
      Length     strokeDashOffset;

      Float      opacity; // master opacity of both stroke and fill

      Colour     color;

      List<String>    fontFamily;
      Length          fontSize;
      Integer         fontWeight;
      FontStyle       fontStyle;
      TextDecoration  textDecoration;
      TextDirection   direction;

      TextAnchor   textAnchor;

      Boolean      overflow;  // true if overflow visible
      CSSClipRect  clip;

      String     markerStart;
      String     markerMid;
      String     markerEnd;

      Boolean    display;    // true if we should display
      Boolean    visibility; // true if visible

      SvgPaint   stopColor;
      Float      stopOpacity;

      String     clipPath;
      FillRule   clipRule;

      String     mask;

      SvgPaint   solidColor;
      Float      solidOpacity;

      SvgPaint   viewportFill;
      Float      viewportFillOpacity;

      VectorEffect  vectorEffect;

      RenderQuality  imageRendering;


      static final int  FONT_WEIGHT_NORMAL = 400;
      static final int  FONT_WEIGHT_BOLD = 700;
      static final int  FONT_WEIGHT_LIGHTER = -1;
      static final int  FONT_WEIGHT_BOLDER = +1;


      public enum FillRule
      {
         NonZero,
         EvenOdd
      }

      public enum LineCap
      {
         Butt,
         Round,
         Square
      }

      public enum LineJoin
      {
         Miter,
         Round,
         Bevel
      }

      public enum FontStyle
      {
         Normal,
         Italic,
         Oblique
      }

      public enum TextAnchor
      {
         Start,
         Middle,
         End
      }

      public enum TextDecoration
      {
         None,
         Underline,
         Overline,
         LineThrough,
         Blink
      }

      public enum TextDirection
      {
         LTR,
         RTL
      }

      public enum VectorEffect
      {
         None,
         NonScalingStroke
      }

      public enum RenderQuality
      {
         auto,
         optimizeQuality,
         optimizeSpeed
      }

      static Style  getDefaultStyle()
      {
         Style  def = new Style();
         def.specifiedFlags = SPECIFIED_ALL;
         //def.inheritFlags = 0;
         def.fill = Colour.BLACK;
         def.fillRule = FillRule.NonZero;
         def.fillOpacity = 1f;
         def.stroke = null;         // none
         def.strokeOpacity = 1f;
         def.strokeWidth = new Length(1f);
         def.strokeLineCap = LineCap.Butt;
         def.strokeLineJoin = LineJoin.Miter;
         def.strokeMiterLimit = 4f;
         def.strokeDashArray = null;
         def.strokeDashOffset = new Length(0f);
         def.opacity = 1f;
         def.color = Colour.BLACK; // currentColor defaults to black
         def.fontFamily = null;
         def.fontSize = new Length(12, Unit.pt);
         def.fontWeight = FONT_WEIGHT_NORMAL;
         def.fontStyle = FontStyle.Normal;
         def.textDecoration = TextDecoration.None;
         def.direction = TextDirection.LTR;
         def.textAnchor = TextAnchor.Start;
         def.overflow = true;  // Overflow shown/visible for root, but not for other elements (see section 14.3.3).
         def.clip = null;
         def.markerStart = null;
         def.markerMid = null;
         def.markerEnd = null;
         def.display = Boolean.TRUE;
         def.visibility = Boolean.TRUE;
         def.stopColor = Colour.BLACK;
         def.stopOpacity = 1f;
         def.clipPath = null;
         def.clipRule = FillRule.NonZero;
         def.mask = null;
         def.solidColor = null;
         def.solidOpacity = 1f;
         def.viewportFill = null;
         def.viewportFillOpacity = 1f;
         def.vectorEffect = VectorEffect.None;
         def.imageRendering = RenderQuality.auto;
         return def;
      }


      // Called on the state.style object to reset the properties that don't inherit
      // from the parent style.
      void  resetNonInheritingProperties(boolean isRootSVG)
      {
         this.display = Boolean.TRUE;
         this.overflow = isRootSVG ? Boolean.TRUE : Boolean.FALSE;
         this.clip = null;
         this.clipPath = null;
         this.opacity = 1f;
         this.stopColor = Colour.BLACK;
         this.stopOpacity = 1f;
         this.mask = null;
         this.solidColor = null;
         this.solidOpacity = 1f;
         this.viewportFill = null;
         this.viewportFillOpacity = 1f;
         this.vectorEffect = VectorEffect.None;
      }


      @Override
      protected Object  clone() throws CloneNotSupportedException
      {
         Style obj = (Style) super.clone();
         if (strokeDashArray != null) {
            obj.strokeDashArray = strokeDashArray.clone();
         }
         return obj;
      }
   }


   // What fill or stroke is
   abstract static class SvgPaint implements Cloneable
   {
   }


   static class Colour extends SvgPaint
   {
      int colour;

      static final Colour BLACK = new Colour(0xff000000);  // Black singleton - a common default value.
      static final Colour TRANSPARENT = new Colour(0);     // Transparent black

      Colour(int val)
      {
         this.colour = val;
      }

      public String toString()
      {
         return String.format("#%08x", colour);
      }
   }


   // Special version of Colour that indicates use of 'currentColor' keyword
   static class CurrentColor extends SvgPaint
   {
      private static CurrentColor  instance = new CurrentColor();

      private CurrentColor()
      {
      }

      static CurrentColor  getInstance()
      {
         return instance;
      }
   }


   static class PaintReference extends SvgPaint
   {
      String    href;
      SvgPaint  fallback;

      PaintReference(String href, SvgPaint fallback)
      {
         this.href = href;
         this.fallback = fallback;
      }

      public String toString()
      {
         return href + " " + fallback;
      }
   }


   public static class Length implements Cloneable
   {
      float  value = 0;
      Unit   unit = Unit.px;

      Length(float value, Unit unit)
      {
         this.value = value;
         this.unit = unit;
      }

      Length(float value)
      {
         this.value = value;
         this.unit = Unit.px;
      }

      public float floatValue()
      {
         return value;
      }

      // Convert length to user units for a horizontally-related context.
      float floatValueX(SVGAndroidRenderer renderer)
      {
         switch (unit)
         {
            case px:
               return value;
            case em:
               return value * renderer.getCurrentFontSize();
            case ex:
               return value * renderer.getCurrentFontXHeight();
            case in:
               return value * renderer.getDPI();
            case cm:
               return value * renderer.getDPI() / 2.54f;
            case mm:
               return value * renderer.getDPI() / 25.4f;
            case pt: // 1 point = 1/72 in
               return value * renderer.getDPI() / 72f;
            case pc: // 1 pica = 1/6 in
               return value * renderer.getDPI() / 6f;
            case percent:
               Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
               if (viewPortUser == null)
                  return value;  // Undefined in this situation - so just return value to avoid an NPE
               return value * viewPortUser.width / 100f;
            default:
               return value;
         }
      }

      // Convert length to user units for a vertically-related context.
      float floatValueY(SVGAndroidRenderer renderer)
      {
         if (unit == Unit.percent) {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
               return value;  // Undefined in this situation - so just return value to avoid an NPE
            return value * viewPortUser.height / 100f;
         }
         return floatValueX(renderer);
      }

      // Convert length to user units for a context that is not orientation specific.
      // For example, stroke width.
      float floatValue(SVGAndroidRenderer renderer)
      {
         if (unit == Unit.percent)
         {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
               return value;  // Undefined in this situation - so just return value to avoid an NPE
            float w = viewPortUser.width;
            float h = viewPortUser.height;
            if (w == h)
               return value * w / 100f;
            float n = (float) (Math.sqrt(w*w+h*h) / SQRT2);  // see spec section 7.10
            return value * n / 100f;
         }
         return floatValueX(renderer);
      }

      // Convert length to user units for a context that is not orientation specific.
      // For percentage values, use the given 'max' parameter to represent the 100% value.
      float floatValue(SVGAndroidRenderer renderer, float max)
      {
         if (unit == Unit.percent)
         {
            return value * max / 100f;
         }
         return floatValueX(renderer);
      }

      // For situations (like calculating the initial viewport) when we can only rely on
      // physical real world units.
      float floatValue(float dpi)
      {
         switch (unit)
         {
            case px:
               return value;
            case in:
               return value * dpi;
            case cm:
               return value * dpi / 2.54f;
            case mm:
               return value * dpi / 25.4f;
            case pt: // 1 point = 1/72 in
               return value * dpi / 72f;
            case pc: // 1 pica = 1/6 in
               return value * dpi / 6f;
            case em:
            case ex:
            case percent:
            default:
               return value;
         }
      }

      boolean isZero()
      {
         return value == 0f;
      }

      boolean isNegative()
      {
         return value < 0f;
      }

      public int add (Length otherLength) {
         return (int) (value + otherLength.value);
      }

      @Override
      public String toString()
      {
         return String.valueOf(value) + unit;
      }
   }


   static class CSSClipRect
   {
      Length  top;
      Length  right;
      Length  bottom;
      Length  left;

      CSSClipRect(Length top, Length right, Length bottom, Length left)
      {
         this.top = top;
         this.right = right;
         this.bottom = bottom;
         this.left = left;
      }
   }


   //===============================================================================
   // The objects in the SVG object tree
   //===============================================================================


   // Any object that can be part of the tree
   public static class SvgObject
   {
      SVG           document;
      SvgContainer  parent;

      public String  toString()
      {
         return this.getClass().getSimpleName();
         //return super.toString();
      }
   }


   // Any object in the tree that corresponds to an SVG element
   static abstract class SvgElementBase extends SvgObject
   {
      String        id = null;
      Boolean       spacePreserve = null;
      Style         baseStyle = null;   // style defined by explicit style attributes in the element (eg. fill="black")
      Style         style = null;       // style expressed in a 'style' attribute (eg. style="fill:black")
      List<String>  classNames = null;  // contents of the 'class' attribute

      abstract String  getNodeName();

      public String getId() {
         return id;
      }
   }


   // Any object in the tree that corresponds to an SVG element
   static abstract class SvgElement extends SvgElementBase
   {
      Box     boundingBox = null;
   }


   // Any element that can appear inside a <switch> element.
   interface SvgConditional
   {
      void         setRequiredFeatures(Set<String> features);
      Set<String>  getRequiredFeatures();
      void         setRequiredExtensions(String extensions);
      String       getRequiredExtensions();
      void         setSystemLanguage(Set<String> languages);
      Set<String>  getSystemLanguage();
      void         setRequiredFormats(Set<String> mimeTypes);
      Set<String>  getRequiredFormats();
      void         setRequiredFonts(Set<String> fontNames);
      Set<String>  getRequiredFonts();
   }


   // Any element that can appear inside a <switch> element.
   static abstract class  SvgConditionalElement extends SvgElement implements SvgConditional
   {
      Set<String>  requiredFeatures = null;
      String       requiredExtensions = null;
      Set<String>  systemLanguage = null;
      Set<String>  requiredFormats = null;
      Set<String>  requiredFonts = null;

      @Override
      public void setRequiredFeatures(Set<String> features) { this.requiredFeatures = features; }
      @Override
      public Set<String> getRequiredFeatures() { return this.requiredFeatures; }
      @Override
      public void setRequiredExtensions(String extensions) { this.requiredExtensions = extensions; }
      @Override
      public String getRequiredExtensions() { return this.requiredExtensions; }
      @Override
      public void setSystemLanguage(Set<String> languages) { this.systemLanguage = languages; }
      @Override
      public Set<String> getSystemLanguage() { return this.systemLanguage; }
      @Override
      public void setRequiredFormats(Set<String> mimeTypes) { this.requiredFormats = mimeTypes; }
      @Override
      public Set<String> getRequiredFormats() { return this.requiredFormats; }
      @Override
      public void setRequiredFonts(Set<String> fontNames) { this.requiredFonts = fontNames; }
      @Override
      public Set<String> getRequiredFonts() { return this.requiredFonts; }
   }


   interface SvgContainer
   {
      List<SvgObject>  getChildren();
      void             addChild(SvgObject elem) throws SVGParseException;
   }


   static abstract class SvgConditionalContainer extends SvgElement implements SvgContainer, SvgConditional
   {
      List<SvgObject> children = new ArrayList<>();

      Set<String>  requiredFeatures = null;
      String       requiredExtensions = null;
      Set<String>  systemLanguage = null;
      Set<String>  requiredFormats = null;
      Set<String>  requiredFonts = null;

      @Override
      public List<SvgObject>  getChildren() { return children; }
      @Override
      public void addChild(SvgObject elem) throws SVGParseException  { children.add(elem); }

      @Override
      public void setRequiredFeatures(Set<String> features) { this.requiredFeatures = features; }
      @Override
      public Set<String> getRequiredFeatures() { return this.requiredFeatures; }
      @Override
      public void setRequiredExtensions(String extensions) { this.requiredExtensions = extensions; }
      @Override
      public String getRequiredExtensions() { return this.requiredExtensions; }
      @Override
      public void setSystemLanguage(Set<String> languages) { this.systemLanguage = languages; }
      @Override
      public Set<String> getSystemLanguage() { return null; }
      @Override
      public void setRequiredFormats(Set<String> mimeTypes) { this.requiredFormats = mimeTypes; }
      @Override
      public Set<String> getRequiredFormats() { return this.requiredFormats; }
      @Override
      public void setRequiredFonts(Set<String> fontNames) { this.requiredFonts = fontNames; }
      @Override
      public Set<String> getRequiredFonts() { return this.requiredFonts; }
   }


   interface HasTransform
   {
      void setTransform(Matrix matrix);
   }


   static abstract class SvgPreserveAspectRatioContainer extends SvgConditionalContainer
   {
      PreserveAspectRatio  preserveAspectRatio = null;
   }


   static abstract class SvgViewBoxContainer extends SvgPreserveAspectRatioContainer
   {
      Box  viewBox;
   }


   public static class Svg extends SvgViewBoxContainer
   {
      Length  x;
      Length  y;
      Length  width;
      Length  height;
      public String  version;

      @Override
      String  getNodeName() { return "svg"; }
   }


   // An SVG element that can contain other elements.
   public static class Group extends SvgConditionalContainer implements HasTransform
   {
      Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }

      @Override
      String  getNodeName() { return "group"; }
   }


   interface NotDirectlyRendered
   {
   }


   // A <defs> object contains objects that are not rendered directly, but are instead
   // referenced from other parts of the file.
   static class Defs extends Group implements NotDirectlyRendered
   {
      @Override
      String  getNodeName() { return "defs"; }
   }


   // One of the element types that can cause graphics to be drawn onto the target canvas.
   // Specifically: 'circle', 'ellipse', 'image', 'line', 'path', 'polygon', 'polyline', 'rect', 'text' and 'use'.
   static abstract class GraphicsElement extends SvgConditionalElement implements HasTransform
   {
      Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }

      public abstract boolean contains(Point point);

      public RectF initRectF(float[] xyArray) {
          RectF rectF = new RectF();
          android.graphics.Path path = new android.graphics.Path();

          for (int i = 0; i < xyArray.length; i++) {
              int xPoint = (int) xyArray[i];
              i++;
              int yPoint = (int) xyArray[i];

              if(i == 1) {
                  path.moveTo(xPoint, yPoint);
              } else {
                  path.lineTo(xPoint, yPoint);
              }
          }
          path.computeBounds(rectF, false);
          return rectF;
      }
   }


   protected static class Use extends Group
   {
      String  href;
      Length  x;
      Length  y;
      Length  width;
      Length  height;
   }


   static class Path extends GraphicsElement
   {
      PathDefinition  d;
      Float           pathLength;

      @Override
      String  getNodeName() { return "path"; }

       @Override
       public boolean contains(Point point) {
           return false;
       }
   }


   public static class Rect extends GraphicsElement
   {
      Length  x;
      Length  y;
      Length  width;
      Length  height;
      Length  rx;
      Length  ry;

      private RectF rectF;

      @Override
      String  getNodeName() { return "rect"; }

      public Length getX() {
         return x;
      }

      public Length getY() {
         return y;
      }

      public Length getWidth() {
         return width;
      }

      public Length getHeight() {
         return height;
      }

       @Override
       public boolean contains(Point point) {
           if (rectF == null) {
               float[] points = new float[] {
                       getX().floatValue(),
                       getY().floatValue(),
                       getX().add(getWidth()),
                       getY().floatValue(),
                       getX().add(getWidth()),
                       getY().add(getHeight()),
                       getX().floatValue(),
                       getY().add(getHeight())
               };
               rectF = initRectF(points);
           }
           return rectF.contains(point.x, point.y);
       }
   }


   static class Circle extends GraphicsElement
   {
      Length  cx;
      Length  cy;
      Length  r;

      @Override
      String  getNodeName() { return "circle"; }

       @Override
       public boolean contains(Point point) {
           return false;
       }
   }


   static class Ellipse extends GraphicsElement
   {
      Length  cx;
      Length  cy;
      Length  rx;
      Length  ry;

      @Override
      String  getNodeName() { return "ellipse"; }

       @Override
       public boolean contains(Point point) {
           return false;
       }
   }


   static class Line extends GraphicsElement
   {
      Length  x1;
      Length  y1;
      Length  x2;
      Length  y2;

      @Override
      String  getNodeName() { return "line"; }

       @Override
       public boolean contains(Point point) {
           return false;
       }
   }


   static class PolyLine extends GraphicsElement
   {
      float[]  points;

      @Override
      String  getNodeName() { return "polyline"; }

      public float[] getPoints() {
         return points;
      }

       @Override
       public boolean contains(Point point) {
           return false;
       }
   }


   public static class Polygon extends PolyLine
   {
       private RectF rectF;

      @Override
      String  getNodeName() { return "polygon"; }

       @Override
       public boolean contains(Point point) {
           if (rectF == null) {
               rectF = initRectF(getPoints());
           }
           return rectF.contains(point.x, point.y);
       }
   }


   // A root text container such as <text> or <textPath>
   interface  TextRoot
   {
   }


   interface  TextChild
   {
      void      setTextRoot(TextRoot obj);
      TextRoot  getTextRoot();
   }


   static abstract class  TextContainer extends SvgConditionalContainer
   {
      @Override
      public void  addChild(SvgObject elem) throws SVGParseException
      {
         if (elem instanceof TextChild)
            children.add(elem);
         else
            throw new SVGParseException("Text content elements cannot contain "+elem+" elements.");
      }
   }


   static abstract class  TextPositionedContainer extends TextContainer
   {
      List<Length>  x;
      List<Length>  y;
      List<Length>  dx;
      List<Length>  dy;
   }


   protected static class Text extends TextPositionedContainer implements TextRoot, HasTransform
   {
      Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
      @Override
      String  getNodeName() { return "text"; }
   }


   static class TSpan extends TextPositionedContainer implements TextChild
   {
      private TextRoot  textRoot;

      @Override
      public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
      @Override
      public TextRoot  getTextRoot() { return this.textRoot; }
      @Override
      String  getNodeName() { return "tspan"; }
   }


   static class TextSequence extends SvgObject implements TextChild
   {
      String  text;

      private TextRoot   textRoot;

      TextSequence(String text)
      {
         this.text = text;
      }

      public String  toString()
      {
         return this.getClass().getSimpleName() + " '"+text+"'";
      }

      @Override
      public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
      @Override
      public TextRoot  getTextRoot() { return this.textRoot; }
   }


   static class TRef extends TextContainer implements TextChild
   {
      String  href;

      private TextRoot   textRoot;

      @Override
      public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
      @Override
      public TextRoot  getTextRoot() { return this.textRoot; }
      @Override
      String  getNodeName() { return "tref"; }
   }


   static class TextPath extends TextContainer implements TextChild
   {
      String  href;
      Length  startOffset;

      private TextRoot  textRoot;

      @Override
      public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
      @Override
      public TextRoot  getTextRoot() { return this.textRoot; }
      @Override
      String  getNodeName() { return "textPath"; }
   }


   // An SVG element that can contain other elements.
   static class Switch extends Group
   {
   }


   static class Symbol extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      @Override
      String  getNodeName() { return "symbol"; }
   }


   static class Marker extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      boolean  markerUnitsAreUser;
      Length   refX;
      Length   refY;
      Length   markerWidth;
      Length   markerHeight;
      Float    orient;

      @Override
      String  getNodeName() { return "marker"; }
   }


   static abstract class GradientElement extends SvgElementBase implements SvgContainer
   {
      List<SvgObject> children = new ArrayList<>();

      Boolean         gradientUnitsAreUser;
      Matrix          gradientTransform;
      GradientSpread  spreadMethod;
      String          href;

      @Override
      public List<SvgObject> getChildren()
      {
         return children;
      }

      @Override
      public void addChild(SvgObject elem) throws SVGParseException
      {
         if (elem instanceof Stop)
            children.add(elem);
         else
            throw new SVGParseException("Gradient elements cannot contain "+elem+" elements.");
      }
   }


   static class Stop extends SvgElementBase implements SvgContainer
   {
      Float  offset;

      // Dummy container methods. Stop is officially a container, but we
      // are not interested in any of its possible child elements.
      @Override
      public List<SvgObject> getChildren() { return Collections.emptyList(); }
      @Override
      public void addChild(SvgObject elem) throws SVGParseException { /* do nothing */ }
      @Override
      String  getNodeName() { return "stop"; }
   }


   static class SvgLinearGradient extends GradientElement
   {
      Length  x1;
      Length  y1;
      Length  x2;
      Length  y2;

      @Override
      String  getNodeName() { return "linearGradient"; }
   }


   static class SvgRadialGradient extends GradientElement
   {
      Length  cx;
      Length  cy;
      Length  r;
      Length  fx;
      Length  fy;

      @Override
      String  getNodeName() { return "radialGradient"; }
   }


   static class ClipPath extends Group implements NotDirectlyRendered
   {
      Boolean  clipPathUnitsAreUser;

      @Override
      String  getNodeName() { return "clipPath"; }
   }


   static class Pattern extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      Boolean  patternUnitsAreUser;
      Boolean  patternContentUnitsAreUser;
      Matrix   patternTransform;
      Length   x;
      Length   y;
      Length   width;
      Length   height;
      String   href;

      @Override
      String  getNodeName() { return "pattern"; }
   }


   protected static class Image extends SvgPreserveAspectRatioContainer implements HasTransform
   {
      String  href;
      Length  x;
      Length  y;
      Length  width;
      Length  height;
      Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
      @Override
      String  getNodeName() { return "image"; }
   }


   protected static class View extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      @Override
      String  getNodeName() { return "view"; }
   }


   static class Mask extends SvgConditionalContainer implements NotDirectlyRendered
   {
      Boolean  maskUnitsAreUser;
      Boolean  maskContentUnitsAreUser;
      Length   x;
      Length   y;
      Length   width;
      Length   height;

      @Override
      String  getNodeName() { return "mask"; }
   }


   static class SolidColor extends SvgElementBase implements SvgContainer
   {
      // Not needed right now. Colour is set in this.baseStyle.
      //public Length  solidColor;
      //public Length  solidOpacity;

      // Dummy container methods. Stop is officially a container, but we
      // are not interested in any of its possible child elements.
      @Override
      public List<SvgObject> getChildren() { return Collections.emptyList(); }
      @Override
      public void addChild(SvgObject elem) throws SVGParseException { /* do nothing */ }
      @Override
      String  getNodeName() { return "solidColor"; }
   }


   //===============================================================================
   // Protected setters for internal use


   void setTitle(String title)
   {
      this.title = title;
   }


   void setDesc(String desc)
   {
      this.desc = desc;
   }


   static SVGExternalFileResolver  getFileResolver()
   {
      return externalFileResolver;
   }


   //===============================================================================
   // Path definition


   interface PathInterface
   {
      void  moveTo(float x, float y);
      void  lineTo(float x, float y);
      void  cubicTo(float x1, float y1, float x2, float y2, float x3, float y3);
      void  quadTo(float x1, float y1, float x2, float y2);
      void  arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y);
      void  close();
   }


   static class PathDefinition implements PathInterface
   {
      private byte[]   commands = null;
      private int      commandsLength = 0;
      private float[]  coords = null;
      private int      coordsLength = 0;

      private static final byte  MOVETO  = 0;
      private static final byte  LINETO  = 1;
      private static final byte  CUBICTO = 2;
      private static final byte  QUADTO  = 3;
      private static final byte  ARCTO   = 4;   // 4-7
      private static final byte  CLOSE   = 8;


      PathDefinition()
      {
         this.commands = new byte[8];
         this.coords = new float[16];
      }


      boolean  isEmpty()
      {
         return commandsLength == 0;
      }


      private void  addCommand(byte value)
      {
         if (commandsLength == commands.length) {
            byte[]  newCommands = new byte[commands.length * 2];
            System.arraycopy(commands, 0, newCommands, 0, commands.length);
            commands = newCommands;
         }
         commands[commandsLength++] = value;
      }


      private void  coordsEnsure(int num)
      {
         if (coords.length < (coordsLength + num)) {
            float[]  newCoords = new float[coords.length * 2];
            System.arraycopy(coords, 0, newCoords, 0, coords.length);
            coords = newCoords;
         }
      }


      @Override
      public void  moveTo(float x, float y)
      {
         addCommand(MOVETO);
         coordsEnsure(2);
         coords[coordsLength++] = x;
         coords[coordsLength++] = y;
      }


      @Override
      public void  lineTo(float x, float y)
      {
         addCommand(LINETO);
         coordsEnsure(2);
         coords[coordsLength++] = x;
         coords[coordsLength++] = y;
      }


      @Override
      public void  cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         addCommand(CUBICTO);
         coordsEnsure(6);
         coords[coordsLength++] = x1;
         coords[coordsLength++] = y1;
         coords[coordsLength++] = x2;
         coords[coordsLength++] = y2;
         coords[coordsLength++] = x3;
         coords[coordsLength++] = y3;
      }


      @Override
      public void  quadTo(float x1, float y1, float x2, float y2)
      {
         addCommand(QUADTO);
         coordsEnsure(4);
         coords[coordsLength++] = x1;
         coords[coordsLength++] = y1;
         coords[coordsLength++] = x2;
         coords[coordsLength++] = y2;
      }


      @Override
      public void  arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y)
      {
         int  arc = ARCTO | (largeArcFlag?2:0) | (sweepFlag?1:0);
         addCommand((byte) arc);
         coordsEnsure(5);
         coords[coordsLength++] = rx;
         coords[coordsLength++] = ry;
         coords[coordsLength++] = xAxisRotation;
         coords[coordsLength++] = x;
         coords[coordsLength++] = y;
      }


      @Override
      public void  close()
      {
         addCommand(CLOSE);
      }


      void enumeratePath(PathInterface handler)
      {
         int  coordsPos = 0;

         for (int commandPos = 0; commandPos < commandsLength; commandPos++)
         {
            byte  command = commands[commandPos];
            switch (command)
            {
               case MOVETO:
                  handler.moveTo(coords[coordsPos++], coords[coordsPos++]);
                  break;
               case LINETO:
                  handler.lineTo(coords[coordsPos++], coords[coordsPos++]);
                  break;
               case CUBICTO:
                  handler.cubicTo(coords[coordsPos++], coords[coordsPos++], coords[coordsPos++], coords[coordsPos++],coords[coordsPos++], coords[coordsPos++]);
                  break;
               case QUADTO:
                  handler.quadTo(coords[coordsPos++], coords[coordsPos++], coords[coordsPos++], coords[coordsPos++]);
                  break;
               case CLOSE:
                  handler.close();
                  break;
               default:
                  boolean  largeArcFlag = (command & 2) != 0;
                  boolean  sweepFlag = (command & 1) != 0;
                  handler.arcTo(coords[coordsPos++], coords[coordsPos++], coords[coordsPos++], largeArcFlag, sweepFlag, coords[coordsPos++], coords[coordsPos++]);
            }
         }
      }

   }


   private SvgObject  getElementById(String id)
   {
      if (id == null || id.length() == 0)
         return null;
      if (id.equals(rootElement.id))
         return rootElement;

      if (idToElementMap.containsKey(id))
         return idToElementMap.get(id);

      // Search the object tree for a node with id property that matches 'id'
      SvgElementBase  result = getElementById(rootElement, id);
      idToElementMap.put(id, result);
      return result;
   }


   private SvgElementBase  getElementById(SvgContainer obj, String id)
   {
      SvgElementBase  elem = (SvgElementBase) obj;
      if (id.equals(elem.id))
         return elem;
      for (SvgObject child: obj.getChildren())
      {
         if (!(child instanceof SvgElementBase))
            continue;
         SvgElementBase  childElem = (SvgElementBase) child;
         if (id.equals(childElem.id))
            return childElem;
         if (child instanceof SvgContainer)
         {
            SvgElementBase  found = getElementById((SvgContainer) child, id);
            if (found != null)
               return found;
         }
      }
      return null;
   }


   @SuppressWarnings("rawtypes")
   private List<SvgObject>  getElementsByTagName(Class clazz)
   {
      // Search the object tree for nodes with the give element class
      return getElementsByTagName(rootElement, clazz);
   }


   @SuppressWarnings("rawtypes")
   private List<SvgObject>  getElementsByTagName(SvgContainer obj, Class clazz)
   {
      List<SvgObject>  result = new ArrayList<>();

      if (obj.getClass() == clazz)
         result.add((SvgObject) obj);
      for (SvgObject child: obj.getChildren())
      {
         if (child.getClass() == clazz)
            result.add(child);
         if (child instanceof SvgContainer)
            getElementsByTagName((SvgContainer) child, clazz);
      }
      return result;
   }


}
