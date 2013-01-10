package com.caverock.androidsvg;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Just a subclass of the Android Canvas that allows us to retrieve
 * the underlying Bitmap object.  This is needed for the compositing process.
 * 
 * @author Paul
 */

public class SVGCanvas extends Canvas
{
   private Bitmap  bitmap = null;

   public SVGCanvas()
   {
      super();
   }


   public SVGCanvas(Bitmap bitmap)
   {
      super(bitmap);
      this.bitmap = bitmap;
   }


   @Override
   public void  setBitmap(Bitmap bitmap)
   {
      this.bitmap = bitmap;
      super.setBitmap(bitmap);
   }


   protected Bitmap  getBitmap()
   {
      return bitmap;
   }

}
