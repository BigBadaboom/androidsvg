package com.caverock.androidsvg;


import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.util.Log;

import com.caverock.androidsvg.SVG.AspectRatioAlignment;
import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.ClipPath;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.CurrentColor;
import com.caverock.androidsvg.SVG.GradientElement;
import com.caverock.androidsvg.SVG.GradientSpread;
import com.caverock.androidsvg.SVG.Marker;
import com.caverock.androidsvg.SVG.PaintReference;
import com.caverock.androidsvg.SVG.PathDefinition;
import com.caverock.androidsvg.SVG.PathInterface;
import com.caverock.androidsvg.SVG.Rect;
import com.caverock.androidsvg.SVG.Stop;
import com.caverock.androidsvg.SVG.Style;
import com.caverock.androidsvg.SVG.SvgElement;
import com.caverock.androidsvg.SVG.SvgLinearGradient;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVG.SvgRadialGradient;
import com.caverock.androidsvg.SVG.Text;
import com.caverock.androidsvg.SVG.TextContainer;
import com.caverock.androidsvg.SVG.TextSequence;


public class SVGAndroidRenderer
{
   private static final String  TAG = "SVGAndroidRenderer";

   private Canvas  canvas;
   private float   dpi = 160;    // dots per inch. Needed for accurate conversion of length values that have real world units, such as "cm".

   // Renderer state
   private RendererState  state = new RendererState();
   private Stack<RendererState> stateStack = new Stack<RendererState>();  // Keeps track of render state as we render

   private static final Box  UNIT_BBOX = new Box(0,0,1,1);
   
   private static final float  BEZIER_ARC_FACTOR = 0.5522847498f;


   private class RendererState implements Cloneable
   {
      public Style    style;
      public boolean  hasFill;
      public boolean  hasStroke;
      public Paint    fillPaint;
      public Paint    strokePaint;
      public SVG.Box  viewPort;
      public SVG.Box  viewBox;

      // Set when we are about to render an object by reference rather than directly. Eg. via <use>.
      public boolean  overrideDisplay;


      public RendererState()
      {
         fillPaint = new Paint();
         fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
         fillPaint.setStyle(Paint.Style.FILL);
         fillPaint.setTypeface(Typeface.DEFAULT);

         strokePaint = new Paint();
         strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
         strokePaint.setStyle(Paint.Style.STROKE);
         strokePaint.setTypeface(Typeface.DEFAULT);

         style = new Style();
         // Initialise the style state
         updateStyle(this, Style.getDefaultStyle());
      }

      @Override
      protected Object  clone()
      {
         RendererState obj;
         try
         {
            obj = (RendererState) super.clone();
            obj.style = (Style) style.clone();
            obj.fillPaint = new Paint(fillPaint);
            obj.strokePaint = new Paint(strokePaint);
            return obj;
         }
         catch (CloneNotSupportedException e)
         {
            throw new InternalError(e.toString());
         }
      }

      @Override
      public String toString()
      {
         StringBuilder sb = new StringBuilder();
         sb.append(style); sb.append("\n");
         sb.append(hasFill); sb.append("\n");
         sb.append(hasStroke); sb.append("\n");
         sb.append(fillPaint); sb.append("\n");
         sb.append(strokePaint); sb.append("\n");
         sb.append(viewPort); sb.append("\n");
         sb.append(viewBox); sb.append("\n");
         return sb.toString();
      }

   }


   /**
    * Create a new renderer instance.
    *
    * @param canvas The canvas to draw to.
    * @param viewPort The default viewport to be rendered into. For example the dimensions of the bitmap.
    * @param dpi The DPI setting to use when converting real-world units such as centimetres.
    */

   public SVGAndroidRenderer(Canvas canvas, SVG.Box viewPort, float dpi)
   {
      this.canvas = canvas;
      this.dpi = dpi;

      state.viewPort = viewPort;

      // Push a copy of the state with 'default' style, so that inherit works for top level objects
      stateStack.push((RendererState) state.clone());   // Manual push here - don't use statePush();
   }


   public float  getDPI()
   {
      return dpi;
   }


   public float  getCurrentFontSize()
   {
      return state.fillPaint.getTextSize();
   }


   public float  getCurrentFontXHeight()
   {
      // The CSS3 spec says to use 0.5em if there is no way to determine true x-height;
      return state.fillPaint.getTextSize() / 2f;
   }


   /**
    * Get the current view port in user units.
    * @return
    */
   public SVG.Box getCurrentViewPortinUserUnits()
   {
      return state.viewBox;
   }


   public void  renderDocument(SVG document, AspectRatioAlignment alignment, boolean fitToCanvas)
   {
      // Calculate the initial transform to position the document in our canvas
      SVG.Svg  obj = document.getRootElement();
      
      if (obj.viewBox != null) {
         canvas.concat(calculateViewBoxTransform(state.viewPort, obj.viewBox, alignment, !fitToCanvas));
         state.viewPort = obj.viewBox;
      }

      render(obj);
   }


   //==============================================================================
   // Render dispatcher


   public void  render(SVG.SvgObject obj)
   {
      if (!display(obj))
         return;

      // Save state
      statePush();

      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if (obj instanceof SVG.Defs) { // A subclass of Group so it needs to come before that
         // do nothing
      } else if (obj instanceof SVG.Group) {
         render((SVG.Group) obj);
      } else if (obj instanceof SVG.Use) {
         render((SVG.Use) obj);
      } else if (obj instanceof SVG.Path) {
         render((SVG.Path) obj);
      } else if (obj instanceof SVG.Rect) {
         render((SVG.Rect) obj);
      } else if (obj instanceof SVG.Circle) {
         render((SVG.Circle) obj);
      } else if (obj instanceof SVG.Ellipse) {
         render((SVG.Ellipse) obj);
      } else if (obj instanceof SVG.Line) {
         render((SVG.Line) obj);
      } else if (obj instanceof SVG.Polygon) {
         render((SVG.Polygon) obj);
      } else if (obj instanceof SVG.PolyLine) {
         render((SVG.PolyLine) obj);
      } else if (obj instanceof SVG.Text) {
         render((SVG.Text) obj);
      } else if (obj instanceof SVG.Symbol) {
         // do nothing
      } else if (obj instanceof SVG.Marker) {
         // do nothing
      }

      // Restore state
      statePop();
   }


   //==============================================================================


   private void  statePush()
   {
      // Save matrix and clip
      canvas.save();
      // Save style state
      stateStack.push(state);
      state = (RendererState) state.clone();
   }


   private void  statePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
   }


   //==============================================================================
   // Renderers for each element type


   private void render(SVG.Svg obj)
   {
      render(obj, obj.width, obj.height);
   }


   // When referenced by a <use> element, it's width and height take precedence over the ones in the <svg> object.
   private void render(SVG.Svg obj, SVG.Length width, SVG.Length height)
   {
/**/Log.d(TAG, "Svg render");

      if ((width != null && width.isZero()) ||
          (height != null && height.isZero()))
         return;

      updateStyle(state, obj.style);

      state.viewBox = obj.viewBox;

      // <svg> elements establish a new viewport.
      // But in the case of the root element, it has already been done for us.
      if (obj.parent != null)
      {
         float  _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
         float  _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
         float  _w = (width != null) ? width.floatValueX(this) : state.viewPort.width;
         float  _h = (height != null) ? height.floatValueX(this) : state.viewPort.height;
         state.viewPort = new SVG.Box(_x, _y, _w, _h);
      }
      if (!state.style.overflow) {
         setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);
      }

      checkForClipPath(obj, state.viewPort);

     if (obj.viewBox != null) {
         canvas.concat(calculateViewBoxTransform(state.viewPort, obj.viewBox, obj.preserveAspectRatioAlignment, obj.preserveAspectRatioSlice));
      }

      for (SVG.SvgObject child: obj.children) {
         render((SvgElement) child);
      }
   }


   private void render(SVG.Group obj)
   {
/**/Log.d(TAG, "Group render");
      updateStyle(state, obj.style);

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      checkForClipPath(obj);

      for (SVG.SvgObject child: obj.children) {
         render((SvgElement) child);
      }
   }


   private void render(SVG.Use obj)
   {
/**/Log.d(TAG, "Use render");

      updateStyle(state, obj.style);

      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(obj.href);
      if (ref == null)
         return;

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      // We handle the x,y,width,height attributes by adjusting the transform
      Matrix m = new Matrix();
      float _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      float _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      m.preTranslate(_x, _y);
      canvas.concat(m);

      checkForClipPath(obj);

      if (ref instanceof SVG.Svg) {
         render((SVG.Svg) ref, obj.width, obj.height);
      } else if (ref instanceof SVG.Symbol) {
         render((SVG.Symbol) ref, obj.width, obj.height);
      } else {
         render(ref);
      }
   }


   //==============================================================================


   private void render(SVG.Path obj)
   {
//Log.d(TAG, "Path render");

      updateStyle(state, obj.style);

      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = (new PathConverter(obj.path)).getPath();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);
      
      if (state.hasFill) {
         path.setFillType(getFillTypeFromState());
         canvas.drawPath(path, state.fillPaint);
      }
      if (state.hasStroke)
         canvas.drawPath(path, state.strokePaint);

      renderMarkers(obj);
   }


   private Box  calculatePathBounds(Path path)
   {
      RectF  pathBounds = new RectF();
      path.computeBounds(pathBounds, true);
      return new Box(pathBounds.left, pathBounds.bottom, pathBounds.width(), pathBounds.height());
   }


   //==============================================================================


   private void render(SVG.Rect obj)
   {
/**/Log.d(TAG, "Rect render");
      if (obj.width == null || obj.height == null || obj.width.isZero() || obj.height.isZero())
         return;

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _x, _y, _w, _h, _rx, _ry;
      if (obj.rx == null && obj.ry == null) {
         _rx = 0;
         _ry = 0;
      } else if (obj.rx == null) {
         _rx = _ry = obj.ry.floatValueY(this);
      } else if (obj.ry == null) {
         _rx = _ry = obj.rx.floatValueX(this);
      } else {
         _rx = obj.rx.floatValueX(this);
         _ry = obj.ry.floatValueY(this);
      }
      _rx = Math.min(_rx, obj.width.floatValueX(this) / 2f);
      _ry = Math.min(_ry, obj.height.floatValueY(this) / 2f);
      _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      _w = obj.width.floatValueX(this);
      _h = obj.height.floatValueY(this);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(_x, _y, _w, _h);
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);

      if (state.hasFill)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + _w, _y + _h, state.fillPaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + _w, _y + _h), _rx, _ry, state.fillPaint);
         }
      }
      if (state.hasStroke)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + _w, _y + _h, state.strokePaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + _w, _y + _h), _rx, _ry, state.strokePaint);
         }
      }

   }


   //==============================================================================


   private void render(SVG.Circle obj)
   {
/**/Log.d(TAG, "Circle render");
      if (obj.r == null || obj.r.isZero())
         return;

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _r;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _r = obj.r.floatValue(this);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(_cx-_r, _cy-_r, _r*2, _r*2);
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);

      if (state.hasFill) {
         canvas.drawCircle(_cx, _cy, _r, state.fillPaint);
      }
      if (state.hasStroke) {
         canvas.drawCircle(_cx, _cy, _r, state.strokePaint);
      }

   }


   //==============================================================================


   private void render(SVG.Ellipse obj)
   {
/**/Log.d(TAG, "Ellipse render");
      if (obj.rx == null || obj.ry == null || obj.rx.isZero() || obj.ry.isZero())
         return;

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _rx, _ry;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _rx = obj.rx.floatValueX(this);
      _ry = obj.ry.floatValueY(this);
      RectF oval = new RectF(_cx-_rx, _cy-_ry, _cx+_rx, _cy+_ry);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(oval.left, oval.bottom, oval.width(), oval.height());
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);

      if (state.hasFill) {
         canvas.drawOval(oval, state.fillPaint);
      }
      if (state.hasStroke) {
         canvas.drawOval(oval, state.strokePaint);
      }

   }


   //==============================================================================


   private void render(SVG.Line obj)
   {
/**/Log.d(TAG, "Line render");

      updateStyle(state, obj.style);

      if (!visible())
         return;
      if (!state.hasStroke)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _x1, _y1, _x2, _y2;
      _x1 = (obj.x1 != null) ? obj.x1.floatValueX(this) : 0f;
      _y1 = (obj.y1 != null) ? obj.y1.floatValueY(this) : 0f;
      _x2 = (obj.x2 != null) ? obj.x2.floatValueX(this) : 0f;
      _y2 = (obj.y2 != null) ? obj.y2.floatValueY(this) : 0f;

      if (obj.boundingBox == null) {
         obj.boundingBox = Box.fromLimits(Math.min(_x1, _x2), Math.min(_y1, _y2), Math.max(_x1, _x2), Math.max(_y1, _y2));
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);

      if ((_x1 != _x2) || (_y1 != _y2))
         canvas.drawLine(_x1, _y1, _x2, _y2, state.strokePaint);
      else
         canvas.drawPoint(_x1, _y1, state.strokePaint);  // Workaround for Android bug 40780

      renderMarkers(obj);
   }


   private List<MarkerVector>  calculateMarkerPositions(SVG.Line obj)
   {
      float _x1, _y1, _x2, _y2;
      _x1 = (obj.x1 != null) ? obj.x1.floatValueX(this) : 0f;
      _y1 = (obj.y1 != null) ? obj.y1.floatValueY(this) : 0f;
      _x2 = (obj.x2 != null) ? obj.x2.floatValueX(this) : 0f;
      _y2 = (obj.y2 != null) ? obj.y2.floatValueY(this) : 0f;

      List<MarkerVector>  markers = new ArrayList<MarkerVector>(2);
      markers.add(new MarkerVector(_x1, _y1, (_x2-_x1), (_y2-_y1)));
      markers.add(new MarkerVector(_x2, _y2, (_x2-_x1), (_y2-_y1)));
      return markers;
   }


   //==============================================================================


   private void render(SVG.PolyLine obj)
   {
/**/Log.d(TAG, "PolyLine render");

      updateStyle(state, obj.style);

      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 2)
         return;

      Path  path = new Path();
      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<numPoints; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);
      
      if (state.hasFill)
         canvas.drawPath(path, state.fillPaint);
      if (state.hasStroke) {
         if (numPoints == 2) {
            // Android path render doesn't draw degenerate lines, so we have to use drawPoint instead.
            canvas.drawPoint(obj.points[0], obj.points[1], state.strokePaint);
         } else {
            canvas.drawPath(path, state.strokePaint);
         }
      }

      renderMarkers(obj);
   }


   private List<MarkerVector>  calculateMarkerPositions(SVG.PolyLine obj)
   {
      int  numPoints = obj.points.length; 
/**/Log.w(TAG, "calculateMarkerPositions "+numPoints);
      if (numPoints < 2)
         return null;

      List<MarkerVector>  markers = new ArrayList<MarkerVector>();
      MarkerVector        lastPos = new MarkerVector(obj.points[0], obj.points[1], 0, 0);
      float               x = 0, y = 0;

      for (int i=2; i<numPoints; i+=2) {
         x = obj.points[i];
         y = obj.points[i+1];
         lastPos.add(x, y);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
         lastPos = newPos;
      }

      // Deal with last point
      if (obj instanceof SVG.Polygon) {
         if (x != obj.points[0] && y != obj.points[1]) {
            x = obj.points[0];
            y = obj.points[1];
            lastPos.add(x, y);
            markers.add(lastPos);
            // Last marker point needs special handling because its orientation depends
            // on the orientation of the very first segment of the path
            MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
            newPos.add(markers.get(0));
            markers.add(newPos);
            markers.set(0, newPos);  // Start marker is the same
         }
      } else {
         markers.add(lastPos);
      }
      return markers;
   }


   //==============================================================================


   private void render(SVG.Polygon obj)
   {
/**/Log.d(TAG, "Polygon render");

      updateStyle(state, obj.style);

      if (!visible())
         return;
      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 4)
         return;

      Path  path = new Path();
      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<numPoints; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }
      path.close();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);
      
      if (state.hasFill)
         canvas.drawPath(path, state.fillPaint);
      if (state.hasStroke)
         canvas.drawPath(path, state.strokePaint);

      renderMarkers(obj);
   }


   //==============================================================================


   private static class TextRenderContext
   {
      float x;
      float y;

      public TextRenderContext(float x, float y)
      {
         this.x = x;
         this.y = y;
      }
   }


   private void render(SVG.Text obj)
   {
/**/Log.d(TAG, "Text render");

      updateStyle(state, obj.style);

      if (obj.transform != null)
         canvas.concat(obj.transform);

      // Get the first coordinate pair from the lists in the x and y properties.
      float  x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
      float  y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
      TextRenderContext currentTextPosition = new TextRenderContext(x, y);

      // Handle text alignment
      if (state.style.textAnchor != Style.TextAnchor.Start) {
         float  textWidth = calculateTextWidth(obj);
         if (state.style.textAnchor == Style.TextAnchor.Middle) {
            currentTextPosition.x -= (textWidth / 2);
         } else {
            currentTextPosition.x -= textWidth;  // 'End' (right justify)
         }
      }

      if (obj.boundingBox == null) {
         StringBuilder  sb = new StringBuilder();
         extractRawText((TextContainer) obj, sb);
         String  str = sb.toString();
         android.graphics.Rect  bbox = new android.graphics.Rect();
         state.fillPaint.getTextBounds(str, 0, str.length(), bbox);
         obj.boundingBox = new Box(bbox.left, bbox.bottom, bbox.width(), bbox.height());
      }
      checkForGradiants(obj);      
      checkForClipPath(obj);
      
      for (SVG.SvgObject child: obj.children) {
         renderText(child, currentTextPosition);
      }
   }


   public void  renderText(SVG.SvgObject obj, TextRenderContext currentTextPosition)
   {
      if (!display(obj))
         return;

      if (obj instanceof SVG.TSpan)
      {
/**/Log.d(TAG, "TSpan render");
         // Save state
         statePush();

         SVG.TSpan tspan = (SVG.TSpan) obj; 

         updateStyle(state, tspan.style);

         for (SVG.SvgObject child: tspan.children) {
            renderText(child, currentTextPosition);
         }

         // Restore state
         statePop();
      }
      else if  (obj instanceof SVG.TRef)
      {
         // Save state
         statePush();

         SVG.TRef tref = (SVG.TRef) obj; 

         updateStyle(state, tref.style);

         // Locate the referenced object
         SVG.SvgObject  ref = obj.document.resolveIRI(tref.href);
         if (ref != null && (ref instanceof TextContainer))
         {
            StringBuilder  str = new StringBuilder();
            extractRawText((TextContainer) ref, str);
            if (str.length() > 0)
               drawText(str.toString(), currentTextPosition);
         }

         // Restore state
         statePop();
      }
      else if  (obj instanceof SVG.TextSequence)
      {
/**/Log.d(TAG, "TextSequence render");
         String  text = ((SVG.TextSequence) obj).text;
         drawText(text, currentTextPosition);
      }
   }


   private void drawText(String text, TextRenderContext currentTextPosition)
   {
      if (visible())
      {
         if (state.hasFill)
            canvas.drawText(text, currentTextPosition.x, currentTextPosition.y, state.fillPaint);
         if (state.hasStroke)
            canvas.drawText(text, currentTextPosition.x, currentTextPosition.y, state.strokePaint);
      }

      // Update the current text position
      currentTextPosition.x += state.fillPaint.measureText(text);
   }


   /*
    * Calculate the approximate width of this line of text.
    * To simplify, we will ignore font changes and just assume that all the text
    * uses the current font.
    */
   private float calculateTextWidth(Text parentTextObj)
   {
      return sumChildWidths(parentTextObj, 0f);
   }

   private float  sumChildWidths(TextContainer parent, float runningTotal)
   {
      for (SVG.SvgObject child: parent.children)
      {
         if (!display(child))
            continue;

         if (child instanceof TextContainer) {
            runningTotal = sumChildWidths((TextContainer) child, runningTotal);
         } else if (child instanceof TextSequence) {
            runningTotal += state.fillPaint.measureText(((TextSequence) child).text);
         }
      }
      return runningTotal;
   }
 

   /*
    * Extract the raw text from a TextContainer. Used by <tref> handler code.
    */
   private void  extractRawText(TextContainer parent, StringBuilder str)
   {
      for (SVG.SvgObject child: parent.children)
      {
         if (child instanceof TextContainer) {
            extractRawText((TextContainer) child, str);
         } else if (child instanceof TextSequence) {
            str.append(((TextSequence) child).text);
         }
      }
   }
 

   //==============================================================================


   private void render(SVG.Symbol obj, SVG.Length width, SVG.Length height)
   {
/**/Log.d(TAG, "Symbol render");

      if ((width != null && width.isZero()) ||
          (height != null && height.isZero()))
         return;

      updateStyle(state, obj.style);

      state.viewBox = obj.viewBox;

      float  _w = (width != null) ? width.floatValueX(this) : state.viewPort.width;
      float  _h = (height != null) ? height.floatValueX(this) : state.viewPort.height;
      state.viewPort = new SVG.Box(0, 0, _w, _h);

      if (!state.style.overflow) {
         setClipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);
      }

      if (obj.viewBox != null) {
         canvas.concat(calculateViewBoxTransform(state.viewPort, obj.viewBox, obj.preserveAspectRatioAlignment, obj.preserveAspectRatioSlice));
      }
      
      for (SVG.SvgObject child: obj.children) {
         render(child);
      }
   }


   //==============================================================================


   private boolean  display(SvgObject obj)
   {
      if (!(obj instanceof SvgElement))
         return true;
      if (state.overrideDisplay)
         return true;
      SvgElement  elem = (SvgElement) obj;
      if (elem.style.display != null)
        return elem.style.display;
      return true;
   }


   private boolean  visible()
   {
      if (state.style.visibility != null)
        return state.style.visibility;
      return true;
   }


   /*
    * Calculate the transform required to fit the supplied viewBox into the current viewPort.
    * See spec section 7.8 for an explanation of how this works.
    * 
    * aspectRatioRule determines where the graphic is placed in the viewPort when aspect ration
    *    is kept.  xMin means left justified, xMid is centred, xMax is right justified etc.
    * slice determines whether we see the whole image or not. True fill the whole viewport.
    *    If slice is false, the image will be "letter-boxed". 
    */
   private Matrix calculateViewBoxTransform(Box viewPort, Box viewBox, AspectRatioAlignment aspectRule, boolean slice)
   {
      if (aspectRule == null)
         aspectRule = AspectRatioAlignment.xMidYMid;

      Matrix m = new Matrix();

      float  xScale = viewPort.width / viewBox.width;
      float  yScale = viewPort.height / viewBox.height;
      float  xOffset = -viewBox.minX;
      float  yOffset = -viewBox.minY;

      // 'none' means scale both dimensions to fit the viewport
      if (aspectRule == AspectRatioAlignment.none)
      {
         m.preTranslate(viewPort.minX, viewPort.minY);
         m.preScale(xScale, yScale);
         m.preTranslate(xOffset, yOffset);
         return m;
      }

      // Otherwise, the aspect ratio of the image is kept.
      // What scale are we going to use?
      float  aspectScale = (slice) ? Math.max(xScale,  yScale) : Math.min(xScale,  yScale);
      // What size will the image end up being? 
      float  imageW = state.viewPort.width / aspectScale;
      float  imageH = state.viewPort.height / aspectScale;
      // Determine final X position
      switch (aspectRule)
      {
         case xMidYMin:
         case xMidYMid:
         case xMidYMax:
            xOffset -= (viewBox.width - imageW) / 2;
            break;
         case xMaxYMin:
         case xMaxYMid:
         case xMaxYMax:
            xOffset -= (viewBox.width - imageW);
            break;
         default:
            // nothing to do 
            break;
      }
      // Determine final Y position
      switch (aspectRule)
      {
         case xMinYMid:
         case xMidYMid:
         case xMaxYMid:
            yOffset -= (viewBox.height - imageH) / 2;
            break;
         case xMinYMax:
         case xMidYMax:
         case xMaxYMax:
            yOffset -= (viewBox.height - imageH);
            break;
         default:
            // nothing to do 
            break;
      }

      m.preTranslate(viewPort.minX, viewPort.minY);
      m.preScale(aspectScale, aspectScale);
      m.preTranslate(xOffset, yOffset);
      return m;
   }


   private boolean  isSpecified(Style style, long flag)
   {
      return (style.specifiedFlags & flag) != 0;
   }


   /*
   private boolean  isInherited(Style style, long flag)
   {
      return (style.inheritFlags & flag) != 0;
   }
   */


   /*
    * Updates the global style state with the style defined by the current object.
    * Will also update the current paints etc where appropriate.
    */
   private void updateStyle(RendererState state, Style style)
   {
      // Some style attributes don't inherit, so first, lets reset those
      state.style.resetNonInheritingProperties();

      // Now update each style property we know about
      if (isSpecified(style, SVG.SPECIFIED_COLOR))
      {
         state.style.color = style.color;
      }

      if (isSpecified(style, SVG.SPECIFIED_OPACITY))
      {
         state.style.opacity = style.opacity;
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL))
      {
         state.style.fill = style.fill;
         state.hasFill = (style.fill != null);
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL_OPACITY))
      {
         state.style.fillOpacity = style.fillOpacity;
      }

      // If either fill or its opacity has changed, update the fillPaint
      if (isSpecified(style, SVG.SPECIFIED_FILL | SVG.SPECIFIED_FILL_OPACITY | SVG.SPECIFIED_COLOR | SVG.SPECIFIED_OPACITY))
      {
         if (state.style.fill instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) state.style.fill).colour;
            col = clamp255(state.style.fillOpacity * state.style.opacity) << 24 | col;
            state.fillPaint.setColor(col);
         }
         else if (state.style.fill instanceof CurrentColor)
         {
            int col = state.style.color.colour;
            col = clamp255(state.style.fillOpacity * state.style.opacity) << 24 | col;
            state.fillPaint.setColor(col);
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL_RULE))
      {
         state.style.fillRule = style.fillRule;
      }


      if (isSpecified(style, SVG.SPECIFIED_STROKE))
      {
         state.style.stroke = style.stroke;
         state.hasStroke = (style.stroke != null);
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_OPACITY))
      {
         state.style.strokeOpacity = style.strokeOpacity;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE | SVG.SPECIFIED_STROKE_OPACITY | SVG.SPECIFIED_COLOR | SVG.SPECIFIED_OPACITY))
      {
         if (state.style.stroke instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) state.style.stroke).colour;
            col = clamp255(state.style.strokeOpacity * state.style.opacity) << 24 | col;
            state.strokePaint.setColor(col);
         }
         else if (state.style.stroke instanceof CurrentColor)
         {
            int col = state.style.color.colour;
            col = clamp255(state.style.strokeOpacity * state.style.opacity) << 24 | col;
            state.strokePaint.setColor(col);
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_WIDTH))
      {
         state.style.strokeWidth = style.strokeWidth;
         state.strokePaint.setStrokeWidth(style.strokeWidth.floatValue(this));
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_LINECAP))
      {
         state.style.strokeLineCap = style.strokeLineCap;
         switch (style.strokeLineCap)
         {
            case Butt:
               state.strokePaint.setStrokeCap(Paint.Cap.BUTT);
               break;
            case Round:
               state.strokePaint.setStrokeCap(Paint.Cap.ROUND);
               break;
            case Square:
               state.strokePaint.setStrokeCap(Paint.Cap.SQUARE);
               break;
            default:
               break;
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_LINEJOIN))
      {
         state.style.strokeLineJoin = style.strokeLineJoin;
         switch (style.strokeLineJoin)
         {
            case Miter:
               state.strokePaint.setStrokeJoin(Paint.Join.MITER);
               break;
            case Round:
               state.strokePaint.setStrokeJoin(Paint.Join.ROUND);
               break;
            case Bevel:
               state.strokePaint.setStrokeJoin(Paint.Join.BEVEL);
               break;
            default:
               break;
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_MITERLIMIT))
      {
         state.style.strokeMiterLimit = style.strokeMiterLimit;
         state.strokePaint.setStrokeMiter(style.strokeMiterLimit);
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHARRAY))
      {
         state.style.strokeDashArray = style.strokeDashArray;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHOFFSET))
      {
         state.style.strokeDashOffset = style.strokeDashOffset;
      }

      if (isSpecified(style, SVG.SPECIFIED_STROKE_DASHARRAY | SVG.SPECIFIED_STROKE_DASHOFFSET))
      {
         // Either the dash array or dash offset has changed.
         if (state.style.strokeDashArray == null)
         {
            state.strokePaint.setPathEffect(null);
         }
         else
         {
            float  intervalSum = 0f;
            int    n = state.style.strokeDashArray.length;
            // SVG dash arrays can be odd length, whereas Android dash arrays must have an even length.
            // So we solve the problem by doubling the array length.
            int    arrayLen = (n % 2==0) ? n : n*2;
            float[] intervals = new float[arrayLen];
            for (int i=0; i<arrayLen; i++) {
               intervals[i] = state.style.strokeDashArray[i % n].floatValue(this);
               intervalSum += intervals[i];
            }
            if (intervalSum == 0f) {
               state.strokePaint.setPathEffect(null);
            } else {
               float offset = state.style.strokeDashOffset.floatValue(this);
               if (offset < 0) {
                  // SVG offsets can be negative. Not sure if Android ones can be.
                  // Just in case we will convert it.
                  offset = intervalSum + (offset % intervalSum);
               }
               state.strokePaint.setPathEffect( new DashPathEffect(intervals, offset) );
            }
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_FAMILY))
      {
         state.style.fontFamily = style.fontFamily;
         // NYI
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_SIZE))
      {
         state.style.fontSize = style.fontSize;
         state.fillPaint.setTextSize(style.fontSize.floatValue(this));
         state.strokePaint.setTextSize(style.fontSize.floatValue(this));
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_WEIGHT))
      {
         state.style.fontWeight = style.fontWeight;
      }

      if (isSpecified(style, SVG.SPECIFIED_FONT_STYLE))
      {
         state.style.fontStyle = style.fontStyle;
      }

      // If weight or style has changed, update the typeface
      if (isSpecified(style, SVG.SPECIFIED_FONT_WEIGHT | SVG.SPECIFIED_FONT_STYLE))
      {
         Typeface  font = Typeface.create(Typeface.DEFAULT,  getTypefaceStyle(style));
         state.fillPaint.setTypeface(font);
         state.strokePaint.setTypeface(font);
      }

      if (isSpecified(style, SVG.SPECIFIED_TEXT_DECORATION))
      {
         state.style.textDecoration = style.textDecoration;
         state.fillPaint.setStrikeThruText(style.textDecoration.equals("line-through"));
         state.fillPaint.setUnderlineText(style.textDecoration.equals("underline"));
         // There is a bug in Android <= JELLY_BEAN (16) that causes stroked underlines to
         // not be drawn properly. See bug (39511). This has been fixed in JELLY_BEAN_MR1 (4.2)
         if (android.os.Build.VERSION.SDK_INT >= 17) {
            state.strokePaint.setStrikeThruText(style.textDecoration.equals("line-through"));
            state.strokePaint.setUnderlineText(style.textDecoration.equals("underline"));
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_TEXT_ANCHOR))
      {
         state.style.textAnchor = style.textAnchor;
      }

      if (isSpecified(style, SVG.SPECIFIED_OVERFLOW))
      {
         state.style.overflow = style.overflow;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_START))
      {
         state.style.markerStart = style.markerStart;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_MID))
      {
         state.style.markerMid = style.markerMid;
      }

      if (isSpecified(style, SVG.SPECIFIED_MARKER_END))
      {
         state.style.markerEnd = style.markerEnd;
      }

      if (isSpecified(style, SVG.SPECIFIED_DISPLAY))
      {
         state.style.display = style.display;
      }

      if (isSpecified(style, SVG.SPECIFIED_VISIBILITY))
      {
         state.style.visibility = style.visibility;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP))
      {
         state.style.clip = style.clip;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP_PATH))
      {
         state.style.clipPath = style.clipPath;
      }

      if (isSpecified(style, SVG.SPECIFIED_CLIP_RULE))
      {
         state.style.clipRule = style.clipRule;
      }

   }


   private int  getTypefaceStyle(Style style)
   {
      boolean  italic = (style.fontStyle == Style.FontStyle.Italic);
      if ("bold".equals(style.fontWeight)) {
         return italic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
      }
      return italic ? Typeface.ITALIC : Typeface.NORMAL;
   }


   private int  clamp255(float val)
   {
      int  i = (int)(val * 256f);
      return (i<0) ? 0 : (i>255) ? 255 : i;
   }


   private Path.FillType  getFillTypeFromState()
   {
      if (state.style.fillRule == null)
         return Path.FillType.WINDING;
      switch (state.style.fillRule)
      {
         case EvenOdd:
            return Path.FillType.EVEN_ODD;
         case NonZero:
         default:
            return Path.FillType.WINDING;
      }
   }


   private void setClipRect(float minX, float minY, float width, float height)
   {
      float  left = minX;
      float  top = minY;
      float  right = minX + width;
      float  bottom = minY + height;

      if (state.style.clip != null) {
         left += state.style.clip.left.floatValueX(this);
         top += state.style.clip.top.floatValueY(this);
         right -= state.style.clip.right.floatValueX(this);
         bottom -= state.style.clip.bottom.floatValueY(this);
      }

      canvas.clipRect(left, top, right, bottom);
   }


   //==============================================================================

   /*
    *  Convert an internal PathDefinition to an android.graphics.Path object
    */
   private class  PathConverter implements PathInterface
   {
      Path   path = new Path();
      float  lastX, lastY;
      
      public PathConverter(PathDefinition pathDef)
      {
         pathDef.enumeratePath(this);
      }

      public Path  getPath()
      {
         return path;
      }

      @Override
      public void moveTo(float x, float y)
      {
         path.moveTo(x, y);
         lastX = x;
         lastY = y;
      }

      @Override
      public void lineTo(float x, float y)
      {
         path.lineTo(x, y);
         lastX = x;
         lastY = y;
      }

      @Override
      public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         path.cubicTo(x1, y1, x2, y2, x3, y3);
         lastX = x3;
         lastY = y3;
      }

      @Override
      public void quadTo(float x1, float y1, float x2, float y2)
      {
         path.quadTo(x1, y1, x2, y2);
         lastX = x2;
         lastY = y2;
      }

      @Override
      public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y)
      {
         SVGAndroidRenderer.arcTo(lastX, lastY, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y, this);
         lastX = x;
         lastY = y;
      }

      @Override
      public void close()
      {
         path.close();
      }
         
   }


   //=========================================================================
   // Handling of Arcs

   /*
    * SVG arc representation uses "endpoint parameterisation" where we specify the endpoint of the arc.
    * This is to be consistent with the other path commands.  However we need to convert this to "centre point
    * parameterisation" in order to calculate the arc. Handily, the SVG spec provides all the required maths
    * in section "F.6 Elliptical arc implementation notes".
    * 
    * Some of this code has been borrowed from the Batik library (Apache-2 license).
    */

   private static void arcTo(float lastX, float lastY, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag, float x, float y, PathInterface pather)
   {
      if (lastX == x && lastY == y) {
         // If the endpoints (x, y) and (x0, y0) are identical, then this
         // is equivalent to omitting the elliptical arc segment entirely.
         // (behaviour specified by the spec)
         return;
      }

      // Handle degenerate case (behaviour specified by the spec)
      if (rx == 0 || ry == 0) {
         pather.lineTo(x, y);
         return;
      }

      // Sign of the radii is ignored (behaviour specified by the spec)
      rx = Math.abs(rx);
      ry = Math.abs(ry);

      // Convert angle from degrees to radians
      float  angleRad = (float) Math.toRadians(angle % 360.0);
      double cosAngle = Math.cos(angleRad);
      double sinAngle = Math.sin(angleRad);
      
      // We simplify the calculations by transforming the arc so that the origin is at the
      // midpoint calculated above followed by a rotation to line up the coordinate axes
      // with the axes of the ellipse.

      // Compute the midpoint of the line between the current and the end point
      double dx2 = (lastX - x) / 2.0;
      double dy2 = (lastY - y) / 2.0;

      // Step 1 : Compute (x1', y1') - the transformed start point
      double x1 = (cosAngle * dx2 + sinAngle * dy2);
      double y1 = (-sinAngle * dx2 + cosAngle * dy2);

      double rx_sq = rx * rx;
      double ry_sq = ry * ry;
      double x1_sq = x1 * x1;
      double y1_sq = y1 * y1;

      // Check that radii are large enough.
      // If they are not, the spec says to scale them up so they are.
      // This is to compensate for potential rounding errors/differences between SVG implementations.
      double radiiCheck = x1_sq / rx_sq + y1_sq / ry_sq;
      if (radiiCheck > 1) {
         rx = (float) Math.sqrt(radiiCheck) * rx;
         ry = (float) Math.sqrt(radiiCheck) * ry;
         rx_sq = rx * rx;
         ry_sq = ry * ry;
      }

      // Step 2 : Compute (cx1, cy1) - the transformed centre point
      double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
      double sq = ((rx_sq * ry_sq) - (rx_sq * y1_sq) - (ry_sq * x1_sq)) / ((rx_sq * y1_sq) + (ry_sq * x1_sq));
      sq = (sq < 0) ? 0 : sq;
      double coef = (sign * Math.sqrt(sq));
      double cx1 = coef * ((rx * y1) / ry);
      double cy1 = coef * -((ry * x1) / rx);

      // Step 3 : Compute (cx, cy) from (cx1, cy1)
      double sx2 = (lastX + x) / 2.0;
      double sy2 = (lastY + y) / 2.0;
      double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
      double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

      // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
      double ux = (x1 - cx1) / rx;
      double uy = (y1 - cy1) / ry;
      double vx = (-x1 - cx1) / rx;
      double vy = (-y1 - cy1) / ry;
      double p, n;

      // Compute the angle start
      n = Math.sqrt((ux * ux) + (uy * uy));
      p = ux; // (1 * ux) + (0 * uy)
      sign = (uy < 0) ? -1.0 : 1.0;
      double angleStart = Math.toDegrees(sign * Math.acos(p / n));

      // Compute the angle extent
      n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
      p = ux * vx + uy * vy;
      sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
      double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
      if (!sweepFlag && angleExtent > 0) {
         angleExtent -= 360f;
      } else if (sweepFlag && angleExtent < 0) {
         angleExtent += 360f;
      }
      angleExtent %= 360f;
      angleStart %= 360f;

      // Many elliptical arc implementations including the Java2D and Android ones, only
      // support arcs that are axis aligned.  Therefore we need to substitute the arc
      // with bezier curves.  The following method call will generate the beziers for
      // a unit circle that covers the arc angles we want.
      float[]  bezierPoints = arcToBeziers(angleStart, angleExtent);

      // Calculate a transformation matrix that will move and scale these bezier points to the correct location.
      Matrix m = new Matrix();
      m.postScale(rx, ry);
      m.postRotate(angle);
      m.postTranslate((float) cx, (float) cy);
      m.mapPoints(bezierPoints);

      // The last point in the bezier set should match exactly the last coord pair in the arc (ie: x,y). But
      // considering all the mathematical manipulation we have been doing, it is bound to be off by a tiny
      // fraction. Experiments show that it can be up to around 0.00002.  So why don't we just set it to
      // exactly what it ought to be.
      bezierPoints[bezierPoints.length-2] = x;
      bezierPoints[bezierPoints.length-1] = y;

      // Final step is to add the bezier curves to the path
      for (int i=0; i<bezierPoints.length; i+=6)
      {
         pather.cubicTo(bezierPoints[i], bezierPoints[i+1], bezierPoints[i+2], bezierPoints[i+3], bezierPoints[i+4], bezierPoints[i+5]);
      }
   }


   /*
    * Generate the control points and endpoints for a set of bezier curves that match
    * a circular arc starting from angle 'angleStart' and sweep the angle 'angleExtent'.
    * The circle the arc follows will be centred on (0,0) and have a radius of 1.0.
    * 
    * Each bezier can cover no more than 90 degrees, so the arc will be divided evenly
    * into a maximum of four curves.
    * 
    * The resulting control points will later be scaled and rotated to match the final
    * arc required.
    * 
    * The returned array has the format [x0,y0, x1,y1,...] and excludes the start point
    * of the arc.
    */
   private static float[]  arcToBeziers(double angleStart, double angleExtent)
   {
      int    numSegments = (int) Math.ceil(Math.abs(angleExtent) / 90.0);
      
      angleStart = Math.toRadians(angleStart);
      angleExtent = Math.toRadians(angleExtent);
      float  angleIncrement = (float) (angleExtent / numSegments);
      
      // The length of each control point vector is given by the following formula.
      double  controlLength = 4.0 / 3.0 * Math.sin(angleIncrement / 2.0) / (1.0 + Math.cos(angleIncrement / 2.0));
      
      float[] coords = new float[numSegments * 6];
      int     pos = 0;

      for (int i=0; i<numSegments; i++)
      {
         double  angle = angleStart + i * angleIncrement;
         // Calculate the control vector at this angle
         double  dx = Math.cos(angle);
         double  dy = Math.sin(angle);
         // First control point
         coords[pos++]   = (float) (dx - controlLength * dy);
         coords[pos++] = (float) (dy + controlLength * dx);
         // Second control point
         angle += angleIncrement;
         dx = Math.cos(angle);
         dy = Math.sin(angle);
         coords[pos++] = (float) (dx + controlLength * dy);
         coords[pos++] = (float) (dy - controlLength * dx);
         // Endpoint of bezier
         coords[pos++] = (float) dx;
         coords[pos++] = (float) dy;
      }
      return coords;
   }


   //==============================================================================
   // Marker handling
   //==============================================================================


   private class MarkerVector
   {
      public float x, y, dx=0f, dy=0f;

      public MarkerVector(float x, float y, float dx, float dy)
      {
         this.x = x;
         this.y = y;
         // normalise direction vector
         double  len = Math.sqrt( dx*dx + dy*dy );
         if (len != 0) {
            this.dx = (float) (dx / len);
            this.dy = (float) (dy / len);
         }
      }

      public void add(float x, float y)
      {
         // In order to get accurate angles, we have to normalise
         // all vectors before we add them.  As long as they are
         // all the same length, the angles will work out correctly.
         float dx = (x - this.x);
         float dy = (y - this.y);
         double  len = Math.sqrt( dx*dx + dy*dy );
         if (len != 0) {
            this.dx += (float) (dx / len);
            this.dy += (float) (dy / len);
         }
      }

      public void add(MarkerVector v2)
      {
         this.dx += v2.dx;
         this.dy += v2.dy;
      }

      @Override
      public String toString()
      {
         return "("+x+","+y+" "+dx+","+dy+")";
      }
   }
   

   /*
    *  Calculates the positions and orientations of any markers that should be placed on the given path.
    */
   private class  MarkerPositionCalculator implements PathInterface
   {
      private List<MarkerVector>  markers = new ArrayList<MarkerVector>();
      private float               startX, startY;
      private MarkerVector        lastPos = null;
      private boolean             startArc = false, normalCubic = true;
      private int                 subpathStartIndex = -1;
      private boolean             closepathReAdjustPending;

      
      public MarkerPositionCalculator(PathDefinition pathDef)
      {
         // Generate and add markers for the first N-1 points
         pathDef.enumeratePath(this);
         // Add the marker for the pending last point
         if (lastPos != null) {
            markers.add(lastPos);
         }
      }

      public List<MarkerVector>  getMarkers()
      {
         return markers;
      }

      @Override
      public void moveTo(float x, float y)
      {
         if (closepathReAdjustPending) {
            // Now correct the start and end marker points of the subpath.
            // They should both be oriented as if this was a midpoint (ie sum the vectors).
            lastPos.add(markers.get(subpathStartIndex));
            // Overwrite start marker. Other (end) marker will be written on exit or at start of next subpath.
            markers.set(subpathStartIndex,  lastPos);
            closepathReAdjustPending = false;
         }
         if (lastPos != null) {
            markers.add(lastPos);
         }
         startX = x;
         startY = y;
         lastPos = new MarkerVector(x, y, 0, 0);
         subpathStartIndex = markers.size();
      }

      @Override
      public void lineTo(float x, float y)
      {
         lastPos.add(x, y);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x, y, x-lastPos.x, y-lastPos.y);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         if (normalCubic || startArc) {
            lastPos.add(x1, y1);
            markers.add(lastPos);
            startArc = false;
         }
         MarkerVector  newPos = new MarkerVector(x3, y3, x3-x2, y3-y2);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void quadTo(float x1, float y1, float x2, float y2)
      {
         lastPos.add(x1, y1);
         markers.add(lastPos);
         MarkerVector  newPos = new MarkerVector(x2, y2, x2-x1, y2-y1);
         lastPos = newPos;
         closepathReAdjustPending = false;
      }

      @Override
      public void arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y)
      {
         // We'll piggy-back on the arc->bezier conversion to get our start and end vectors
         startArc = true;
         normalCubic = false;
         SVGAndroidRenderer.arcTo(lastPos.x, lastPos.y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y, this);
         normalCubic = true;
         closepathReAdjustPending = false;
      }

      @Override
      public void close()
      {
         markers.add(lastPos);
         lineTo(startX, startY);
         // We may need to readjust the first and last markers on this subpath so that
         // the orientation is a sum of the inward and outward vectors.
         // But this only happens if the path ends or the next subpath starts with a Move.
         // See description of "orient" attribute in section 11.6.2.
         closepathReAdjustPending = true;
      }
         
   }


   private void  renderMarkers(SVG.GraphicsElement obj)
   {
      if (state.style.markerStart == null && state.style.markerMid == null && state.style.markerEnd == null)
         return;

      SVG.Marker  _markerStart = null;
      SVG.Marker  _markerMid = null;
      SVG.Marker  _markerEnd = null;

      if (state.style.markerStart != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerStart);
         if (ref != null)
            _markerStart = (SVG.Marker) ref;
      }

      if (state.style.markerMid != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerMid);
         if (ref != null)
            _markerMid = (SVG.Marker) ref;
      }

      if (state.style.markerEnd != null) {
         SVG.SvgObject  ref = obj.document.resolveIRI(state.style.markerEnd);
         if (ref != null)
            _markerEnd = (SVG.Marker) ref;
      }

      List<MarkerVector>  markers = null;
      if (obj instanceof SVG.Path)
         markers = (new MarkerPositionCalculator(((SVG.Path) obj).path)).getMarkers();
      else if (obj instanceof SVG.Line)
         markers = calculateMarkerPositions((SVG.Line) obj);
      else // PolyLine and Polygon
         markers = calculateMarkerPositions((SVG.PolyLine) obj);
      
      if (markers == null)
         return;

      int  markerCount = markers.size();
      if (markerCount == 0)
         return;

      // Tell children (markers) that they are being indirectly rendered
      state.overrideDisplay = true;

      // We don't want the markers to inherit themselves as markers, otherwise we get infinite recursion. 
      state.style.markerStart = state.style.markerMid = state.style.markerEnd = null;

      if (_markerStart != null)
         renderMarker(_markerStart, markers.get(0));

      if (_markerMid != null)
      {
         for (int i=1; i<(markerCount-1); i++) {
            renderMarker(_markerMid, markers.get(i));
         }
      }

      if (_markerEnd != null)
         renderMarker(_markerEnd, markers.get(markerCount-1));
   }


   /*
    * Render the given marker type at the given position
    */
   private void renderMarker(Marker marker, MarkerVector pos)
   {
      float  angle = 0f;
      float  unitsScale;

      statePush();

      // Calculate vector angle
      if (marker.orient != null)
      {
         if (Float.isNaN(marker.orient))  // Indicates "auto"
         {
            if (pos.dx != 0 || pos.dy != 0) {
               angle = (float) Math.toDegrees( Math.atan2(pos.dy, pos.dx) );
            }
         } else {
            angle = marker.orient;
         }
      }
      // Calculate units scale
      unitsScale = marker.markerUnitsAreUser ? 1f : state.style.strokeWidth.floatValue(dpi);

      Matrix m = new Matrix();
      m.preTranslate(pos.x, pos.y);
      m.preRotate(angle);
      m.preScale(unitsScale, unitsScale);
      // Scale and/or translate the marker to fit in the marker viewPort
      float _refX = (marker.refX != null) ? marker.refX.floatValueX(this) : 0f;
      float _refY = (marker.refY != null) ? marker.refY.floatValueY(this) : 0f;
      float _markerWidth = (marker.markerWidth != null) ? marker.markerWidth.floatValueX(this) : 3f;
      float _markerHeight = (marker.markerHeight != null) ? marker.markerHeight.floatValueY(this) : 3f;
      
      // We now do a simplified version of calculateViewBoxTransform().  For now we will
      // ignore the alignment setting because refX and refY have to be aligned with the
      // marker position, and alignment would complicate the calculations.
      Box   _viewBox = (marker.viewBox != null) ? marker.viewBox : state.viewPort;
      float xScale, yScale;

      xScale = _markerWidth / _viewBox.width;
      yScale = _markerHeight / _viewBox.height;

      // If we are keeping aspect ratio, then set both scales to the appropriate value depending on 'slice'
      AspectRatioAlignment  align = (marker.preserveAspectRatioAlignment != null) ? marker.preserveAspectRatioAlignment :  AspectRatioAlignment.xMidYMid;
      if (align != AspectRatioAlignment.none)
      {
         float  aspectScale = (marker.preserveAspectRatioSlice) ? Math.max(xScale,  yScale) : Math.min(xScale,  yScale);
         xScale = yScale = aspectScale;
      }

      //m.preTranslate(viewPort.minX, viewPort.minY);
      m.preTranslate(-_refX * xScale, -_refY * yScale);
      canvas.concat(m);

      if (!state.style.overflow) {
         // Now we need to take account of alignment setting, because it affects the
         // size and position of the clip rectangle.
         float  imageW = _viewBox.width * xScale;
         float  imageH = _viewBox.height * yScale;
         float  xOffset = 0f;
         float  yOffset = 0f;
         switch (align)
         {
            case xMidYMin:
            case xMidYMid:
            case xMidYMax:
               xOffset -= (_markerWidth - imageW) / 2;
               break;
            case xMaxYMin:
            case xMaxYMid:
            case xMaxYMax:
               xOffset -= (_markerWidth - imageW);
               break;
            default:
               // nothing to do 
               break;
         }
         // Determine final Y position
         switch (align)
         {
            case xMinYMid:
            case xMidYMid:
            case xMaxYMid:
               yOffset -= (_markerHeight - imageH) / 2;
               break;
            case xMinYMax:
            case xMidYMax:
            case xMaxYMax:
               yOffset -= (_markerHeight - imageH);
               break;
            default:
               // nothing to do 
               break;
         }
         setClipRect(xOffset, yOffset, _markerWidth, _markerHeight);
      }

      m.reset();
      m.preScale(xScale, yScale);
      canvas.concat(m);

      // "Properties inherit into the <marker> element from its ancestors; properties do not
      // inherit from the element referencing the <marker> element." (sect 11.6.2)
      state = findInheritFromAncestorState(marker);

      for (SVG.SvgObject child: marker.children) {
         render(child);
      }

      statePop();
   }


   /*
    * Determine an elements style based on it's ancestors in the tree rather than
    * it's render time ancestors.
    */
   private RendererState  findInheritFromAncestorState(SvgObject obj)
   {
      List<Style>    styles = new ArrayList<Style>();

      // Traverse up the document tree adding element styles to a list.
      while (true) {
         if (obj instanceof SvgElement) {
            styles.add(0, ((SvgElement) obj).style);
         }
         if (obj.parent == null)
            break;
         obj = obj.parent;
      }
      
      // Now apply the ancestor styles in reverse order to a fresh RendererState object
      RendererState  newState = new RendererState();
      for (Style style: styles)
         updateStyle(newState, style);
      
      return newState;
   }


   //==============================================================================
   // Gradients
   //==============================================================================


   /*
    * Check for gradiant fills or strokes on this object.  These are always relative
    * to the object, so can't be preconfigured. They have to be initialised at the
    * time each object is rendered.
    */
   private void  checkForGradiants(SvgElement obj)
   {
      if (state.style.fill instanceof PaintReference) {
         decodePaintReference(true, obj, (PaintReference) state.style.fill);
      }
      if (state.style.stroke instanceof PaintReference) {
         decodePaintReference(false, obj, (PaintReference) state.style.stroke);
      }
   }


   /*
    * Takes a PaintReference object and generates an appropriate Android Shader object from it.
    */
   private void  decodePaintReference(boolean isFill, SvgElement obj, PaintReference paintref)
   {
      SVG.SvgObject  ref = obj.document.resolveIRI(paintref.href);
      if (ref == null)
         return;
      if (ref instanceof SvgLinearGradient)
         makeLinearGradiant(isFill, obj, (SvgLinearGradient) ref);
      if (ref instanceof SvgRadialGradient)
         makeRadialGradiant(isFill, obj, (SvgRadialGradient) ref);
   }


   private void  makeLinearGradiant(boolean isFill, SvgElement obj, SvgLinearGradient gradient)
   {
      if (gradient.href != null)
         fillInChainedGradientFields(gradient, gradient.href);

      boolean  userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
      Paint    paint = isFill ? state.fillPaint : state.strokePaint;

      // Temporarily set viewBox to (0,0,1,1) for user units calculation (so percentages work)
      if (!userUnits)
         state.viewBox = UNIT_BBOX;
      float  _x1 = (gradient.x1 != null) ? gradient.x1.floatValueX(this): 0f;
      float  _y1 = (gradient.y1 != null) ? gradient.y1.floatValueY(this): 0f;
      float  _x2 = (gradient.x2 != null) ? gradient.x2.floatValueX(this): 1f;
      float  _y2 = (gradient.y2 != null) ? gradient.y2.floatValueY(this): 0f;

      // Calculate the gradient transform matrix
      Matrix m = new Matrix();
      if (!userUnits)
      {
         m.preTranslate(obj.boundingBox.minX, obj.boundingBox.minY);
         m.preScale(obj.boundingBox.width, obj.boundingBox.height);
      }
      if (gradient.gradientTransform != null)
      {
         m.preConcat(gradient.gradientTransform);
      }

      // Create the colour and position arrays for the shader
      int    numStops = gradient.children.size();
      if (numStops == 0) {
         // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
         if (isFill)
            state.hasFill = false;
         else
            state.hasStroke = false;
         return;
      }

      int[]  colours = new int[numStops];
      float[]  positions = new float[numStops];
      int  i = 0;
      float  lastOffset = -1;
      for (SvgObject child: gradient.children)
      {
         Stop  stop = (Stop) child;
         if (i == 0 || stop.offset >= lastOffset) {
            positions[i] = stop.offset;
            lastOffset = stop.offset;
         } else {
            // Each offset must be equal or grater than the last one.
            // If it doesn't we need to replace it with the previous value.
            positions[i] = lastOffset;
         }
         Colour col = (SVG.Colour) stop.style.stopColor;
         if (col == null)
            col = Colour.BLACK;
         float opacity = (stop.style.stopOpacity != null) ? stop.style.stopOpacity : 1f;
         colours[i] = clamp255(opacity * state.style.opacity) << 24 | col.colour;
         i++;
      }

      // If gradient vector is zero length, we instead fill with last stop colour
      if ((_x1 == _x2 && _y1 == _y2) || numStops == 1) {
         paint.setColor(colours[numStops - 1]);
         return;
      }

      // Convert spreadMethod->TileMode
      TileMode  tileMode = TileMode.CLAMP;
      if (gradient.spreadMethod != null)
      {
         if (gradient.spreadMethod == GradientSpread.reflect)
            tileMode = TileMode.MIRROR;
         else if (gradient.spreadMethod == GradientSpread.repeat)
            tileMode = TileMode.REPEAT;
      }

      // Create shader instance
      LinearGradient  gr = new LinearGradient(_x1, _y1, _x2, _y2, colours, positions, tileMode); 
      gr.setLocalMatrix(m);
      paint.setShader(gr);
   }


   private void makeRadialGradiant(boolean isFill, SvgElement obj, SvgRadialGradient gradient)
   {
      if (gradient.href != null)
         fillInChainedGradientFields(gradient, gradient.href);

      boolean  userUnits = (gradient.gradientUnitsAreUser != null && gradient.gradientUnitsAreUser);
      Paint    paint = isFill ? state.fillPaint : state.strokePaint;

      statePush();
      statePop();
      // Temporarily set viewBox to (0,0,1,1) for user units calculation (so percentages work)
      if (!userUnits)
         state.viewBox = UNIT_BBOX;
      float  _cx = (gradient.cx != null) ? gradient.cx.floatValueX(this): 0.5f;
      float  _cy = (gradient.cy != null) ? gradient.cy.floatValueY(this): 0.5f;
      float  _r = (gradient.r != null) ? gradient.r.floatValue(this): 0.5f;
      // fx and fy are ignored because Android RadialGradient doesn't support a
      // 'focus' point that is different from cx,cy.

      // Calculate the gradient transform matrix
      Matrix m = new Matrix();
      if (!userUnits)
      {
         m.preTranslate(obj.boundingBox.minX, obj.boundingBox.minY);
         m.preScale(obj.boundingBox.width, obj.boundingBox.height);
      }
      if (gradient.gradientTransform != null)
      {
         m.preConcat(gradient.gradientTransform);
      }

      // Create the colour and position arrays for the shader
      int    numStops = gradient.children.size();
      if (numStops == 0) {
         // If there are no stops defined, we are to treat it as paint = 'none' (see spec 13.2.4)
         if (isFill)
            state.hasFill = false;
         else
            state.hasStroke = false;
         return;
      }

      int[]  colours = new int[numStops];
      float[]  positions = new float[numStops];
      int  i = 0;
      for (SvgObject child: gradient.children)
      {
         Stop  stop = (Stop) child;
         positions[i] = stop.offset;
         Colour col = (SVG.Colour) stop.style.stopColor;
         if (col == null)
            col = Colour.BLACK;
         float opacity = (stop.style.stopOpacity != null) ? stop.style.stopOpacity : 1f;
         colours[i] = clamp255(opacity * state.style.opacity) << 24 | col.colour;
         i++;
      }

      // If gradient radius is zero, we instead fill with last stop colour
      if (_r == 0 || numStops == 1) {
         paint.setColor(colours[numStops - 1]);
         return;
      }

      // Convert spreadMethod->TileMode
      TileMode  tileMode = TileMode.CLAMP;
      if (gradient.spreadMethod != null)
      {
         if (gradient.spreadMethod == GradientSpread.reflect)
            tileMode = TileMode.MIRROR;
         else if (gradient.spreadMethod == GradientSpread.repeat)
            tileMode = TileMode.REPEAT;
      }

      // Create shader instance
      RadialGradient  gr = new RadialGradient(_cx, _cy, _r, colours, positions, tileMode); 
      gr.setLocalMatrix(m);
      paint.setShader(gr);
   }


   /*
    * Any unspecified fields in this gradient can be 'borrowed' from another
    * gradient specified by the href attribute.
    */
   private void fillInChainedGradientFields(GradientElement gradient, String href)
   {
      // Locate the referenced object
      SVG.SvgObject  ref = gradient.document.resolveIRI(href);
      if (ref == null) {
         // Non-existent - return silently and hope we have enough info to render the gradient
         return;
      }
      if (!(ref instanceof GradientElement)) {
         Log.e(TAG, "Gradient href attributes must point to other gradient elements");
         return;
      }
      if (ref == gradient) {
         Log.e(TAG, "Circular reference in gradient href attribute: "+href);
         return;
      }

      GradientElement  grRef = (GradientElement) ref;

      if (gradient.gradientUnitsAreUser == null)
         gradient.gradientUnitsAreUser = grRef.gradientUnitsAreUser;
      if (gradient.gradientTransform == null)
         gradient.gradientTransform = grRef.gradientTransform;
      if (gradient.spreadMethod == null)
         gradient.spreadMethod = grRef.spreadMethod;
      if (gradient.children.isEmpty())
         gradient.children = grRef.children;

      try
      {
         if (gradient instanceof SvgLinearGradient) {
            fillInChainedGradientFields((SvgLinearGradient) gradient, (SvgLinearGradient) ref);
         } else {
            fillInChainedGradientFields((SvgRadialGradient) gradient, (SvgRadialGradient) ref);
         }
      }
      catch (ClassCastException e) { /* expected - do nothing */ }

      if (grRef.href != null)
         fillInChainedGradientFields(gradient, grRef.href);
   }


   private void fillInChainedGradientFields(SvgLinearGradient gradient, SvgLinearGradient grRef)
   {
      if (gradient.x1 == null)
         gradient.x1 = grRef.x1;
      if (gradient.y1 == null)
         gradient.y1 = grRef.y1;
      if (gradient.x2 == null)
         gradient.x2 = grRef.x2;
      if (gradient.y2 == null)
         gradient.y2 = grRef.y2;
   }


   private void fillInChainedGradientFields(SvgRadialGradient gradient, SvgRadialGradient grRef)
   {
      if (gradient.cx == null)
         gradient.cx = grRef.cx;
      if (gradient.cy == null)
         gradient.cy = grRef.cy;
      if (gradient.r == null)
         gradient.r = grRef.r;
      if (gradient.fx == null)
         gradient.fx = grRef.fx;
      if (gradient.fy == null)
         gradient.fy = grRef.fy;
   }


   //==============================================================================
   // Clip paths
   //==============================================================================


   private void  checkForClipPath(SvgElement obj)
   {
      checkForClipPath(obj, obj.boundingBox);
   }


   private void  checkForClipPath(SvgElement obj, Box boundingBox)
   {
      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(state.style.clipPath);
      if (ref == null)
         return;

      ClipPath  clipPath = (ClipPath) ref;

      // An empty clipping path will completely clip away the element (sect 14.3.5).
      if (clipPath.children.isEmpty()) {
         canvas.clipRect(0, 0, 0, 0);
         return;
      }

      boolean  userUnits = (clipPath.clipPathUnitsAreUser == null || clipPath.clipPathUnitsAreUser);

      if ((obj instanceof SVG.Group) && !userUnits) {
         Log.w(TAG, "<clipPath clipPathUnits=\"objectBoundingBox\"> is not supported when referenced from container elements (like "+obj.getClass().getSimpleName()+")");
         return;
      }

      clipStatePush();

      if (!userUnits)
      {
         Matrix m = new Matrix();
         m.preTranslate(boundingBox.minX, boundingBox.minY);
         m.preScale(boundingBox.width, boundingBox.height);
         canvas.concat(m);
      }
      if (clipPath.transform != null)
      {
         canvas.concat(clipPath.transform);
      }

      // "Properties inherit into the <clipPath> element from its ancestors; properties do not
      // inherit from the element referencing the <clipPath> element." (sect 14.3.5)
      state = findInheritFromAncestorState(clipPath);

      checkForClipPath(clipPath);

      boolean  firstClip = true;
      for (SvgObject child: clipPath.children)
      {
         addObjectToClip(child, true, firstClip);
         firstClip = false;
      }

      clipStatePop();
   }


   private void addObjectToClip(SvgObject obj, boolean allowUse, boolean firstClip)
   {
/**/Log.w(TAG, "aOTC 0");
      if (!display(obj))
         return;

      // Save state
      clipStatePush();

      if (obj instanceof SVG.Use) {
         if (allowUse) {
            addObjectToClip((SVG.Use) obj, firstClip);
         } else {
            Log.e(TAG, "<use> elements inside a <clipPath> cannot reference another <use>");
         }
      } else if (obj instanceof SVG.Path) {
         addObjectToClip((SVG.Path) obj, firstClip);
      } else if (obj instanceof SVG.Text) {
         Log.w(TAG, "Text elements are not supported as a component for clipPaths");
      } else if (obj instanceof SVG.GraphicsElement) {
         addObjectToClip((SVG.GraphicsElement) obj, firstClip);
      } else {
         Log.e(TAG, "Invalid element found in clipPath definition: "+obj.getClass().getSimpleName());
      }

      // Restore state
      clipStatePop();
   }


   private void  clipStatePush()
   {
      // Save matrix and clip
      canvas.save(Canvas.MATRIX_SAVE_FLAG);
      // Save style state
      stateStack.push(state);
      state = (RendererState) state.clone();
   }


   private void  clipStatePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
   }


   private Path.FillType  getClipRuleFromState()
   {
      if (state.style.clipRule == null)
         return Path.FillType.WINDING;
      switch (state.style.clipRule)
      {
         case EvenOdd:
            return Path.FillType.EVEN_ODD;
         case NonZero:
         default:
            return Path.FillType.WINDING;
      }
   }


   private void addObjectToClip(SVG.Path obj, boolean firstClip)
   {
Log.w(TAG, "addPath");

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path = (new PathConverter(obj.path)).getPath();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }
      checkForClipPath(obj);
      
      path.setFillType(getClipRuleFromState());
      canvas.clipPath(path, firstClip ? Op.INTERSECT : Op.UNION);
   }


   private void addObjectToClip(SVG.Use obj, boolean firstClip)
   {
Log.w(TAG, "addUse");

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      // Locate the referenced object
      SVG.SvgObject  ref = obj.document.resolveIRI(obj.href);
      if (ref == null)
         return;

      checkForClipPath(obj);
      
      addObjectToClip(ref, false, firstClip);
   }


   private void addObjectToClip(SVG.GraphicsElement obj, boolean firstClip)
   {
Log.w(TAG, "addGE");

      updateStyle(state, obj.style);

      if (!visible())
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      Path  path;
      if (obj instanceof SVG.Rect)
         path = makePathAndBoundingBox((SVG.Rect) obj);
      else if (obj instanceof SVG.Circle)
         path = makePathAndBoundingBox((SVG.Circle) obj);
      else if (obj instanceof SVG.Ellipse)
         path = makePathAndBoundingBox((SVG.Ellipse) obj);
      else if (obj instanceof SVG.PolyLine)
         path = makePathAndBoundingBox((SVG.PolyLine) obj);
      else
         return;

      checkForClipPath(obj);
      
      canvas.clipPath(path, firstClip ? Op.INTERSECT : Op.UNION);
   }


   private Path  makePathAndBoundingBox(Rect obj)
   {
      float x, y, w, h, rx, ry;

      if (obj.rx == null && obj.ry == null) {
         rx = 0;
         ry = 0;
      } else if (obj.rx == null) {
         rx = ry = obj.ry.floatValueY(this);
      } else if (obj.ry == null) {
         rx = ry = obj.rx.floatValueX(this);
      } else {
         rx = obj.rx.floatValueX(this);
         ry = obj.ry.floatValueY(this);
      }
      rx = Math.min(rx, obj.width.floatValueX(this) / 2f);
      ry = Math.min(ry, obj.height.floatValueY(this) / 2f);
      x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
      y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
      w = obj.width.floatValueX(this);
      h = obj.height.floatValueY(this);

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(x, y, w, h);
      }

      float  right = x + w;
      float  bottom = y + h;

      Path  p = new Path();
      if (rx == 0 || ry == 0)
      {
         // Simple rect
         p.moveTo(x, y);
         p.lineTo(right, y);
         p.lineTo(right, bottom);
         p.lineTo(x, bottom);
         p.lineTo(x, y);
      }
      else
      {
         // Rounded rect
         
         // Bexier control point lengths for a 90 degress arc
         float  cpx = rx * BEZIER_ARC_FACTOR;
         float  cpy = ry * BEZIER_ARC_FACTOR;

         p.moveTo(x, y+ry);
         p.cubicTo(x, y+ry-cpy, x+rx-cpx, y, x+rx, y);
         p.lineTo(right-rx, y);
         p.cubicTo(right-rx+cpx, y, right, y+ry-cpy, right, y+ry);
         p.lineTo(right, bottom-ry);
         p.cubicTo(right, bottom-ry+cpy, right-rx+cpx, bottom, right-rx, bottom);
         p.lineTo(x+rx, bottom);
         p.cubicTo(x+rx-cpx, bottom, x, bottom-ry+cpy, x, bottom-ry);
         p.lineTo(x, y+ry);
      }
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.Circle obj)
   {
      float  cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      float  cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      float  r = obj.r.floatValue(this);

      float  left = cx - r;
      float  top = cy - r;
      float  right = cx + r;
      float  bottom = cy + r;

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(left, top, r*2, r*2);
      }

      float  cp = r * BEZIER_ARC_FACTOR;

      Path  p = new Path();
      p.moveTo(cx, top);
      p.cubicTo(cx+cp, top, right, cy-cp, right, cy);
      p.cubicTo(right, cy+cp, cx+cp, bottom, cx, bottom);
      p.cubicTo(cx-cp, bottom, left, cy+cp, left, cy);
      p.cubicTo(left, cy-cp, cx-cp, top, cx, top);
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.Ellipse obj)
   {
      float  cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      float  cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      float  rx = obj.rx.floatValueX(this);
      float  ry = obj.ry.floatValueY(this);

      float  left = cx - rx;
      float  top = cy - ry;
      float  right = cx + rx;
      float  bottom = cy + ry;

      if (obj.boundingBox == null) {
         obj.boundingBox = new Box(left, top, rx*2, ry*2);
      }

      float  cpx = rx * BEZIER_ARC_FACTOR;
      float  cpy = ry * BEZIER_ARC_FACTOR;

      Path  p = new Path();
      p.moveTo(cx, top);
      p.cubicTo(cx+cpx, top, right, cy-cpy, right, cy);
      p.cubicTo(right, cy+cpy, cx+cpx, bottom, cx, bottom);
      p.cubicTo(cx-cpx, bottom, left, cy+cpy, left, cy);
      p.cubicTo(left, cy-cpy, cx-cpx, top, cx, top);
      p.close();
      return p;
   }


   private Path makePathAndBoundingBox(SVG.PolyLine obj)
   {
      Path  path = new Path();

      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<obj.points.length; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }
      if (obj instanceof SVG.Polygon)
         path.close();

      if (obj.boundingBox == null) {
         obj.boundingBox = calculatePathBounds(path);
      }

      path.setFillType(getClipRuleFromState());
      return path;
   }


}
