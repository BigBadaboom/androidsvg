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
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * A thin layer over android.widget.ImageView.
 *
 */
public class SVGImageView extends ImageView
{
   private static Method  setLayerTypeMethod = null;

   {
      try
      {
         setLayerTypeMethod = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
      }
      catch (NoSuchMethodException e) { /* do nothing */ }
   }


   public SVGImageView(Context context)
   {
      super(context);
   }


   public SVGImageView(Context context, AttributeSet attrs)
   {
      super(context, attrs, 0);
      init(attrs, 0);
   }


   public SVGImageView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      init(attrs, defStyle);
   }

   
   private void  init(AttributeSet attrs, int defStyle)
   {
      TypedArray a = getContext().getTheme()
                     .obtainStyledAttributes(attrs, R.styleable.SVGImageView, defStyle, 0);
      try
      {
         int  resourceId = a.getResourceId(R.styleable.SVGImageView_svg, -1);
         if (resourceId != -1) {
            setImageResource(resourceId);
            return;
         }

         String  url = a.getString(R.styleable.SVGImageView_svg);
         Uri  uri = Uri.parse(url);
         if (internalSetImageURI(uri))
            return;
 
         // Last chance, try loading it as an asset filename
         setImageAsset(url);
         
      } finally {
         a.recycle();
      }
   }



   /**
    * Load an SVG image from the given resource id.
    */
   @Override
   public void setImageResource(int resourceId)
   {
      try
      {
         SVG  svg = SVG.getFromResource(getContext(), resourceId);
         setSoftwareLayerType();
         setImageDrawable(new PictureDrawable(svg.renderToPicture()));
      }
      catch (SVGParseException e)
      {
         Log.w("SVGImageView", "Unable to find resource: " + resourceId, e);
      }
   }


   /**
    * Load an SVG image from the given URI.
    */
   @Override
   public void  setImageURI(Uri uri)
   {
      internalSetImageURI(uri);
   }


   /**
    * Load an SVG image from the given asset filename.
    */
   public void setImageAsset(String filename)
   {
      try
      {
         SVG  svg = SVG.getFromAsset(getContext().getAssets(), filename);
         setSoftwareLayerType();
         setImageDrawable(new PictureDrawable(svg.renderToPicture()));
      }
      catch (Exception e)
      {
         Log.w("SVGImageView", "Unable to find asset file: " + filename, e);
      }
   }


   /*
    * Attempt to set a picture from a Uri. Return true if it worked.
    */
   private boolean  internalSetImageURI(Uri uri)
   {
      InputStream  is = null;
      try {
         is = getContext().getContentResolver().openInputStream(uri);
         SVG  svg = SVG.getFromInputStream(is);
         setSoftwareLayerType();
         setImageDrawable(new PictureDrawable(svg.renderToPicture()));
         return true;
      }
      catch (Exception e) {
         Log.w("ImageView", "Unable to open content: " + uri, e);
         return false;
      }
      finally {
         try
         {
            if (is != null)
               is.close();
         }
         catch (IOException e) { /* do nothing */ }
      }
   }


   /*
    * Use reflection to call an API 11 method from this library (which is configured with a minSdkVersion of 8)
    */
   private void  setSoftwareLayerType()
   {
      if (setLayerTypeMethod == null)
         return;

      try
      {
         setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
      }
      catch (Exception e)
      {
         Log.w("SVGImageView", "Unexpected failure calling setLayerType", e);
      }
   }
}
