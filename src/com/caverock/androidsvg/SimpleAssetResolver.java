package com.caverock.androidsvg;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;


/**
 * A sample implementation of {@link SVGExternalFileResolver} that retrieves files from
 * an application's "assets" folder.
 */

public class SimpleAssetResolver extends SVGExternalFileResolver
{
   private static final String  TAG = SimpleAssetResolver.class.getSimpleName();

   private AssetManager  assetManager;
   

   public SimpleAssetResolver(AssetManager assetManager)
   {
      super();
      this.assetManager = assetManager;
   }


   private static final Set<String>  supportedFormats = new HashSet<String>(8);

   // Static initialiser
   {
      // PNG, JPEG and SVG are required by the SVG 1.2 spec
      supportedFormats.add("image/svg+xml");
      supportedFormats.add("image/jpeg");
      supportedFormats.add("image/png");
      // Other image formats supported by Android BitmapFactory
      supportedFormats.add("image/pjpeg");
      supportedFormats.add("image/gif");
      supportedFormats.add("image/bmp");
      supportedFormats.add("image/x-windows-bmp");
      // .webp supported in 4.0+ (ICE_CREAM_SANDWICH)
      if (android.os.Build.VERSION.SDK_INT >= 14) {
         supportedFormats.add("image/webp");
      }
   }


   /**
    * Attempt to find the specified font in the "assets" folder and return a Typeface object.
    * For the font name "Foo", first the file "Foo.ttf" will be tried and if that fails, "Foo.otf".
    */
   @Override
   public Typeface resolveFont(String fontFamily, int fontWeight, String fontStyle)
   {
      Log.i(TAG, "resolveFont("+fontFamily+","+fontWeight+","+fontStyle+")");

      // Try font name with suffix ".ttf"
      try
      {
         return Typeface.createFromAsset(assetManager, fontFamily + ".ttf");
      }
      catch (Exception e) {}

      // That failed, so try ".otf"
      try
      {
         return Typeface.createFromAsset(assetManager, fontFamily + ".otf");
      }
      catch (Exception e)
      {
         return null;
      }
   }


   /**
    * Attempt to find the specified image file in the "assets" folder and return a decoded Bitmap.
    */
   @Override
   public Bitmap resolveImage(String filename)
   {
      Log.i(TAG, "resolveImage("+filename+")");

      try
      {
         InputStream  istream = assetManager.open(filename);
         return BitmapFactory.decodeStream(istream);
      }
      catch (IOException e1)
      {
         return null;
      }
   }


   /**
    * Returns true when passed the MIME types for SVG, JPEG, PNG or any of the
    * other bitmap image formats supported by Android's BitmapFactory class.
    */
   @Override
   public boolean isFormatSupported(String mimeType)
   {
      return supportedFormats.contains(mimeType);
   }

}
