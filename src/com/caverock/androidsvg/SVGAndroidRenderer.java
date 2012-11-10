package com.caverock.androidsvg;

import java.util.Stack;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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

      strokePaint = new Paint();
      strokePaint.setStyle(Paint.Style.STROKE);
   }


   public void  render(SVG.SvgElement obj)
   {
      // Save matrix and clip
      canvas.save();
      // Save paint styles
      paintStack.push(fillPaint);
      paintStack.push(strokePaint);

      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if  (obj instanceof SVG.Group) {
         render((SVG.Group) obj);
      } else if  (obj instanceof SVG.Path) {
         render((SVG.Path) obj);
      } else if  (obj instanceof SVG.Rect) {
         render((SVG.Rect) obj);
      } else if  (obj instanceof SVG.Circle) {
         render((SVG.Circle) obj);
      } else if  (obj instanceof SVG.Ellipse) {
         render((SVG.Ellipse) obj);
      } else if  (obj instanceof SVG.Line) {
         render((SVG.Line) obj);
      } else if  (obj instanceof SVG.Polygon) {
         render((SVG.Polygon) obj);
      } else if  (obj instanceof SVG.PolyLine) {
         render((SVG.PolyLine) obj);
      }

      // Restore paint styles
      strokePaint = paintStack.pop();
      fillPaint = paintStack.pop();
      // Restore matrix and clip
      canvas.restore();
   }



   public void render(SVG.Svg obj)
   {
/**/Log.d(TAG, "Svg render");         
      for (SVG.SvgElement child: obj.children) {
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

      for (SVG.SvgElement child: obj.children) {
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

      if (obj.style.hasFill) {
         if (_rx == 0f || _ry == 0f) {
            canvas.drawRect(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi), fillPaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi)), _rx, _ry, fillPaint);
         }
      }
      if (obj.style.hasStroke) {
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


   //==============================================================================


   private void updatePaintsFromStyle(Style style)
   {
      if ((style.specifiedFlags & SVG.SPECIFIED_FILL) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_FILL_OPACITY) != 0)
      {
         if (style.fill instanceof SVG.Colour) {
            int col = ((SVG.Colour)style.fill).colour;
            col = clamp(style.fillOpacity) << 24 | col;
            fillPaint.setColor( col );
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE) != 0 ||
          (style.specifiedFlags & SVG.SPECIFIED_STROKE_OPACITY) != 0)
      {
         if (style.stroke instanceof SVG.Colour) {
            int col = ((SVG.Colour)style.stroke).colour;
            col = clamp(style.strokeOpacity) << 24 | col;
            strokePaint.setColor( col );
/**/Log.d(TAG, String.format("Setting stroke colour to %x",col));
         }
      }

      if ((style.specifiedFlags & SVG.SPECIFIED_STROKE_WIDTH) != 0)
      {
         strokePaint.setStrokeWidth(style.strokeWidth.floatValue(dpi));
      }

   }


   private int  clamp(float val)
   {
      int  i = (int)(val * 256f);
      return (i<0) ? 0 : (i>255) ? 255 : i;
   }



}
