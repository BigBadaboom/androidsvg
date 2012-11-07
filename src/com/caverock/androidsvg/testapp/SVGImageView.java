package com.caverock.androidsvg.testapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParser;


public class SVGImageView extends View
{
   private int      width = 0;
   private int      height = 0;
   private Bitmap   bm = null;
   private Paint    paint = new Paint();

   private SVG      svg = null;
   private Picture  picture = null;



   public SVGImageView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }


   public SVGImageView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
   }



   @Override
   protected void onDraw(Canvas canvas)
   {
/*
      RectF bounds = svg.getElementById("bounds").getBoundingBox();
      if (bounds == null) {
         bounds = svg.getLimits();
         if (bounds == null)
            throw new RuntimeException("SVG resource is missing a bounds or limits record");
      }
      // Scale SVG to fit bitmap
      float xscale = (float)width / (bounds.right - bounds.left);
      float yscale = (float)height / (bounds.bottom - bounds.top);
      // Shift to display in the right place. Bounds minx and miny may not be 0.
      float xbase = xscale * bounds.left;
      float ybase = yscale * bounds.top;
      // We render SVG into a bitmap, not directly to the View canvas
      if (this.bm == null) {
          this.bm = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
      }
      Canvas bmcanvas = new Canvas(this.bm);
//Log.d("foo", "xscale="+xscale+" yscale="+yscale);
      bmcanvas.scale(xscale, yscale);
//Log.d("foo", "xbase="+xbase+" ybase="+ybase);
//Log.d("onDraw", "limits = "+limits.left+","+limits.top+" "+limits.right+","+limits.bottom);
      // Render SVG
//Log.d("foo", "reject="+canvas.quickReject(bounds, EdgeType.AA));
      bmcanvas.drawPicture(picture);
      canvas.drawBitmap(bm, 0, 0, paint);
*/
      if (this.bm == null) {
          this.bm = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
      }
      Canvas bmcanvas = new Canvas(this.bm);
      bmcanvas.drawRGB(255, 255, 255);  // Clear bg to white
      if (this.picture != null) {
         bmcanvas.drawPicture(picture);
      }
      canvas.drawBitmap(bm, 0, 0, paint);
   }


   @Override
   protected void onLayout(boolean changed, int left, int top, int right, int bottom)
   {
      super.onLayout(changed, left, top, right, bottom);
      
      this.width = right - left;
      this.height = bottom - top;
/**/Log.d("onLayout", "width = "+width+" height="+height);
   }


   public void  setSVGAsset(String svgPath)
   {
/**/Log.d("setSVGAsset", svgPath);
      try
      {
         this.svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
         this.picture = this.svg.getPicture();
         invalidate();
      }
      catch (Exception e)
      {
         //Log.e("SVGImageView", e.getMessage());
         Log.e("SVGImageView", e.toString());
         e.printStackTrace();

         this.picture = null;
         invalidate();
      }
   }

   public void  setSVGResource(int resId)
   {
      try
      {
         this.svg = SVG.getFromResource(this.getContext(), resId);
         this.picture = this.svg.getPicture();
         invalidate();
      }
      catch (Exception e)
      {
         Log.e("SVGImageView", e.getMessage());
         e.printStackTrace();

         this.picture = null;
         invalidate();
      }
   }

}
