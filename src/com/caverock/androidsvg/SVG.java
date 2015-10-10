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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.RectF;
import android.util.Log;

import com.caverock.androidsvg.CSSParser.Ruleset;

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
 * SVG  svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
 * svg.registerExternalFileResolver(myResolver);
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

public class SVG implements Serializable
{
    private static final long serialVersionUID = 12202L; //

    private static final String  TAG = "AndroidSVG";

    private static final String  VERSION = "1.2.2-beta-2";

    protected static final String  SUPPORTED_SVG_VERSION = "1.2";

    private static final int     DEFAULT_PICTURE_WIDTH = 512;
    private static final int     DEFAULT_PICTURE_HEIGHT = 512;

    private static final double  SQRT2 = 1.414213562373095;

    private Svg     rootElement = null;

    // Metadata
    private String  title = "";
    private String  desc = "";

    // Resolver
    private SVGExternalFileResolver  fileResolver = null;

    // DPI to use for rendering
    private float   renderDPI = 96f;   // default is 96

    // CSS rules
    transient private Ruleset  cssRules = new Ruleset();

    // Map from id attribute to element
    transient private Map<String, SvgElementBase> idToElementMap = new HashMap<String, SvgElementBase>();
    transient private Map<String, List<SvgElementBase>> classToElementMap = new HashMap<String, List<SvgElementBase>>();

    // Push and Pop stack
    transient private Stack<InputStream> pushPopStack = new Stack<InputStream>();
    transient private ByteArrayOutputStream originalStream; // InputStream can only be read once

    protected enum Unit
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


    protected enum GradientSpread
    {
        pad,
        reflect,
        repeat
    }


    protected SVG()
    {
    }

    protected Object readResolve() throws ObjectStreamException {
        return this;
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        // write 'this' to 'out'...

        out.defaultWriteObject();

    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // populate the fields of 'this' from the data in 'in'...
        in.defaultReadObject();
        rootElement.document = this;
        cssRules = new Ruleset();


    }

    private ByteArrayOutputStream convertToPushPopOutputStream() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(this.rootElement);
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream;
    }

    /**
     * Restore the original SVG state.
     */
    public void restoreOriginalSVGState() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(originalStream.toByteArray()));
            this.rootElement = (Svg) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        classToElementMap.clear();
        idToElementMap.clear();
    }

    /**
     * Push the current SVG state to stack. Should be in pair with {@code this.restoreSVGState()}.
     */
    public void saveSVGState() {

        pushPopStack.push(new ByteArrayInputStream(convertToPushPopOutputStream().toByteArray()));
    }

    /**
     * Pop the last saved SVG state from stack. Should be in pair with {@code this.saveSVGState()}.
     */
    public void restoreSVGState() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(pushPopStack.pop());
            this.rootElement = (Svg) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        classToElementMap.clear();
        idToElementMap.clear();
    }

    /**
     * Read and parse an SVG from the given {@code InputStream}.
     *
     * @param is the input stream from which to read the file.
     * @return an SVG instance on which you can call one of the render methods.
     * @throws SVGParseException if there is an error parsing the document.
     */
    public static SVG  getFromInputStream(InputStream is) throws SVGParseException
    {
        SVGParser  parser = new SVGParser();
        SVG svg = parser.parse(is);
        svg.originalStream = svg.convertToPushPopOutputStream();
        return svg;
    }


    /**
     * Read and parse an SVG from the given {@code String}.
     *
     * @param svgDocument the String instance containing the SVG document.
     * @return an SVG instance on which you can call one of the render methods.
     * @throws SVGParseException if there is an error parsing the document.
     */
    public static SVG  getFromString(String svgDocument) throws SVGParseException
    {
        SVGParser  parser = new SVGParser();
        SVG svg = parser.parse(new ByteArrayInputStream(svgDocument.getBytes()));
        svg.originalStream = svg.convertToPushPopOutputStream();
        return svg;
    }


    /**
     * Read and parse an SVG from the given resource location.
     *
     * @param context the Android context of the resource.
     * @param resourceId the resource identifier of the SVG document.
     * @return an SVG instance on which you can call one of the render methods.
     * @throws SVGParseException if there is an error parsing the document.
     */
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
    public static SVG  getFromResource(Resources resources, int resourceId) throws SVGParseException
    {
        SVGParser    parser = new SVGParser();
        InputStream  is = resources.openRawResource(resourceId);
        try {
            SVG svg = parser.parse(is);
            svg.originalStream = svg.convertToPushPopOutputStream();
            return svg;
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
    public static SVG  getFromAsset(AssetManager assetManager, String filename) throws SVGParseException, IOException
    {
        SVGParser    parser = new SVGParser();
        InputStream  is = assetManager.open(filename);
        try {
            SVG svg = parser.parse(is);
            svg.originalStream = svg.convertToPushPopOutputStream();
            return svg;
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
     * Register an {@link SVGExternalFileResolver} instance that the renderer should use when resolving
     * external references such as images and fonts.
     *
     * @param fileResolver the resolver to use.
     */
    public void  registerExternalFileResolver(SVGExternalFileResolver fileResolver)
    {
        this.fileResolver = fileResolver;
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
    public void  setRenderDPI(float dpi)
    {
        this.renderDPI = dpi;
    }


    /**
     * Get the current render DPI setting.
     * @return the DPI value
     */
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
     * @return a Picture object suitable for later rendering using {@code Canvas.darwPicture()}
     */
    public Picture  renderToPicture(int widthInPixels, int heightInPixels)
    {
        Picture  picture = new Picture();
        Canvas   canvas = picture.beginRecording(widthInPixels, heightInPixels);
        Box      viewPort = new Box(0f, 0f, (float) widthInPixels, (float) heightInPixels);

        SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, viewPort, this.renderDPI);

        renderer.renderDocument(this, null, null, false);

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

        SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, viewPort, this.renderDPI);

        renderer.renderDocument(this, view.viewBox, view.preserveAspectRatio, false);

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
    public void  renderToCanvas(Canvas canvas, RectF viewPort)
    {
        Box  svgViewPort;

        if (viewPort != null) {
            svgViewPort = Box.fromLimits(viewPort.left, viewPort.top, viewPort.right, viewPort.bottom);
        } else {
            svgViewPort = new Box(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight());
        }

        SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, svgViewPort, this.renderDPI);

        renderer.renderDocument(this, null, null, true);
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

        Box  svgViewPort;

        if (viewPort != null) {
            svgViewPort = Box.fromLimits(viewPort.left, viewPort.top, viewPort.right, viewPort.bottom);
        } else {
            svgViewPort = new Box(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight());
        }

        SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, svgViewPort, this.renderDPI);

        renderer.renderDocument(this, view.viewBox, view.preserveAspectRatio, true);
    }


    //===============================================================================
    // Other document utility API functions


    /**
     * Returns the version number of this library.
     *
     * @return the version number in string format
     */
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
    public Set<String> getViewList()
    {
        if (this.rootElement == null)
            throw new IllegalArgumentException("SVG document is empty");

      List<SvgObject>  viewElems = getElementsByTagName(View.class);

        Set<String>  viewIds = new HashSet<String>(viewElems.size());
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
    public void  setDocumentWidth(String value) throws SVGParseException
    {
        if (this.rootElement == null)
            throw new IllegalArgumentException("SVG document is empty");

        try {
            this.rootElement.width = SVGParser.parseLength(value);
        } catch (SAXException e) {
            throw new SVGParseException(e.getMessage());
        }
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
    public void  setDocumentHeight(String value) throws SVGParseException
    {
        if (this.rootElement == null)
            throw new IllegalArgumentException("SVG document is empty");

        try {
            this.rootElement.height = SVGParser.parseLength(value);
        } catch (SAXException e) {
            throw new SVGParseException(e.getMessage());
        }
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


    protected SVG.Svg  getRootElement()
    {
        return rootElement;
    }


    protected void setRootElement(SVG.Svg rootElement)
    {
        this.rootElement = rootElement;
    }


    protected SvgObject  resolveIRI(String iri)
    {
        if (iri == null)
            return null;

        if (iri.length() > 1 && iri.startsWith("#"))
        {
            return getElementById(iri.substring(1));
        }
        return null;
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


    protected void  addCSSRules(Ruleset ruleset)
    {
        this.cssRules.addAll(ruleset);
    }


    protected List<CSSParser.Rule>  getCSSRules()
    {
        return this.cssRules.getRules();
    }


    protected boolean  hasCSSRules()
    {
        return !this.cssRules.isEmpty();
    }


    //===============================================================================
    // Object sub-types used in the SVG object tree


    protected static class  Box implements Cloneable, Serializable
    {
        private static final long serialVersionUID = 12202L;
        public float  minX, minY, width, height;

        public Box(float minX, float minY, float width, float height)
        {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(java.io.ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();

//            out.writeFloat(minX);
//            out.writeFloat(minY);
//            out.writeFloat(width);
//            out.writeFloat(height);

            
        }

        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();

//            minX = in.readFloat();
//            minY = in.readFloat();
//            width = in.readFloat();
//            height = in.readFloat();

            
        }

        public static Box  fromLimits(float minX, float minY, float maxX, float maxY)
        {
            return new Box(minX, minY, maxX-minX, maxY-minY);
        }

        public RectF  toRectF()
        {
            return new RectF(minX, minY, maxX(), maxY());
        }

        public float  maxX() { return minX + width; }
        public float  maxY() { return minY + height; }

        public void  union(Box other)
        {
            if (other.minX < minX) minX = other.minX;
            if (other.minY < minY) minY = other.minY;
            if (other.maxX() > maxX()) width = other.maxX() - minX;
            if (other.maxY() > maxY()) height = other.maxY() - minY;
        }

        public String toString() { return "["+minX+" "+minY+" "+width+" "+height+"]"; }
    }


    protected static final long SPECIFIED_FILL                  = (1<<0);
    protected static final long SPECIFIED_FILL_RULE             = (1<<1);
    protected static final long SPECIFIED_FILL_OPACITY          = (1<<2);
    protected static final long SPECIFIED_STROKE                = (1<<3);
    protected static final long SPECIFIED_STROKE_OPACITY        = (1<<4);
    protected static final long SPECIFIED_STROKE_WIDTH          = (1<<5);
    protected static final long SPECIFIED_STROKE_LINECAP        = (1<<6);
    protected static final long SPECIFIED_STROKE_LINEJOIN       = (1<<7);
    protected static final long SPECIFIED_STROKE_MITERLIMIT     = (1<<8);
    protected static final long SPECIFIED_STROKE_DASHARRAY      = (1<<9);
    protected static final long SPECIFIED_STROKE_DASHOFFSET     = (1<<10);
    protected static final long SPECIFIED_OPACITY               = (1<<11);
    protected static final long SPECIFIED_COLOR                 = (1<<12);
    protected static final long SPECIFIED_FONT_FAMILY           = (1<<13);
    protected static final long SPECIFIED_FONT_SIZE             = (1<<14);
    protected static final long SPECIFIED_FONT_WEIGHT           = (1<<15);
    protected static final long SPECIFIED_FONT_STYLE            = (1<<16);
    protected static final long SPECIFIED_TEXT_DECORATION       = (1<<17);
    protected static final long SPECIFIED_TEXT_ANCHOR           = (1<<18);
    protected static final long SPECIFIED_OVERFLOW              = (1<<19);
    protected static final long SPECIFIED_CLIP                  = (1<<20);
    protected static final long SPECIFIED_MARKER_START          = (1<<21);
    protected static final long SPECIFIED_MARKER_MID            = (1<<22);
    protected static final long SPECIFIED_MARKER_END            = (1<<23);
    protected static final long SPECIFIED_DISPLAY               = (1<<24);
    protected static final long SPECIFIED_VISIBILITY            = (1<<25);
    protected static final long SPECIFIED_STOP_COLOR            = (1<<26);
    protected static final long SPECIFIED_STOP_OPACITY          = (1<<27);
    protected static final long SPECIFIED_CLIP_PATH             = (1<<28);
    protected static final long SPECIFIED_CLIP_RULE             = (1<<29);
    protected static final long SPECIFIED_MASK                  = (1<<30);
    protected static final long SPECIFIED_SOLID_COLOR           = (1L<<31);
    protected static final long SPECIFIED_SOLID_OPACITY         = (1L<<32);
    protected static final long SPECIFIED_VIEWPORT_FILL         = (1L<<33);
    protected static final long SPECIFIED_VIEWPORT_FILL_OPACITY = (1L<<34);
    protected static final long SPECIFIED_VECTOR_EFFECT         = (1L<<35);
    protected static final long SPECIFIED_DIRECTION             = (1L<<36);

    protected static final long SPECIFIED_ALL = 0xffffffff;

    protected static final long SPECIFIED_NON_INHERITING = SPECIFIED_DISPLAY | SPECIFIED_OVERFLOW | SPECIFIED_CLIP
            | SPECIFIED_CLIP_PATH | SPECIFIED_OPACITY | SPECIFIED_STOP_COLOR
            | SPECIFIED_STOP_OPACITY | SPECIFIED_MASK | SPECIFIED_SOLID_COLOR
            | SPECIFIED_SOLID_OPACITY | SPECIFIED_VIEWPORT_FILL
            | SPECIFIED_VIEWPORT_FILL_OPACITY | SPECIFIED_VECTOR_EFFECT;

    protected static class  Style implements Cloneable, Serializable
    {
        private static final long serialVersionUID = 12202L;
        // Which properties have been explicitly specified by this element
        public long       specifiedFlags = 0;

        protected SvgPaint   fill;
        protected FillRule   fillRule;
        protected Float      fillOpacity;

        protected SvgPaint   stroke;
        protected Float      strokeOpacity;
        protected Length     strokeWidth;
        protected LineCaps   strokeLineCap;
        protected LineJoin   strokeLineJoin;
        protected Float      strokeMiterLimit;
        protected Length[]   strokeDashArray;
        protected Length     strokeDashOffset;

        protected Float      opacity; // master opacity of both stroke and fill

        protected Colour     color;

        protected List<String>    fontFamily;
        protected Length          fontSize;
        protected Integer         fontWeight;
        protected FontStyle       fontStyle;
        protected TextDecoration  textDecoration;
        protected TextDirection   direction;

        protected TextAnchor   textAnchor;

        protected Boolean      overflow;  // true if overflow visible
        protected CSSClipRect  clip;

        protected String     markerStart;
        protected String     markerMid;
        protected String     markerEnd;

        protected Boolean    display;    // true if we should display
        protected Boolean    visibility; // true if visible

        protected SvgPaint   stopColor;
        protected Float      stopOpacity;

        protected String     clipPath;
        protected FillRule   clipRule;

        protected String     mask;

        protected SvgPaint   solidColor;
        protected Float      solidOpacity;

        protected SvgPaint   viewportFill;
        protected Float      viewportFillOpacity;

        protected VectorEffect  vectorEffect;


        public static final int  FONT_WEIGHT_NORMAL = 400;
        public static final int  FONT_WEIGHT_BOLD = 700;
        public static final int  FONT_WEIGHT_LIGHTER = -1;
        public static final int  FONT_WEIGHT_BOLDER = +1;


        public enum FillRule
        {
            NonZero,
            EvenOdd
        }

        public enum LineCaps
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

        public static Style  getDefaultStyle()
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
            def.strokeLineCap = LineCaps.Butt;
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
            return def;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            out.writeObject(strokeDashArray == null ? null : new ArrayList<Length>(Arrays.asList(strokeDashArray)));
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            ArrayList<Length> _strokeDashArray = (ArrayList<Length>) in.readObject();
            strokeDashArray = _strokeDashArray == null ? null : (Length[]) _strokeDashArray.toArray();

        }

        // Called on the state.style object to reset the properties that don't inherit
        // from the parent style.
        public void  resetNonInheritingProperties()
        {
            resetNonInheritingProperties(false);
        }

        public void  resetNonInheritingProperties(boolean isRootSVG)
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
        protected Object  clone()
        {
            Style obj;
            try
            {
                obj = (Style) super.clone();
                if (strokeDashArray != null) {
                    obj.strokeDashArray = (Length[]) strokeDashArray.clone();
                }
                return obj;
            }
            catch (CloneNotSupportedException e)
            {
                throw new InternalError(e.toString());
            }
        }

        public void setFill(SvgPaint fill) {
            this.fill = fill;
            this.specifiedFlags |= SPECIFIED_FILL;
        }

        public void setFillRule(FillRule fillRule) {
            this.fillRule = fillRule;
            this.specifiedFlags |= SPECIFIED_FILL_RULE;
        }

        public void setFillOpacity(Float fillOpacity) {
            this.fillOpacity = fillOpacity;
            this.specifiedFlags |= SPECIFIED_FILL_OPACITY;
        }

        public void setStroke(SvgPaint stroke) {
            this.stroke = stroke;
            this.specifiedFlags |= SPECIFIED_STROKE;
        }

        public void setStrokeOpacity(Float strokeOpacity) {
            this.strokeOpacity = strokeOpacity;
            this.specifiedFlags |= SPECIFIED_STROKE_OPACITY;
        }

        public void setStrokeWidth(Length strokeWidth) {
            this.strokeWidth = strokeWidth;
            this.specifiedFlags |= SPECIFIED_STROKE_WIDTH;
        }

        public void setStrokeLineCap(LineCaps strokeLineCap) {
            this.strokeLineCap = strokeLineCap;
            this.specifiedFlags |= SPECIFIED_STROKE_LINECAP;
        }

        public void setStrokeLineJoin(LineJoin strokeLineJoin) {
            this.strokeLineJoin = strokeLineJoin;
            this.specifiedFlags |= SPECIFIED_STROKE_LINEJOIN;
        }

        public void setStrokeMiterLimit(Float strokeMiterLimit) {
            this.strokeMiterLimit = strokeMiterLimit;
            this.specifiedFlags |= SPECIFIED_STROKE_MITERLIMIT;
        }

        public void setStrokeDashArray(Length[] strokeDashArray) {
            this.strokeDashArray = strokeDashArray;
            this.specifiedFlags |= SPECIFIED_STROKE_DASHARRAY;
        }

        public void setStrokeDashOffset(Length strokeDashOffset) {
            this.strokeDashOffset = strokeDashOffset;
            this.specifiedFlags |= SPECIFIED_STROKE_DASHOFFSET;
        }

        public void setOpacity(Float opacity) {
            this.opacity = opacity;
            this.specifiedFlags |= SPECIFIED_OPACITY;
        }

        public void setColor(Colour color) {
            this.color = color;
            this.specifiedFlags |= SPECIFIED_COLOR;
        }

        public void setFontFamily(List<String> fontFamily) {
            this.fontFamily = fontFamily;
            this.specifiedFlags |= SPECIFIED_FONT_FAMILY;
        }

        public void setFontSize(Length fontSize) {
            this.fontSize = fontSize;
            this.specifiedFlags |= SPECIFIED_FONT_SIZE;
        }

        public void setFontWeight(Integer fontWeight) {
            this.fontWeight = fontWeight;
            this.specifiedFlags |= SPECIFIED_FONT_WEIGHT;
        }

        public void setFontStyle(FontStyle fontStyle) {
            this.fontStyle = fontStyle;
            this.specifiedFlags |= SPECIFIED_FONT_STYLE;
        }

        public void setTextDecoration(TextDecoration textDecoration) {
            this.textDecoration = textDecoration;
            this.specifiedFlags |= SPECIFIED_TEXT_DECORATION;
        }

        public void setDirection(TextDirection direction) {
            this.direction = direction;
            this.specifiedFlags |= SPECIFIED_DIRECTION;
        }

        public void setTextAnchor(TextAnchor textAnchor) {
            this.textAnchor = textAnchor;
            this.specifiedFlags |= SPECIFIED_TEXT_ANCHOR;
        }

        public void setOverflow(Boolean overflow) {
            this.overflow = overflow;
            this.specifiedFlags |= SPECIFIED_OVERFLOW;
        }

        public void setClip(CSSClipRect clip) {
            this.clip = clip;
            this.specifiedFlags |= SPECIFIED_CLIP;
        }

        public void setMarkerStart(String markerStart) {
            this.markerStart = markerStart;
            this.specifiedFlags |= SPECIFIED_MARKER_START;
        }

        public void setMarkerMid(String markerMid) {
            this.markerMid = markerMid;
            this.specifiedFlags |= SPECIFIED_MARKER_MID;
        }

        public void setMarkerEnd(String markerEnd) {
            this.markerEnd = markerEnd;
            this.specifiedFlags |= SPECIFIED_MARKER_END;
        }

        public void setDisplay(Boolean display) {
            this.display = display;
            this.specifiedFlags |= SPECIFIED_DISPLAY;
        }

        public void setVisibility(Boolean visibility) {
            this.visibility = visibility;
            this.specifiedFlags |= SPECIFIED_VISIBILITY;
        }

        public void setStopColor(SvgPaint stopColor) {
            this.stopColor = stopColor;
            this.specifiedFlags |= SPECIFIED_STOP_COLOR;
        }

        public void setStopOpacity(Float stopOpacity) {
            this.stopOpacity = stopOpacity;
            this.specifiedFlags |= SPECIFIED_STOP_OPACITY;
        }

        public void setClipPath(String clipPath) {
            this.clipPath = clipPath;
            this.specifiedFlags |= SPECIFIED_CLIP_PATH;
        }

        public void setClipRule(FillRule clipRule) {
            this.clipRule = clipRule;
            this.specifiedFlags |= SPECIFIED_CLIP_RULE;
        }

        public void setMask(String mask) {
            this.mask = mask;
            this.specifiedFlags |= SPECIFIED_MASK;
        }

        public void setSolidColor(SvgPaint solidColor) {
            this.solidColor = solidColor;
            this.specifiedFlags |= SPECIFIED_SOLID_COLOR;
        }

        public void setSolidOpacity(Float solidOpacity) {
            this.solidOpacity = solidOpacity;
            this.specifiedFlags |= SPECIFIED_SOLID_OPACITY;
        }

        public void setViewportFill(SvgPaint viewportFill) {
            this.viewportFill = viewportFill;
            this.specifiedFlags |= SPECIFIED_VIEWPORT_FILL;
        }

        public void setViewportFillOpacity(Float viewportFillOpacity) {
            this.viewportFillOpacity = viewportFillOpacity;
            this.specifiedFlags |= SPECIFIED_VIEWPORT_FILL_OPACITY;
        }

        public void setVectorEffect(VectorEffect vectorEffect) {
            this.vectorEffect = vectorEffect;
            this.specifiedFlags |= SPECIFIED_VECTOR_EFFECT;
        }

        public VectorEffect getVectorEffect() {
            return vectorEffect;
        }

        public Boolean isOverflow() {
            return overflow;
        }

        public CSSClipRect getClip() {
            return clip;
        }

        public String getMarkerStart() {
            return markerStart;
        }

        public String getMarkerMid() {
            return markerMid;
        }

        public String getMarkerEnd() {
            return markerEnd;
        }

        public Boolean isDisplay() {
            return display;
        }

        public Boolean isVisibility() {
            return visibility;
        }

        public SvgPaint getStopColor() {
            return stopColor;
        }

        public Float getStopOpacity() {
            return stopOpacity;
        }

        public String getClipPath() {
            return clipPath;
        }

        public FillRule getClipRule() {
            return clipRule;
        }

        public String getMask() {
            return mask;
        }

        public SvgPaint getSolidColor() {
            return solidColor;
        }

        public Float getSolidOpacity() {
            return solidOpacity;
        }

        public SvgPaint getViewportFill() {
            return viewportFill;
        }

        public Float getViewportFillOpacity() {
            return viewportFillOpacity;
        }

        public FillRule getFillRule() {
            return fillRule;
        }

        public Float getFillOpacity() {
            return fillOpacity;
        }

        public SvgPaint getStroke() {
            return stroke;
        }

        public Float getStrokeOpacity() {
            return strokeOpacity;
        }

        public Length getStrokeWidth() {
            return strokeWidth;
        }

        public LineCaps getStrokeLineCap() {
            return strokeLineCap;
        }

        public LineJoin getStrokeLineJoin() {
            return strokeLineJoin;
        }

        public Float getStrokeMiterLimit() {
            return strokeMiterLimit;
        }

        public Length[] getStrokeDashArray() {
            return strokeDashArray;
        }

        public Length getStrokeDashOffset() {
            return strokeDashOffset;
        }

        public Float getOpacity() {
            return opacity;
        }

        public Colour getColor() {
            return color;
        }

        public List<String> getFontFamily() {
            return fontFamily;
        }

        public Length getFontSize() {
            return fontSize;
        }

        public Integer getFontWeight() {
            return fontWeight;
        }

        public FontStyle getFontStyle() {
            return fontStyle;
        }

        public TextDecoration getTextDecoration() {
            return textDecoration;
        }

        public TextDirection getDirection() {
            return direction;
        }

        public TextAnchor getTextAnchor() {
            return textAnchor;
        }

        public SvgPaint getFill() {
            return fill;
        }
    }


    // What fill or stroke is
    protected abstract static class SvgPaint implements Cloneable, Serializable
    {
        private static final long serialVersionUID = 12202L;


    }

    protected static class Colour extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;

        public int colour;

        public static final Colour BLACK = new Colour(0);  // Black singleton - a common default value.

        public Colour(int val)
        {
            this.colour = val;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            
        }

        public String toString()
        {
            return String.format("#%06x", colour);
        }
    }

    // Special version of Colour that indicates use of 'currentColor' keyword
    protected static class CurrentColor extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        private static CurrentColor  instance = new CurrentColor();

        private CurrentColor()
        {
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            
        }

        public static CurrentColor  getInstance()
        {
            return instance;
        }
    }


    protected static class PaintReference extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        public String    href;
        public SvgPaint  fallback;

        public PaintReference(String href, SvgPaint fallback)
        {
            this.href = href;
            this.fallback = fallback;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            
        }

        public String toString()
        {
            return href + " " + fallback;
        }
    }


    /**
     * @hide
     */
    protected static class Length implements Cloneable, Serializable
    {
        private static final long serialVersionUID = 12202L;
        float  value = 0;
        Unit   unit = Unit.px;

        public Length(float value, Unit unit)
        {
            this.value = value;
            this.unit = unit;
        }

        public Length(float value)
        {
            this.value = value;
            this.unit = Unit.px;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            
        }

        public float floatValue()
        {
            return value;
        }

        // Convert length to user units for a horizontally-related context.
        public float floatValueX(SVGAndroidRenderer renderer)
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
        public float floatValueY(SVGAndroidRenderer renderer)
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
        public float floatValue(SVGAndroidRenderer renderer)
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
        public float floatValue(SVGAndroidRenderer renderer, float max)
        {
            if (unit == Unit.percent)
            {
                return value * max / 100f;
            }
            return floatValueX(renderer);
        }

        // For situations (like calculating the initial viewport) when we can only rely on
        // physical real world units.
        public float floatValue(float dpi)
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

        public boolean isZero()
        {
            return value == 0f;
        }

        public boolean isNegative()
        {
            return value < 0f;
        }

        @Override
        public String toString()
        {
            return String.valueOf(value) + unit;
        }
    }


    protected static class CSSClipRect implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        public Length  top;
        public Length  right;
        public Length  bottom;
        public Length  left;

        public CSSClipRect(Length top, Length right, Length bottom, Length left)
        {
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
        }
        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
            
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
            
        }
    }


    //===============================================================================
    // The objects in the SVG object tree
    //===============================================================================


    // Any object that can be part of the tree
    protected static class SvgObject implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        transient public SVG           document;
        transient public SvgContainer  parent;

        public String  toString()
        {
            return this.getClass().getSimpleName();
            //return super.toString();
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    // Any object in the tree that corresponds to an SVG element
    protected static class SvgElementBase extends SvgObject
    {
        private static final long serialVersionUID = 12202L;
        public String        id = null;
        public Boolean       spacePreserve = null;
        public Style         baseStyle = null;   // style defined by explicit style attributes in the element (eg. fill="black")
        public Style         style = null;       // style expressed in a 'style' attribute (eg. style="fill:black")
        public List<String>  classNames = null;  // contents of the 'class' attribute

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    // Any object in the tree that corresponds to an SVG element
    protected static class SvgElement extends SvgElementBase
    {
        private static final long serialVersionUID = 12202L;
        public Box     boundingBox = null;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    // Any element that can appear inside a <switch> element.
    protected interface SvgConditional
    {
        public void         setRequiredFeatures(Set<String> features);
        public Set<String>  getRequiredFeatures();
        public void         setRequiredExtensions(String extensions);
        public String       getRequiredExtensions();
        public void         setSystemLanguage(Set<String> languages);
        public Set<String>  getSystemLanguage();
        public void         setRequiredFormats(Set<String> mimeTypes);
        public Set<String>  getRequiredFormats();
        public void         setRequiredFonts(Set<String> fontNames);
        public Set<String>  getRequiredFonts();
    }


    // Any element that can appear inside a <switch> element.
    protected static class  SvgConditionalElement extends SvgElement implements SvgConditional
    {
        private static final long serialVersionUID = 12202L;
        public Set<String>  requiredFeatures = null;
        public String       requiredExtensions = null;
        public Set<String>  systemLanguage = null;
        public Set<String>  requiredFormats = null;
        public Set<String>  requiredFonts = null;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

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


    protected interface SvgContainer
    {
        public List<SvgObject>  getChildren();
        public void             addChild(SvgObject elem) throws SAXException;
    }


    protected static class SvgConditionalContainer extends SvgElement implements SvgContainer, SvgConditional
    {
        private static final long serialVersionUID = 12202L;
        public List<SvgObject> children = new ArrayList<SvgObject>();

        public Set<String>  requiredFeatures = null;
        public String       requiredExtensions = null;
        public Set<String>  systemLanguage = null;
        public Set<String>  requiredFormats = null;
        public Set<String>  requiredFonts = null;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public List<SvgObject>  getChildren() { return children; }
        @Override
        public void addChild(SvgObject elem) throws SAXException  { children.add(elem); }

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


    protected interface HasTransform
    {
        public void setTransform(SerializableMatrix matrix);
    }


    protected static class SvgPreserveAspectRatioContainer extends SvgConditionalContainer
    {
        public PreserveAspectRatio  preserveAspectRatio = null;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    protected static class SvgViewBoxContainer extends SvgPreserveAspectRatioContainer
    {
        public Box  viewBox;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Svg extends SvgViewBoxContainer
    {
        public Length  x;
        public Length  y;
        public Length  width;
        public Length  height;
        public String  version;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    // An SVG element that can contain other elements.
    protected static class Group extends SvgConditionalContainer implements HasTransform
    {
        public SerializableMatrix  transform;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void setTransform(SerializableMatrix transform) { this.transform = transform; }
    }


    protected interface NotDirectlyRendered
    {
    }


    // A <defs> object contains objects that are not rendered directly, but are instead
    // referenced from other parts of the file.
    protected static class Defs extends Group implements NotDirectlyRendered
    {
    }


    // One of the element types that can cause graphics to be drawn onto the target canvas.
    // Specifically: �circle�, �ellipse�, �image�, �line�, �path�, �polygon�, �polyline�, �rect�, �text� and �use�.
    protected static abstract class GraphicsElement extends SvgConditionalElement implements HasTransform
    {
        public SerializableMatrix  transform;

        @Override
        public void setTransform(SerializableMatrix transform) { this.transform = transform; }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Use extends Group
    {
        public String  href;
        public Length  x;
        public Length  y;
        public Length  width;
        public Length  height;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Path extends GraphicsElement
    {
        public PathDefinition  d;
        public Float           pathLength;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Rect extends GraphicsElement
    {
        public Length  x;
        public Length  y;
        public Length  width;
        public Length  height;
        public Length  rx;
        public Length  ry;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Circle extends GraphicsElement
    {
        public Length  cx;
        public Length  cy;
        public Length  r;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Ellipse extends GraphicsElement
    {
        public Length  cx;
        public Length  cy;
        public Length  rx;
        public Length  ry;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Line extends GraphicsElement
    {
        public Length  x1;
        public Length  y1;
        public Length  x2;
        public Length  y2;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class PolyLine extends GraphicsElement
    {
        public float[]  points;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Polygon extends PolyLine
    {
        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    // A root text container such as <text> or <textPath>
    protected interface  TextRoot
    {
    }


    protected interface  TextChild
    {
        public void      setTextRoot(TextRoot obj);
        public TextRoot  getTextRoot();
    }


    protected static class  TextContainer extends SvgConditionalContainer
    {
        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void  addChild(SvgObject elem) throws SAXException
        {
            if (elem instanceof TextChild)
                children.add(elem);
            else
                throw new SAXException("Text content elements cannot contain "+elem+" elements.");
        }
    }


    protected static class  TextPositionedContainer extends TextContainer
    {
        public List<Length>  x;
        public List<Length>  y;
        public List<Length>  dx;
        public List<Length>  dy;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Text extends TextPositionedContainer implements TextRoot, HasTransform
    {
        public SerializableMatrix  transform;

        @Override
        public void setTransform(SerializableMatrix transform) { this.transform = transform; }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class TSpan extends TextPositionedContainer implements TextChild
    {
        private TextRoot  textRoot;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
        @Override
        public TextRoot  getTextRoot() { return this.textRoot; }
    }


    protected static class TextSequence extends SvgObject implements TextChild
    {
        public String  text;

        private TextRoot   textRoot;

        public TextSequence(String text)
        {
            this.text = text;
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
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


    protected static class TRef extends TextContainer implements TextChild
    {
        public String  href;

        private TextRoot   textRoot;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
        @Override
        public TextRoot  getTextRoot() { return this.textRoot; }
    }


    protected static class TextPath extends TextContainer implements TextChild
    {
        public String  href;
        public Length  startOffset;

        private TextRoot  textRoot;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void  setTextRoot(TextRoot obj) { this.textRoot = obj; }
        @Override
        public TextRoot  getTextRoot() { return this.textRoot; }
    }


    // An SVG element that can contain other elements.
    protected static class Switch extends Group
    {
    }


    protected static class Symbol extends SvgViewBoxContainer implements NotDirectlyRendered
    {
    }


    protected static class Marker extends SvgViewBoxContainer implements NotDirectlyRendered
    {
        public boolean  markerUnitsAreUser;
        public Length   refX;
        public Length   refY;
        public Length   markerWidth;
        public Length   markerHeight;
        public Float    orient;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    protected static class GradientElement extends SvgElementBase implements SvgContainer
    {
        public List<SvgObject> children = new ArrayList<SvgObject>();

        public Boolean         gradientUnitsAreUser;
        public Matrix          gradientTransform;
        public GradientSpread  spreadMethod;
        public String          href;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public List<SvgObject> getChildren()
        {
            return children;
        }

        @Override
        public void addChild(SvgObject elem) throws SAXException
        {
            if (elem instanceof Stop)
                children.add(elem);
            else
                throw new SAXException("Gradient elements cannot contain "+elem+" elements.");
        }
    }


    protected static class Stop extends SvgElementBase implements SvgContainer
    {
        public Float  offset;

        // Dummy container methods. Stop is officially a container, but we
        // are not interested in any of its possible child elements.
        @Override
        public List<SvgObject> getChildren() { return Collections.emptyList(); }
        @Override
        public void addChild(SvgObject elem) throws SAXException { /* do nothing */ }
    }


    protected static class SvgLinearGradient extends GradientElement
    {
        public Length  x1;
        public Length  y1;
        public Length  x2;
        public Length  y2;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class SvgRadialGradient extends GradientElement
    {
        public Length  cx;
        public Length  cy;
        public Length  r;
        public Length  fx;
        public Length  fy;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class ClipPath extends Group implements NotDirectlyRendered
    {
        public Boolean  clipPathUnitsAreUser;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Pattern extends SvgViewBoxContainer implements NotDirectlyRendered
    {
        public Boolean  patternUnitsAreUser;
        public Boolean  patternContentUnitsAreUser;
        public Matrix   patternTransform;
        public Length   x;
        public Length   y;
        public Length   width;
        public Length   height;
        public String   href;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class Image extends SvgPreserveAspectRatioContainer implements HasTransform
    {
        public String  href;
        public Length  x;
        public Length  y;
        public Length  width;
        public Length  height;
        public SerializableMatrix  transform;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        @Override
        public void setTransform(SerializableMatrix transform) { this.transform = transform; }
    }


    protected static class View extends SvgViewBoxContainer implements NotDirectlyRendered
    {
    }


    protected static class Mask extends SvgConditionalContainer implements NotDirectlyRendered
    {
        public Boolean  maskUnitsAreUser;
        public Boolean  maskContentUnitsAreUser;
        public Length   x;
        public Length   y;
        public Length   width;
        public Length   height;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }
    }


    public static class SolidColor extends SvgElementBase implements SvgContainer
    {
        public Length  solidColor;
        public Length  solidOpacity;

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            super.writeObjectForInherited(out);
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            super.readObjectForInherited(in);
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        // Dummy container methods. Stop is officially a container, but we
        // are not interested in any of its possible child elements.
        @Override
        public List<SvgObject> getChildren() { return Collections.emptyList(); }
        @Override
        public void addChild(SvgObject elem) throws SAXException { /* do nothing */ }
    }


    //===============================================================================
    // Protected setters for internal use


    protected void setTitle(String title)
    {
        this.title = title;
    }


    protected void setDesc(String desc)
    {
        this.desc = desc;
    }


    protected SVGExternalFileResolver  getFileResolver()
    {
        return fileResolver;
    }


    //===============================================================================
    // Path definition


    protected interface PathInterface
    {
        public void  moveTo(float x, float y);
        public void  lineTo(float x, float y);
        public void  cubicTo(float x1, float y1, float x2, float y2, float x3, float y3);
        public void  quadTo(float x1, float y1, float x2, float y2);
        public void  arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y);
        public void  close();
    }


    protected static class PathDefinition implements PathInterface, Serializable
    {
        private static final long serialVersionUID = 12202L;

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


        public PathDefinition()
        {
            this.commands = new byte[8];
            this.coords = new float[16];
        }

        protected Object readResolve() throws ObjectStreamException {
            return this;
        }

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            // write 'this' to 'out'...
            out.defaultWriteObject();
        }

        protected void writeObjectForInherited(ObjectOutputStream out)
                throws IOException {
            out.writeObject(this);
        }

        @SuppressWarnings("unchecked")
        private void readObject(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            // populate the fields of 'this' from the data in 'in'...
            in.defaultReadObject();
        }

        protected void readObjectForInherited(java.io.ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.readObject();
        }

        public boolean  isEmpty()
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


        public void enumeratePath(PathInterface handler)
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

    /**
     * Get {@code SvgObject} by the given id in this SVG.
     *
     * @param id the requested id.
     * @return the {@code SvgObject} with the given id in this SVG. Cast the retrieved object to the original Class by yourself.
     */
    public SvgObject  getElementById(String id)
    {
        if (id == null || id.length() == 0)
            return null;
        if (id.equals(rootElement.id))
            return rootElement;

        if (idToElementMap.containsKey(id))
            return idToElementMap.get(id);

        // Search the object tree for a node with id property that matches 'id'
        SvgElementBase  result = getElementById(rootElement, id);
        return result;
    }

    /**
     * Get {@code SvgObject} by the given id which located in the given {@code SvgContainer}.
     *
     * @param obj where the {@code SvgObject} located in.
     * @param id the requested id.
     * @return the {@code SvgObject} with the given id in the given container. Cast the retrieved object to the original Class by yourself.
     */
    public SvgElementBase  getElementById(SvgContainer obj, String id)
    {
        SvgElementBase  elem = (SvgElementBase) obj;
        if (id.equals(elem.id))
            return elem;
        for (SvgObject child: obj.getChildren())
        {
            if (!(child instanceof SvgElementBase))
                continue;
            SvgElementBase  childElem = (SvgElementBase) child;

            if (id.equals(childElem.id)) {
                idToElementMap.put(id, childElem);
                return childElem;
            }

            if (child instanceof SvgContainer)
            {
                SvgElementBase  found = getElementById((SvgContainer) child, id);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Get {@code SvgObject}s by the given class name in this SVG.
     *
     * @param className the requested class name (SVG class attribute, not JAVA {@code Class}).
     * @return the list of {@code SvgObject} with the given class name in this SVG. Cast the retrieved object to the original Class by yourself.
     */
    public List<SvgElementBase>  getElementsByClass(String className)
    {
        if (className == null || className.length() == 0)
            return null;

        if (classToElementMap.containsKey(className))
            return classToElementMap.get(className);

        // Search the object tree for a node with id property that matches 'id'
        return getElementsByClass(rootElement, className);
    }

    /**
     * Get {@code SvgObject}s by the given class name which located in the given {@code SvgContainer}.
     *
     * @param obj where the {@code SvgObject} located in.
     * @param className the requested class name (SVG class attribute, not JAVA {@code Class}).
     * @return the list of {@code SvgObject} with the given class name in the given container. Cast the retrieved object to the original Class by yourself.
     */
    public List<SvgElementBase>  getElementsByClass(SvgContainer obj, String className)
    {
        Set<SvgElementBase> elemSet = new LinkedHashSet<SvgElementBase>();

        SvgElementBase  elem = (SvgElementBase) obj;

        for (String elemClassName : elem.classNames) {
            if (className.equals(elemClassName)) {
                elemSet.add(elem);
            }
        }
        for (SvgObject child: obj.getChildren())
        {
            if (!(child instanceof SvgElementBase))
                continue;
            SvgElementBase  childElem = (SvgElementBase) child;
            if (child instanceof SvgContainer)
            {
                List<SvgElementBase> foundInContainer = (List<SvgElementBase>) getElementsByClass((SvgContainer) child, className);
                elemSet.addAll(foundInContainer);
            } else {
                for (String childElemClassName : childElem.classNames) {
                    if (className.equals(childElemClassName))
                        elemSet.add(childElem);
                }
            }
        }
        if (obj == rootElement) {
            List<SvgElementBase> storedElemList = classToElementMap.get(className);
            if (storedElemList == null) storedElemList = new ArrayList<SvgElementBase>();

            Set<SvgElementBase> storedElemSet = new LinkedHashSet<SvgElementBase>(storedElemList);
            storedElemSet.addAll(elemSet);

            storedElemList.clear();
            storedElemList.addAll(storedElemSet);
            classToElementMap.put(className, storedElemList);
        }

        return new ArrayList<SvgElementBase>(elemSet);
    }

    /**
     * Get {@code SvgObject}s by the given tag name which located in this SVG.
     *
     * @param clazz the {@code Class} representing the requested tag name.
     * @return the list of {@code SvgObject} with the given tag name in this SVG. Cast the retrieved object to the original Class by yourself.
     */
    @SuppressWarnings("rawtypes")
    public List<SvgObject>  getElementsByTagName(Class clazz)
    {
        // Search the object tree for nodes with the give element class
        return getElementsByTagName(rootElement, clazz);
    }


    /**
     * Get {@code SvgObject}s by the given tag name which located in the given {@code SvgContainer}.
     *
     * @param obj where the {@code SvgObject} located in.
     * @param clazz the {@code Class} representing the requested tag name.
     * @return the list of {@code SvgObject} with the given tag name in the given container. Cast the retrieved object to the original Class by yourself.
     */
    @SuppressWarnings("rawtypes")
    public List<SvgObject>  getElementsByTagName(SvgContainer obj, Class clazz)
    {
        List<SvgObject>  result = new ArrayList<SvgObject>();

        if (obj.getClass() == clazz)
            result.add((SvgObject) obj);
        for (SvgObject child: obj.getChildren())
        {
            if (child.getClass() == clazz)
                result.add(child);
            if (child instanceof SvgContainer)
                result.addAll(getElementsByTagName((SvgContainer) child, clazz));
        }
        return result;
    }


}
