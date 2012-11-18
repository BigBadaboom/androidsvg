package com.caverock.androidsvg;


import java.util.Stack;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.caverock.androidsvg.SVG.AspectRatioAlignment;
import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.Style;
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
   private RendererState  parentState;

   private Stack<RendererState> stateStack = new Stack<RendererState>();  // Keeps track of render state as we render


   private static class RendererState implements Cloneable
   {
      public Style    style;
      public boolean  hasFill;
      public boolean  hasStroke;
      public Paint    fillPaint;
      public Paint    strokePaint;
      public SVG.Box  viewPort;
      public SVG.Box  viewBox;

      public RendererState()
      {
      }

      @Override
      protected Object  clone()
      {
         RendererState obj;
         try
         {
            obj = (RendererState) super.clone();
            obj.fillPaint = new Paint(fillPaint);
            obj.strokePaint = new Paint(strokePaint);
            return obj;
         }
         catch (CloneNotSupportedException e)
         {
            throw new InternalError(e.toString());
         }
      }

   }



   public SVGAndroidRenderer(Canvas canvas, SVG.Box viewPort, float dpi)
   {
      this.canvas = canvas;
      this.dpi = dpi;

      state.viewPort = viewPort;

      state.fillPaint = new Paint();
      state.fillPaint.setStyle(Paint.Style.FILL);
      state.fillPaint.setTypeface(Typeface.DEFAULT);

      state.strokePaint = new Paint();
      state.strokePaint.setStyle(Paint.Style.STROKE);
      state.strokePaint.setTypeface(Typeface.DEFAULT);

      state.style = new Style();
      // Initialise the style state
      updateStyle(Style.getDefaultStyle());
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


   public void  render(SVG.SvgObject obj)
   {
      // Save state
      statePush();

      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if (obj instanceof SVG.Defs) {
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
      }

      // Restore state
      statePop();
   }


   // ==============================================================================


   private void  statePush()
   {
      // Save matrix and clip
      canvas.save();
      // Save style state
      parentState = statePeek();
      stateStack.push((RendererState) state.clone());
   }


   private void  statePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
      parentState = statePeek();
   }


   private RendererState  statePeek()
   {
      return stateStack.peek();
   }


   // ==============================================================================
   // Renderers for each element type


   public void render(SVG.Svg obj)
   {
/**/Log.d(TAG, "Svg render");

      if ((obj.width != null && obj.width.isZero()) ||
          (obj.height != null && obj.height.isZero()))
         return;

      updateStyle(obj.style);

      state.viewBox = obj.viewBox;

      // <svg> elements establish a new viewport.
      // But in the case of the root element, it has already been done for us.
      if (obj.parent != null)
      {
         float  _x = (obj.x != null) ? obj.x.floatValueX(this) : 0f;
         float  _y = (obj.y != null) ? obj.y.floatValueY(this) : 0f;
         float  _w = (obj.width != null) ? obj.width.floatValueX(this) : state.viewPort.width;
         float  _h = (obj.height != null) ? obj.height.floatValueX(this) : state.viewPort.height;
         state.viewPort = new SVG.Box(_x, _y, _w, _h);
      }
      if (!state.style.overflow) {
         canvas.clipRect(state.viewPort.minX, state.viewPort.minY, state.viewPort.width, state.viewPort.height);  //TODO only do clipRect if overflow property says so
      }

      if (obj.viewBox != null) {
         if (obj.preserveAspectRatioAlignment != null) {
            canvas.concat(calculateViewBoxTransform(obj.viewBox, obj.preserveAspectRatioAlignment, obj.preserveAspectRatioSlice));
         } else {
            canvas.concat(calculateViewBoxTransform(obj.viewBox));
         }
      }
      
      for (SVG.SvgObject child: obj.children) {
         render(child);
      }
   }


   public void render(SVG.Group obj)
   {
/**/Log.d(TAG, "Group render");
      updateStyle(obj.style);

      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      updateStyle(obj.style);

      for (SVG.SvgObject child: obj.children) {
         render(child);
      }
   }


   public void render(SVG.Use obj)
   {
/**/Log.d(TAG, "Use render");

      updateStyle(obj.style);

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

      render(ref);
   }


   public void render(SVG.Path obj)
   {
/**/Log.d(TAG, "Path render");

      updateStyle(obj.style);

      if (!state.hasStroke && !state.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      if (state.hasFill)
         canvas.drawPath(obj.path, state.fillPaint);
      if (state.hasStroke)
         canvas.drawPath(obj.path, state.strokePaint);
   }


   public void render(SVG.Rect obj)
   {
/**/Log.d(TAG, "Rect render");
      if (obj.width == null || obj.height == null || obj.width.isZero() || obj.height.isZero())
         return;

      updateStyle(obj.style);

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _x, _y, _rx, _ry;
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

      if (state.hasFill)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + obj.width.floatValueX(this), _y + obj.height.floatValueY(this), state.fillPaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValueX(this), _y + obj.height.floatValueY(this)), _rx, _ry, state.fillPaint);
         }
      }
      if (state.hasStroke)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + obj.width.floatValueX(this), _y + obj.height.floatValueY(this), state.strokePaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValueX(this), _y + obj.height.floatValueY(this)), _rx, _ry, state.strokePaint);
         }
      }

   }


   public void render(SVG.Circle obj)
   {
/**/Log.d(TAG, "Circle render");
      if (obj.r == null || obj.r.isZero())
         return;

      updateStyle(obj.style);

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _r;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _r = obj.r.floatValue(this);

      if (state.hasFill) {
         canvas.drawCircle(_cx, _cy, _r, state.fillPaint);
      }
      if (state.hasStroke) {
         canvas.drawCircle(_cx, _cy, _r, state.strokePaint);
      }

   }


   public void render(SVG.Ellipse obj)
   {
/**/Log.d(TAG, "Ellipse render");
      if (obj.rx == null || obj.ry == null || obj.rx.isZero() || obj.ry.isZero())
         return;

      updateStyle(obj.style);

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _rx, _ry;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _rx = obj.rx.floatValueX(this);
      _ry = obj.ry.floatValueY(this);
      RectF oval = new RectF(_cx-_rx, _cy-_ry, _cx+_rx, _cy+_ry);

      if (state.hasFill) {
         canvas.drawOval(oval, state.fillPaint);
      }
      if (state.hasStroke) {
         canvas.drawOval(oval, state.strokePaint);
      }

   }


   public void render(SVG.Line obj)
   {
/**/Log.d(TAG, "Line render");

      updateStyle(obj.style);

/**/Log.d(TAG, "LR hasStroke "+state.hasStroke+" "+obj.style.specifiedFlags);
      if (!state.hasStroke)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _x1, _y1, _x2, _y2;
      _x1 = (obj.x1 != null) ? obj.x1.floatValueX(this) : 0f;
      _y1 = (obj.y1 != null) ? obj.y1.floatValueY(this) : 0f;
      _x2 = (obj.x2 != null) ? obj.x2.floatValueX(this) : 0f;
      _y2 = (obj.y2 != null) ? obj.y2.floatValueY(this) : 0f;

      canvas.drawLine(_x1, _y1, _x2, _y2, state.strokePaint);
   }


   public void render(SVG.PolyLine obj)
   {
/**/Log.d(TAG, "PolyLine render");

      updateStyle(obj.style);

      if (!state.hasStroke)
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
      canvas.drawPath(path, state.strokePaint);
   }


   public void render(SVG.Polygon obj)
   {
/**/Log.d(TAG, "Polygon render");

      updateStyle(obj.style);

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

      if (state.hasFill)
         canvas.drawPath(path, state.fillPaint);
      if (state.hasStroke)
         canvas.drawPath(path, state.strokePaint);
   }


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


   public void render(SVG.Text obj)
   {
/**/Log.d(TAG, "Text render");

      updateStyle(obj.style);

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

      for (SVG.SvgObject child: obj.children) {
         renderText(child, currentTextPosition);
      }

   }


   public void  renderText(SVG.SvgObject obj, TextRenderContext currentTextPosition)
   {
      if (obj instanceof SVG.TSpan)
      {
/**/Log.d(TAG, "TSpan render");
         // Save state
         statePush();

         SVG.TSpan tspan = (SVG.TSpan) obj; 

         updateStyle(tspan.style);

         for (SVG.SvgObject child: tspan.children) {
            renderText(child, currentTextPosition);
         }

         // Restore state
         statePop();
      }
      else if  (obj instanceof SVG.TextSequence)
      {
/**/Log.d(TAG, "TextSequence render");
         drawText(((SVG.TextSequence) obj).text, currentTextPosition);
      }
      else if  (obj instanceof SVG.TRef)
      {
         // TODO
      }
   }


   private void drawText(String ts, TextRenderContext currentTextPosition)
   {
      if (state.hasFill)
         canvas.drawText(ts, currentTextPosition.x, currentTextPosition.y, state.fillPaint);
      if (state.hasStroke)
         canvas.drawText(ts, currentTextPosition.x, currentTextPosition.y, state.strokePaint);

      // Update the current text position
      currentTextPosition.x += state.fillPaint.measureText(ts);
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
         if (child instanceof TextContainer) {
            runningTotal = sumChildWidths((TextContainer) child, runningTotal);
         } else if (child instanceof TextSequence) {
            runningTotal += state.fillPaint.measureText(((TextSequence) child).text);
         }
      }
      return runningTotal;
   }
 

   // ==============================================================================


   /*
    * Calculate the transform required to fit the supplied viewBox into the current viewPort.
    * See spec section 7.8 for an explanation of how this works.
    * 
    * aspectRatioRule determines where the graphic is placed in the viewPort when aspect ration
    *    is kept.  xMin means left justified, xMid is centred, xMax is right justified etc.
    * slice determines whether we see the whole image or not. True fill the whole viewport.
    *    If slice is false, the image will be "letter-boxed". 
    */
   private Matrix calculateViewBoxTransform(Box viewBox, AspectRatioAlignment aspectRule, boolean slice)
   {
      Matrix m = new Matrix();

      float  xScale = state.viewPort.width / viewBox.width;
      float  yScale = state.viewPort.height / viewBox.height;
      float  xOffset = -viewBox.minX;
      float  yOffset = -viewBox.minY;

      // 'none' means scale both dimensions to fit the viewport
      if (aspectRule == AspectRatioAlignment.none)
      {
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
      m.preScale(aspectScale, aspectScale);
      m.preTranslate(xOffset, yOffset);
      return m;
   }

   private Matrix calculateViewBoxTransform(Box viewBox)
   {
      return calculateViewBoxTransform(viewBox, AspectRatioAlignment.xMidYMid, false);
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
   private void updateStyle(Style style)
   {
      // Some style attributes don't inherit, so first, lets reset those
      state.style.resetNonInheritingProperties();

      // Now update each style property we know about
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
      if (isSpecified(style, SVG.SPECIFIED_FILL | SVG.SPECIFIED_FILL_OPACITY))
      {
         if (style.fill instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.fill).colour;
            col = clamp(state.style.fillOpacity) << 24 | col;
            state.fillPaint.setColor(col);
         }
      }

      if (isSpecified(style, SVG.SPECIFIED_FILL_RULE))
      {
         // Not supported by Android? It always uses a non-zero winding rule.
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

      if (isSpecified(style, SVG.SPECIFIED_STROKE | SVG.SPECIFIED_STROKE_OPACITY))
      {
         if (style.stroke instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.stroke).colour;
            col = clamp(state.style.strokeOpacity) << 24 | col;
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

      if (isSpecified(style, SVG.SPECIFIED_OPACITY))
      {
         state.style.opacity = style.opacity;
         // NYI
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
         //state.strokePaint.setStrikeThruText(style.textDecoration.equals("line-through"));  // Bug in Android (39511) - can't stroke an underline
         state.fillPaint.setUnderlineText(style.textDecoration.equals("underline"));
         //state.strokePaint.setUnderlineText(style.textDecoration.equals("underline"));
      }

      if (isSpecified(style, SVG.SPECIFIED_TEXT_ANCHOR))
      {
         state.style.textAnchor = style.textAnchor;
      }

      if (isSpecified(style, SVG.SPECIFIED_OVERFLOW))
      {
         state.style.overflow = style.overflow;
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


   private int  clamp(float val)
   {
      int  i = (int)(val * 256f);
      return (i<0) ? 0 : (i>255) ? 255 : i;
   }

}
