/*
   Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.caverock.androidsvg;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.Typeface.Builder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


/**
 * A sample implementation of {@link SVGExternalFileResolver} that retrieves files from
 * an application's "assets" folder.
 */

public class SimpleAssetResolver extends SVGExternalFileResolver
{
   private static final String  TAG = "SimpleAssetResolver";

   private AssetManager  assetManager;
   

   @SuppressWarnings({"WeakerAccess", "unused"})
   public SimpleAssetResolver(AssetManager assetManager)
   {
      super();
      this.assetManager = assetManager;
   }


   private static final Set<String>  supportedFormats = new HashSet<>(8);

   // Static initialiser
   static {
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
      // .avif supported in 12.0+ (S)
      if (android.os.Build.VERSION.SDK_INT >= 31) {
         supportedFormats.add("image/avif");
      }
   }


   /**
    * Attempt to find the specified font in the "assets" folder and return a Typeface object.
    * For the font name "Foo", first the file "Foo.ttf" will be tried and if that fails, "Foo.otf".
    */
   @Override
   public Typeface  resolveFont(String fontFamily, float fontWeight, String fontStyle, float fontStretch)
   {
      Log.i(TAG, "resolveFont('"+fontFamily+"',"+fontWeight+",'"+fontStyle+"',"+fontStretch+")");

      // Try font name with suffix ".ttf"
      try
      {
         return Typeface.createFromAsset(assetManager, fontFamily + ".ttf");
      }
      catch (RuntimeException ignored) {}

      // That failed, so try ".otf"
      try
      {
         return Typeface.createFromAsset(assetManager, fontFamily + ".otf");
      }
      catch (RuntimeException e) {}

      // That failed, so try ".ttc" (Truetype collection), if supported on this version of Android
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      {
         Builder builder = new Builder(assetManager, fontFamily + ".ttc");
         // Get the first font file in the collection
         builder.setTtcIndex(0);
         return builder.build();
      }

      return null;
   }


   /**
    * Attempt to find the specified image file in the <code>assets</code> folder and return a decoded Bitmap.
    */
   @Override
   public Bitmap  resolveImage(String filename)
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
   public boolean  isFormatSupported(String mimeType)
   {
      return supportedFormats.contains(mimeType);
   }


   /**
    * Attempt to find the specified stylesheet file in the "assets" folder and return its string contents.
    * @since 1.3
    */
   @Override
   public String  resolveCSSStyleSheet(String url)
   {
      Log.i(TAG, "resolveCSSStyleSheet("+url+")");
      return getAssetAsString(url);
   }


   /*
    * Read the contents of the asset whose name is given by "url" and return it as a String.
    */
   private String getAssetAsString(String url)
   {
      InputStream is = null;
      //noinspection TryFinallyCanBeTryWithResources
      try
      {
         is = assetManager.open(url);

         //noinspection CharsetObjectCanBeUsed
         Reader r = new InputStreamReader(is, Charset.forName("UTF-8"));
         char[]         buffer = new char[4096];
         StringBuilder  sb = new StringBuilder();
         int            len = r.read(buffer);
         while (len > 0) {
            sb.append(buffer, 0, len);
            len = r.read(buffer);
         }
         return sb.toString();
      }
      catch (IOException e)
      {
         return null;
      }
      finally {
         try {
            if (is != null)
               is.close();
         } catch (IOException e) {
           // Do nothing
         }
      }
   }

}
