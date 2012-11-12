package com.caverock.androidsvg;


import java.util.Iterator;
import java.util.Stack;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.caverock.androidsvg.SVG.AspectRatioRule;
import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.Style;


public class SVGAndroidRenderer
{
   private static final String  TAG = "SVGAndroidRenderer";

   private Canvas  canvas;
   private float   dpi = 90;    // dots per inch. Needed for accurate conversion of length values that have real world units, such as "cm".

   // Renderer state
   private RendererState  state = new RendererState();

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
      updatePaintsFromStyle(Style.getDefaultStyle());
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


   public SVG.Box getCurrentViewBox()
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
      stateStack.push((RendererState) state.clone());
   }


   private void  statePop()
   {
      // Restore matrix and clip
      canvas.restore();
      // Restore style state
      state = stateStack.pop();
   }


   // ==============================================================================
   // Renderers for each element type


   public void render(SVG.Svg obj)
   {
/**/Log.d(TAG, "Svg render");

      if (obj.viewBox != null) {
         canvas.concat(calculateViewBoxTransform(obj.viewBox));
      }
      
      for (SVG.SvgObject child: obj.children) {
         render(child);
      }
   }


   public void render(SVG.Group obj)
   {
/**/Log.d(TAG, "Group render");
      if (obj.transform != null) {
         canvas.concat(obj.transform);
      }

      updatePaintsFromStyle(obj.style);

      for (SVG.SvgObject child: obj.children) {
         render(child);
      }
   }


   public void render(SVG.Use obj)
   {
/**/Log.d(TAG, "Use render");

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

      updatePaintsFromStyle(obj.style);

      render(ref);
   }


   public void render(SVG.Path obj)
   {
/**/Log.d(TAG, "Path render");

      updatePaintsFromStyle(obj.style);

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

      updatePaintsFromStyle(obj.style);

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

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _r;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _r = obj.r.floatValue(this);

      updatePaintsFromStyle(obj.style);

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

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _cx, _cy, _rx, _ry;
      _cx = (obj.cx != null) ? obj.cx.floatValueX(this) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValueY(this) : 0f;
      _rx = obj.rx.floatValueX(this);
      _ry = obj.ry.floatValueY(this);
      RectF oval = new RectF(_cx-_rx, _cy-_ry, _cx+_rx, _cy+_ry);

      updatePaintsFromStyle(obj.style);

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

      updatePaintsFromStyle(obj.style);

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

      updatePaintsFromStyle(obj.style);

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

      updatePaintsFromStyle(obj.style);

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

      if (obj.transform != null)
         canvas.concat(obj.transform);

      updatePaintsFromStyle(obj.style);

      // Get the first coordinate pair from the lists in the x and y properties.
      float  x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.get(0).floatValueX(this);
      float  y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.get(0).floatValueY(this);
      TextRenderContext currentTextPosition = new TextRenderContext(x, y);

      boolean isFirstNode = true;
      for (Iterator<SVG.SvgObject> iterator = obj.children.iterator(); iterator.hasNext(); isFirstNode = false)
      {
         SVG.SvgObject child = iterator.next();
         renderText(child, obj, currentTextPosition, isFirstNode, !iterator.hasNext());
      }

   }


   public void  renderText(SVG.SvgObject obj, SVG.SvgElement parent, TextRenderContext currentTextPosition, boolean isFirstNode, boolean isLastNode)
   {
      if (obj instanceof SVG.TSpan)
      {
/**/Log.d(TAG, "TSpan render");
         // Save state
         statePush();

         SVG.TSpan tspan = (SVG.TSpan) obj; 

         updatePaintsFromStyle(tspan.style);

         isFirstNode = true;
         for (Iterator<SVG.SvgObject> iterator = tspan.children.iterator(); iterator.hasNext(); isFirstNode=false) {
            SVG.SvgObject child = iterator.next();
            renderText(child, tspan, currentTextPosition, isFirstNode, !iterator.hasNext());
         }

         // Restore state
         statePop();
      }
      else if  (obj instanceof SVG.TextSequence)
      {
/**/Log.d(TAG, "TextSequence render");
         String ts = ((SVG.TextSequence) obj).text;
         ts = trimmer(ts, isFirstNode, isLastNode);
         drawText(ts, currentTextPosition);
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


   // Trim whitespace chars appropriately depending on where the node is
   // relative to other text nodes.
   private String trimmer(String str, boolean isFirstNode, boolean isLastNode)
   {
      int len = str.length();
      int offset = 0;

      if (len == 0)
         return str;

      char[] chars = str.toCharArray();

      while (offset < len && chars[offset] <= ' ') {
         offset++;
      }
      len -= offset;
      while (len > 0 && chars[offset + len - 1] <= ' ') {
         len--;
      }

      // Allow one space at the start if this wasn't the first node
      if (len == 0)
      {
         if (!isFirstNode && !isLastNode)
         {
            chars[0] = ' ';
            offset = 0;
            len = 1;
         }
      }
      else
      {
         if (offset > 0 && !isFirstNode)
         {
            chars[--offset] = ' ';
            len++;
         }
         // Allow one space at the end if this wasn't the last node
         if ((offset + len) < str.length() && !isLastNode)
         {
            chars[offset + len] = ' ';
            len++;
         }
      }

      return new String(chars, offset, len);
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
   private Matrix calculateViewBoxTransform(Box viewBox, AspectRatioRule aspectRule, boolean slice)
   {
      Matrix m = new Matrix();

      float  xScale = state.viewPort.width / viewBox.width;
      float  yScale = state.viewPort.height / viewBox.height;
      float  xOffset = -viewBox.minX;
      float  yOffset = -viewBox.minY;

      // 'none' means scale both dimensions to fit the viewport
      if (aspectRule == AspectRatioRule.none)
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
/**/Log.d(TAG, "calculateViewBoxTransform "+aspectScale+" "+xOffset+" "+yOffset);
      m.preScale(aspectScale, aspectScale);
      m.preTranslate(xOffset, yOffset);
      return m;
   }

   private Matrix calculateViewBoxTransform(Box viewBox)
   {
      return calculateViewBoxTransform(viewBox, AspectRatioRule.xMidYMid, false);
   }


   private void updatePaintsFromStyle(Style style)
   {
      if ((style.specifiedFlags & SVG.SPECIFIED_FILL) != 0)
      {
         state.style.fill = style.fill;
         state.hasFill = (style.fill != null);
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FILL_OPACITY) != 0)
      {
         state.style.fillOpacity = style.fillOpacity;
      }

      // If either fill or its opacity has changed, update the fillPaint
      if ((style.specifiedFlags & SVG.SPECIFIED_FILL) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_FILL_OPACITY) != 0)
      {
         if (style.fill instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.fill).colour;
            col = clamp(state.style.fillOpacity) << 24 | col;
            state.fillPaint.setColor(col);
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FILL_RULE) != 0)
      {
         // Not supported by Android? It always uses a non-zero winding rule.
      }


      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE) != 0)
      {
         state.style.stroke = style.stroke;
         state.hasStroke = (style.stroke != null);
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_OPACITY) != 0)
      {
         state.style.strokeOpacity = style.strokeOpacity;
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_STROKE_OPACITY) != 0)
      {
         if (style.stroke instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.stroke).colour;
            col = clamp(state.style.strokeOpacity) << 24 | col;
            state.strokePaint.setColor(col);
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_WIDTH) != 0)
      {
         state.style.strokeWidth = style.strokeWidth;
         state.strokePaint.setStrokeWidth(style.strokeWidth.floatValue(this));
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_LINECAP) != 0)
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

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_LINEJOIN) != 0)
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

      if ((style.specifiedFlags & SVG.SPECIFIED_OPACITY) != 0)
      {
         state.style.opacity = style.opacity;
         // NYI
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_FAMILY) != 0)
      {
         state.style.fontFamily = style.fontFamily;
         // NYI
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_SIZE) != 0)
      {
         state.style.fontSize = style.fontSize;
         state.fillPaint.setTextSize(style.fontSize.floatValue(this));
         state.strokePaint.setTextSize(style.fontSize.floatValue(this));
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_WEIGHT) != 0)
      {
         state.style.fontWeight = style.fontWeight;
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_STYLE) != 0)
      {
         state.style.fontStyle = style.fontStyle;
      }

      // If weight or style has changed, update the typeface
      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_WEIGHT) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_FONT_STYLE) != 0)
      {
         Typeface  font = Typeface.create(Typeface.DEFAULT,  getTypefaceStyle(style));
         state.fillPaint.setTypeface(font);
         state.strokePaint.setTypeface(font);
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_TEXT_DECORATION) != 0)
      {
         state.style.textDecoration = style.textDecoration;
         state.fillPaint.setStrikeThruText(style.textDecoration.equals("line-through"));
         //state.strokePaint.setStrikeThruText(style.textDecoration.equals("line-through"));  // Bug in Android (39511) - can't stroke an underline
         state.fillPaint.setUnderlineText(style.textDecoration.equals("underline"));
         //state.strokePaint.setUnderlineText(style.textDecoration.equals("underline"));
      }

   }


   private int  getTypefaceStyle(Style style)
   {
      boolean  italic = "italic".equals(style.fontStyle);
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
