package com.caverock.androidsvg;

import com.caverock.androidsvg.SVG.Style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class SVGAndroidRenderer
{
   private static final String  TAG = "SVGAndroidRenderer";

   // Renderer state
   Canvas  canvas;
   Paint   fillPaint;
   Paint   strokePaint;
   int     dpi = 90;    // dots per inch. Needed for accurate conversion of length values that have real world units, such as "cm".


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
      if (obj instanceof SVG.Svg) {
         render((SVG.Svg) obj);
      } else if  (obj instanceof SVG.Rect) {
         render((SVG.Rect) obj);
      }
   }



   public void render(SVG.Svg obj)
   {
/**/Log.d(TAG, "Svg render");         
      for (SVG.SvgElement child: obj.children) {
         render(child);
      }
   }


   public void render(SVG.Rect obj)
   {
/**/Log.d(TAG, "Rect render");         
      if (obj.width == null || obj.height == null || obj.width.isZero() || obj.height.isZero())
         return;
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
      //pushTransform(this.transform);
      if (obj.style.hasFill) {
         setFillPaint(obj.style);
         if (_rx == 0f || _ry == 0f) {
/**/Log.d(TAG, "do Rect fill "+_x+" "+_y+" "+obj.width+" "+obj.height);         
            canvas.drawRect(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi), fillPaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi)), _rx, _ry, fillPaint);
         }
      }
      if (obj.style.hasStroke) {
         setStrokePaint(obj.style);
         if (_rx == 0f || _ry == 0f) {
/**/Log.d(TAG, "do Rect stroke "+_x+" "+_y+" "+obj.width+" "+obj.height);         
            canvas.drawRect(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi), strokePaint);
         } else {
            canvas.drawRoundRect(new RectF(_x, _y, _x + obj.width.floatValue(dpi), _y + obj.height.floatValue(dpi)), _rx, _ry, strokePaint);
         }
      }
      //popTransform();
   }


   private void setFillPaint(Style style)
   {
      if (style.fill instanceof SVG.Colour) {
         int col = ((SVG.Colour)style.fill).colour;
         col = clamp(style.fillOpacity) << 24 | col;
         fillPaint.setColor( col );
      }
      
   }


   private void setStrokePaint(Style style)
   {
      if (style.stroke instanceof SVG.Colour) {
         int col = ((SVG.Colour)style.stroke).colour;
         col = clamp(style.strokeOpacity) << 24 | col;
         strokePaint.setColor( col );
         strokePaint.setStrokeWidth(style.strokeWidth.floatValue(dpi));
      }
      
   }


   private int  clamp(float val)
   {
      int  i = (int)(val * 256f);
      return (i<0) ? 0 : (i>255) ? 255 : i;
   }


}
