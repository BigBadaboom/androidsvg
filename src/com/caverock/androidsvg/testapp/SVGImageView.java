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
   private int      width;
   private int      height;
   private SVG      svg = null;
   private Bitmap   bm = null;
   private Paint    paint = new Paint();


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
      canvas.drawBitmap(bm, 0, 0, paint);
   }


   @Override
   protected void onLayout(boolean changed, int left, int top, int right, int bottom)
   {
      super.onLayout(changed, left, top, right, bottom);
      
      if (this.svg == null)
         return;

      this.width = right - left;
      this.height = bottom - top;
/**/Log.d("onLayout", "width = "+width+" height="+height);
      rebuildBitmap();
   }


   private void rebuildBitmap()
   {
      if (this.width == 0)
         return;
      Bitmap  newBM = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
      Canvas  bmcanvas = new Canvas(newBM);
      bmcanvas.drawRGB(255, 255, 255);  // Clear bg to white
      if (this.svg != null) {
         svg.ensureRootViewBox();
         bmcanvas.drawPicture(this.svg.getPicture(width, height, getResources().getDisplayMetrics().xdpi));
         //bmcanvas.drawPicture(this.svg.getPicture(width, height, 96f));
      }
      this.bm = newBM;
   }


   public void  setSVGAsset(String svgPath)
   {
/**/Log.d("setSVGAsset", svgPath);
      try
      {
         this.svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
         rebuildBitmap();
         invalidate();
      }
      catch (Exception e)
      {
         //Log.e("SVGImageView", e.getMessage());
         Log.e("SVGImageView", e.toString());
         e.printStackTrace();

         this.svg = null;
         rebuildBitmap();
         invalidate();
      }
   }

   public void  setSVGResource(int resId)
   {
      try
      {
         this.svg = SVG.getFromResource(this.getContext(), resId);
         invalidate();
      }
      catch (Exception e)
      {
         Log.e("SVGImageView", e.getMessage());
         e.printStackTrace();

         this.svg = null;
         invalidate();
      }
   }

}
