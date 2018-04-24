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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * SVGImageView is a View widget that allows users to include SVG images in their layouts.
 * 
 * It is implemented as a thin layer over {@code android.widget.ImageView}.
 * <p>
 * In its present form it has one significant limitation.  It uses the {@link SVG#renderToPicture()}
 * method. That means that SVG documents that use {@code <mask>} elements will not display correctly.
 * 
 * @attr ref R.styleable#SVGImageView_svg
 */
@SuppressWarnings("JavaDoc")
public class SVGBaseImageView extends ImageView
{
   private static Method  setLayerTypeMethod = null;

   static {
      try
      {
         setLayerTypeMethod = View.class.getMethod("setLayerType", Integer.TYPE, Paint.class);
      }
      catch (NoSuchMethodException e) { /* do nothing */ }
   }


   public SVGBaseImageView(Context context)
   {
      super(context);
   }


   public SVGBaseImageView(Context context, AttributeSet attrs)
   {
      super(context, attrs, 0);
      init(attrs, 0);
   }


   public SVGBaseImageView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      init(attrs, defStyle);
   }

   
   private void  init(AttributeSet attrs, int defStyle)
   {
      if (isInEditMode())
         return;

      TypedArray a = getContext().getTheme()
                     .obtainStyledAttributes(attrs, R.styleable.SVGBaseImageView, defStyle, 0);
      try
      {
         int  resourceId = a.getResourceId(R.styleable.SVGBaseImageView_svg, -1);
         if (resourceId != -1) {
            setImageResource(resourceId);
            return;
         }

         String  url = a.getString(R.styleable.SVGBaseImageView_svg);
         if (url != null)
         {
            Uri  uri = Uri.parse(url);
            if (internalSetImageURI(uri, false))
               return;

            // Last chance, try loading it as an asset filename
            setImageAsset(url);
         }
         
      } finally {
         a.recycle();
      }
   }



   /**
    * Directly set the SVG.
    */
   public void  setSVG(SVG mysvg)
   {
      if (mysvg == null)
         throw new IllegalArgumentException("Null value passed to setSVG()");

      setSoftwareLayerType();
      setImageDrawable(new PictureDrawable(mysvg.renderToPicture()));
   }


   /**
    * Load an SVG image from the given resource id.
    */
   @Override
   public void setImageResource(int resourceId)
   {
      new LoadResourceTask(getContext(), resourceId).execute();
   }


   /**
    * Load an SVG image from the given resource URI.
    */
   @Override
   public void  setImageURI(Uri uri)
   {
      internalSetImageURI(uri, true);
   }


   /**
    * Load an SVG image from the given asset filename.
    */
   public void  setImageAsset(String filename)
   {
      new LoadAssetTask(getContext(), filename).execute();
   }


   /*
    * Attempt to set a picture from a Uri. Return true if it worked.
    */
   private boolean  internalSetImageURI(Uri uri, boolean isDirectRequestFromUser)
   {
      InputStream  is;
      try
      {
         is = getContext().getContentResolver().openInputStream(uri);
      }
      catch (FileNotFoundException e)
      {
         if (isDirectRequestFromUser)
            Log.e("SVGImageView", "File not found: " + uri);
         return false;
      }

      new LoadURITask().execute(is);
      return true;
   }


   //===============================================================================================


   private class LoadResourceTask extends AsyncTask<Integer, Integer, Picture>
   {
      private Context  context;
      private int      resourceId;

      LoadResourceTask(Context context, int resourceId)
      {
         this.context = context;
         this.resourceId = resourceId;
      }

      protected Picture  doInBackground(Integer... params)
      {
         try
         {
            SVG  svg = SVG.getFromResource(context, resourceId);
            return svg.renderToPicture();
         }
         catch (SVGParseException e)
         {
            Log.e("SVGImageView", String.format("Error loading resource 0x%x: %s", resourceId, e.getMessage()));
         }
         return null;
      }

      protected void  onPostExecute(Picture picture)
      {
         if (picture != null) {
            setSoftwareLayerType();
            setImageDrawable(new PictureDrawable(picture));
         }
      }
   }


   private class LoadAssetTask extends AsyncTask<String, Integer, Picture>
   {
      private Context  context;
      private String   filename;

      LoadAssetTask(Context context, String filename)
      {
         this.context = context;
         this.filename = filename;
      }

      protected Picture  doInBackground(String... params)
      {
         try
         {
            SVG  svg = SVG.getFromAsset(context.getAssets(), filename);
            return svg.renderToPicture();
         }
         catch (SVGParseException e)
         {
            Log.e("SVGImageView", "Error loading file " + filename + ": " + e.getMessage());
         }
         catch (FileNotFoundException e)
         {
            Log.e("SVGImageView", "File not found: " + filename);
         }
         catch (IOException e)
         {
            Log.e("SVGImageView", "Unable to load asset file: " + filename, e);
         }
         return null;
      }

      protected void  onPostExecute(Picture picture)
      {
         if (picture != null) {
            setSoftwareLayerType();
            setImageDrawable(new PictureDrawable(picture));
         }
      }
   }


   private class LoadURITask extends AsyncTask<InputStream, Integer, Picture>
   {
      protected Picture  doInBackground(InputStream... is)
      {
         try
         {
            SVG  svg = SVG.getFromInputStream(is[0]);
            return svg.renderToPicture();
         }
         catch (SVGParseException e)
         {
            Log.e("SVGImageView", "Parse error loading URI: " + e.getMessage());
         }
         finally
         {
            try
            {
               is[0].close();
            }
            catch (IOException e) { /* do nothing */ }
         }
         return null;
      }

      protected void  onPostExecute(Picture picture)
      {
         if (picture != null) {
            setSoftwareLayerType();
            setImageDrawable(new PictureDrawable(picture));
         }
      }
   }


   //===============================================================================================


   /*
    * Use reflection to call an API 11 method from this library (which is configured with a minSdkVersion of 8)
    */
   private void  setSoftwareLayerType()
   {
      if (setLayerTypeMethod == null)
         return;

      try
      {
         int  LAYER_TYPE_SOFTWARE = View.class.getField("LAYER_TYPE_SOFTWARE").getInt(new View(getContext()));
         setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
      }
      catch (Exception e)
      {
         Log.w("SVGImageView", "Unexpected failure calling setLayerType", e);
      }
   }
}
