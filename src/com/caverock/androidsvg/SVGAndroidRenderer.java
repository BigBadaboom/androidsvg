package com.caverock.androidsvg;


import java.util.Iterator;
import java.util.Stack;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.caverock.androidsvg.SVG.Style;


public class SVGAndroidRenderer
{
   private static final String  TAG = "SVGAndroidRenderer";

   private int     dpi = 90;    // dots per inch. Needed for accurate conversion of length values that have real world units, such as "cm".

   // Renderer state
   private Canvas  canvas;
   private Paint   fillPaint;
   private Paint   strokePaint;

   private Stack<Paint> paintStack = new Stack<Paint>();   // So we can save/restore style when dealing with nested objects


   public SVGAndroidRenderer(Canvas canvas)
   {
      this.canvas = canvas;

      fillPaint = new Paint();
      fillPaint.setStyle(Paint.Style.FILL);
      fillPaint.setTypeface(Typeface.DEFAULT);

      strokePaint = new Paint();
      strokePaint.setStyle(Paint.Style.STROKE);
      strokePaint.setTypeface(Typeface.DEFAULT);
   }


   public void  render(SVG.SvgObject obj)
   {
      // Save matrix and clip
      canvas.save();
      // Save paint styles
      paintsPush();

      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if (obj instanceof SVG.Group) {
         render((SVG.Group) obj);
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

      // Restore paint styles
      paintsPop();
      // Restore matrix and clip
      canvas.restore();
   }


   private void  paintsPush()
   {
      paintStack.push(new Paint(fillPaint));
      paintStack.push(new Paint(strokePaint));
   }


   private void  paintsPop()
   {
      strokePaint = paintStack.pop();
      fillPaint = paintStack.pop();
   }


   public void render(SVG.Svg obj)
   {
/**/Log.d(TAG, "Svg render");
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


   public void render(SVG.Path obj)
   {
/**/Log.d(TAG, "Path render");

      if (!obj.style.hasStroke && !obj.style.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      updatePaintsFromStyle(obj.style);

      if (obj.style.hasFill)
         canvas.drawPath(obj.path, fillPaint);
      if (obj.style.hasStroke)
         canvas.drawPath(obj.path, strokePaint);
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
         _rx = _ry = obj.ry.floatValue(dpi);
      } else if (obj.ry == null) {
         _rx = _ry = obj.rx.floatValue(dpi);
      } else {
         _rx = obj.rx.floatValue(dpi);
         _ry = obj.ry.floatValue(dpi);
      }
      _rx = Math.min(_rx, obj.width.floatValue(dpi) / 2f);
      _ry = Math.min(_ry, obj.height.floatValue(dpi) / 2f);
      _x = (obj.x != null) ? obj.x.floatValue(dpi) : 0f;
      _y = (obj.y != null) ? obj.y.floatValue(dpi) : 0f;

      updatePaintsFromStyle(obj.style);

      if (obj.style.hasFill)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi), fillPaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi)), _rx, _ry, fillPaint);
         }
      }
      if (obj.style.hasStroke)
      {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi), strokePaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi)), _rx, _ry, strokePaint);
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
      _cx = (obj.cx != null) ? obj.cx.floatValue(dpi) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValue(dpi) : 0f;
      _r = obj.r.floatValue(dpi);

      updatePaintsFromStyle(obj.style);

      if (obj.style.hasFill) {
         canvas.drawCircle(_cx, _cy, _r, fillPaint);
      }
      if (obj.style.hasStroke) {
         canvas.drawCircle(_cx, _cy, _r, strokePaint);
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
      _cx = (obj.cx != null) ? obj.cx.floatValue(dpi) : 0f;
      _cy = (obj.cy != null) ? obj.cy.floatValue(dpi) : 0f;
      _rx = obj.rx.floatValue(dpi);
      _ry = obj.ry.floatValue(dpi);
      RectF oval = new RectF(_cx-_rx, _cy-_ry, _cx+_rx, _cy+_ry);

      updatePaintsFromStyle(obj.style);

      if (obj.style.hasFill) {
         canvas.drawOval(oval, fillPaint);
      }
      if (obj.style.hasStroke) {
         canvas.drawOval(oval, strokePaint);
      }

   }


   public void render(SVG.Line obj)
   {
/**/Log.d(TAG, "Line render");

      if (!obj.style.hasStroke)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      float _x1, _y1, _x2, _y2;
      _x1 = (obj.x1 != null) ? obj.x1.floatValue(dpi) : 0f;
      _y1 = (obj.y1 != null) ? obj.y1.floatValue(dpi) : 0f;
      _x2 = (obj.x2 != null) ? obj.x2.floatValue(dpi) : 0f;
      _y2 = (obj.y2 != null) ? obj.y2.floatValue(dpi) : 0f;

      updatePaintsFromStyle(obj.style);

      canvas.drawLine(_x1, _y1, _x2, _y2, strokePaint);
   }


   public void render(SVG.PolyLine obj)
   {
/**/Log.d(TAG, "PolyLine render");

      if (!obj.style.hasStroke)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 4)
         return;

      updatePaintsFromStyle(obj.style);

      Path  path = new Path();
      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<numPoints; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }
      canvas.drawPath(path, strokePaint);
   }


   public void render(SVG.Polygon obj)
   {
/**/Log.d(TAG, "Polygon render");

      if (!obj.style.hasStroke && !obj.style.hasFill)
         return;

      if (obj.transform != null)
         canvas.concat(obj.transform);

      int  numPoints = obj.points.length;
      if (numPoints < 4)
         return;

      updatePaintsFromStyle(obj.style);

      Path  path = new Path();
      path.moveTo(obj.points[0], obj.points[1]);
      for (int i=2; i<numPoints; i+=2) {
         path.lineTo(obj.points[i], obj.points[i+1]);
      }
      path.close();

      if (obj.style.hasFill)
         canvas.drawPath(path, fillPaint);
      if (obj.style.hasStroke)
         canvas.drawPath(path, strokePaint);
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
      float  x = (obj.x == null || obj.x.size() == 0) ? 0f : obj.x.remove(0).floatValue(dpi);
      float  y = (obj.y == null || obj.y.size() == 0) ? 0f : obj.y.remove(0).floatValue(dpi);
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
         // Save matrix and clip
         canvas.save();
         // Save paint styles
         paintsPush();

         SVG.TSpan tspan = (SVG.TSpan) obj; 

         updatePaintsFromStyle(tspan.style);

         isFirstNode = true;
         for (Iterator<SVG.SvgObject> iterator = tspan.children.iterator(); iterator.hasNext(); isFirstNode=false) {
            SVG.SvgObject child = iterator.next();
            renderText(child, tspan, currentTextPosition, isFirstNode, !iterator.hasNext());
         }

         // Restore paint styles
         paintsPop();
         // Restore matrix and clip
         canvas.restore();
      }
      else if  (obj instanceof SVG.TextSequence)
      {
/**/Log.d(TAG, "TextSequence render");
         String ts = ((SVG.TextSequence) obj).text;
         ts = trimmer(ts, isFirstNode, isLastNode);
         drawText(ts, parent, currentTextPosition);
      }
      else if  (obj instanceof SVG.TRef)
      {
         // TODO
      }
   }


   private void drawText(String ts, SVG.SvgElement parentObj, TextRenderContext currentTextPosition)
   {
      if (parentObj.style.hasFill)
         canvas.drawText(ts, currentTextPosition.x, currentTextPosition.y, fillPaint);
      if (parentObj.style.hasStroke)
         canvas.drawText(ts, currentTextPosition.x, currentTextPosition.y, strokePaint);

      // Update the current text position
      currentTextPosition.x += fillPaint.measureText(ts);
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


   private void updatePaintsFromStyle(Style style)
   {
      if ((style.specifiedFlags & SVG.SPECIFIED_FILL) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_FILL_OPACITY) != 0)
      {
         if (style.fill instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.fill).colour;
            col = clamp(style.fillOpacity) << 24 | col;
            fillPaint.setColor(col);
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_STROKE_OPACITY) != 0)
      {
         if (style.stroke instanceof SVG.Colour)
         {
            int col = ((SVG.Colour) style.stroke).colour;
            col = clamp(style.strokeOpacity) << 24 | col;
            strokePaint.setColor(col);
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_WIDTH) != 0)
      {
         strokePaint.setStrokeWidth(style.strokeWidth.floatValue(dpi));
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_OPACITY) != 0)
      {
         // NYI
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_FAMILY) != 0)
      {
         // NYI
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_SIZE) != 0)
      {
         fillPaint.setTextSize(style.fontSize.floatValue(dpi));
         strokePaint.setTextSize(style.fontSize.floatValue(dpi));
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_FONT_WEIGHT) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_FONT_STYLE) != 0)
      {
         Typeface  font = Typeface.create(Typeface.DEFAULT,  getTypefaceStyle(style));
         fillPaint.setTypeface(font);
         strokePaint.setTypeface(font);
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_TEXT_DECORATION) != 0)
      {
         fillPaint.setStrikeThruText(style.textDecoration.equals("line-through"));
         //strokePaint.setStrikeThruText(style.textDecoration.equals("line-through"));  // Bug in Android (39511) - can't stroke an underline
         fillPaint.setUnderlineText(style.textDecoration.equals("underline"));
         //strokePaint.setUnderlineText(style.textDecoration.equals("underline"));
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
