package com.caverock.androidsvg.testapp;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVG.Style.FontStyle;
import com.caverock.androidsvg.SVGExternalFileResolver;


public class SVGImageView extends View
{
   private int      width;
   private int      height;
   private SVG      svg = null;
   private Bitmap   bm = null;
   private Paint    paint = new Paint();
   private Float    renderDPI = null;
   private String   viewId = null;
   private boolean  directRender = false;

   private static final String  TAG = SVGImageView.class.getSimpleName();


   private SVGExternalFileResolver  myResolver = new SVGExternalFileResolver() {
      
      @Override
      public Typeface resolveFont(String fontFamily, int fontWeight, FontStyle fontStyle)
      {
         Log.i(TAG, "resolveFont("+fontFamily+","+fontWeight+","+fontStyle+")");

         if (fontFamily.equals("Arial")) {
            return Typeface.createFromAsset(SVGImageView.this.getContext().getAssets(), "Arial.ttf");
         }
         if (fontFamily.equals("Bitter")) {
            return Typeface.createFromAsset(SVGImageView.this.getContext().getAssets(), "Bitter-Bold.ttf");
         }

         return null;
      }

      @Override
      public Bitmap resolveImage(String filename)
      {
         try
         {
            InputStream  istream = SVGImageView.this.getContext().getAssets().open(filename);
            return BitmapFactory.decodeStream(istream);
         }
         catch (IOException e1)
         {
            return null;
         }
      }

   };


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
      if (this.bm == null)
         return;
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
      rebuildBitmap();
   }


   private void rebuildBitmap()
   {
      if (this.width == 0)
         return;
      Bitmap  newBM = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);

      Canvas  bmcanvas = new Canvas(newBM);
      bmcanvas.drawRGB(255, 255, 255);  // Clear bg to white

      if (this.svg != null)
      {
         if (this.viewId != null)
         {
            if (this.renderDPI == null) {
               bmcanvas.drawPicture(this.svg.getPictureForView(this.viewId, width, height, getResources().getDisplayMetrics().xdpi));
            } else {
               bmcanvas.drawPicture(this.svg.getPictureForView(this.viewId, width, height, this.renderDPI));
            }
         }
         else if (this.directRender)
         {
            if (this.renderDPI == null) {
               this.svg.renderToCanvas(bmcanvas, null, getResources().getDisplayMetrics().xdpi, SVG.AspectRatioAlignment.xMidYMid, true);
            } else {
               this.svg.renderToCanvas(bmcanvas, null, this.renderDPI, SVG.AspectRatioAlignment.xMidYMid, true);
            }
         }
         else
         {
            svg.ensureRootViewBox();
            if (this.renderDPI == null) {
               bmcanvas.drawPicture(this.svg.getPicture(width, height, getResources().getDisplayMetrics().xdpi, SVG.AspectRatioAlignment.xMidYMid, true));
            } else {
               bmcanvas.drawPicture(this.svg.getPicture(width, height, this.renderDPI, SVG.AspectRatioAlignment.xMidYMid, true));
            }
         }
      }
      this.bm = newBM;
   }


   public void  setSVGAsset(String svgRef)
   {
/**/Log.d("setSVGAsset", svgRef);
      String svgPath = null;

      // Flag to indicate direct render mode
      if (svgRef.startsWith("@")) {
         svgRef = svgRef.substring(1);
         this.directRender = true;
      } else {
         this.directRender = false;
      }
      
      int hash = svgRef.indexOf('#');
      if (hash == -1) {
         svgPath = svgRef;
         this.viewId = null;
      } else {
         svgPath = svgRef.substring(0, hash);
         this.viewId = svgRef.substring(hash+1);
      }

      try
      {
         this.svg = null;
         this.svg = SVG.getFromAsset(getContext().getAssets(), svgPath);
         this.svg.registerExternalFileResolver(myResolver);
         rebuildBitmap();
         invalidate();
      }
      catch (Exception e)
      {
         //Log.e("SVGImageView", e.getMessage());
         Log.e("SVGImageView", e.toString());
         e.printStackTrace();

         // Some of the document (up till the error) may have been created.
         rebuildBitmap();
         invalidate();
      }
   }

   public void  setSVGResource(int resId)
   {
      try
      {
         this.svg = SVG.getFromResource(this.getContext(), resId);
         this.svg.registerExternalFileResolver(myResolver);
         rebuildBitmap();
         invalidate();
      }
      catch (Exception e)
      {
         Log.e("SVGImageView", e.getMessage());
         e.printStackTrace();

         rebuildBitmap();
         invalidate();
      }
   }


   /**
    * Set the DPI to be used when rendering the SVG file.
    * Some files assume a DPI of 96 or so, which is the typical DPI of a PC screen.
    * Modern mobile screens can have a DPI that is much higher, meaning dimensions
    * based on real world units can be rendered much larger.
    * 
    * You should call this method before calling setSVGAsset() or setSVGResource(). 
    *    
    * @param dpi The DPI to use when rendering. Null if you want to use the default for this device.
    */
   public void  setRenderDPI(Float dpi)
   {
      this.renderDPI = dpi;
   }


}
