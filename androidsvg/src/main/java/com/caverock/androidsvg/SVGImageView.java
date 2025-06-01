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
import java.util.Collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.caverock.androidsvg.utils.SVGBase;

/**
 * SVGImageView is a View widget that allows users to include SVG images in their layouts.
 * 
 * It is implemented as a thin layer over {@code android.widget.ImageView}.
 *
 * <h2>XML attributes</h2>
 * <dl>
 *   <dt><code>svg</code></dt>
 *   <dd>A resource reference, or a file name, of an SVG in your application</dd>
 *   <dt><code>css</code></dt>
 *   <dd>Optional extra CSS to apply when rendering the SVG</dd>
 * </dl>
 */
public class SVGImageView extends ImageView
{
   private SVG                  svg = null;
   private final RenderOptions  renderOptions = new RenderOptions();

   private static Method  setLayerTypeMethod = null;

   private SVGElementClickListener svgElementClickListener;

   static {
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
      if (isInEditMode())
         return;

      TypedArray a = getContext().getTheme()
                     .obtainStyledAttributes(attrs, R.styleable.SVGImageView, defStyle, 0);
      try
      {
         // Check for css attribute
         String  css = a.getString(R.styleable.SVGImageView_css);
         if (css != null)
            renderOptions.css(css);

         // Check whether svg attribute is a resourceId
         int  resourceId = a.getResourceId(R.styleable.SVGImageView_svg, -1);
         if (resourceId != -1) {
            setImageResource(resourceId);
            return;
         }

         // Check whether svg attribute is a string.
         // Could be a URL/filename or an SVG itself
         String  url = a.getString(R.styleable.SVGImageView_svg);
         if (url != null)
         {
            Uri  uri = Uri.parse(url);
            if (internalSetImageURI(uri))
               return;

            // Not a URL, so try loading it as an asset filename
            if (internalSetImageAsset(url))
               return;

            // Last chance, maybe there is an actual SVG in the string
            // If the SVG is in the string, then we will assume it is not very large, and thus doesn't need to be parsed in the background.
            setFromString(url);
         }
         
      } finally {
         a.recycle();
      }
   }


   /**
    * Directly set the SVG that should be rendered by this view.
    * @param svg An {@code SVG} instance
    * @since 1.2.1
    */
   public void  setSVG(SVG svg)
   {
      if (svg == null)
         throw new IllegalArgumentException("Null value passed to setSVG()");
      this.svg = svg;
      doRender();
   }


   /**
    * Directly set the SVG and the CSS.
    * @param svg An {@code SVG} instance
    * @param css Optional extra CSS to apply when rendering
    * @since 1.3
    */
   public void  setSVG(SVG svg, String css)
   {
      if (svg == null)
         throw new IllegalArgumentException("Null value passed to setSVG()");

      this.svg = svg;
      this.renderOptions.css(css);

      doRender();
   }


   /**
    * Directly set the CSS.
    * @param css Extra CSS to apply when rendering
    * @since 1.3
    */
   public void  setCSS(String css)
   {
      this.renderOptions.css(css);
      doRender();
   }



   /**
    * Load an SVG image from the given resource id.
    * @param resourceId the id of an Android resource in your application
    */
   @Override
   public void setImageResource(int resourceId)
   {
      new LoadResourceTask(this.getContext()).execute(resourceId);
   }


   /**
    * Load an SVG image from the given resource URI.
    * @param uri the URI of an Android resource in your application
    */
   @Override
   public void  setImageURI(Uri uri)
   {
      if (!internalSetImageURI(uri))
         Log.e("SVGImageView", "File not found: " + uri);
   }


   /**
    * Load an SVG image from the given asset filename.
    * @param filename the file name of an SVG in the assets folder in your application
    */
   public void  setImageAsset(String filename)
   {
      if (!internalSetImageAsset(filename))
         Log.e("SVGImageView", "File not found: " + filename);
   }



   //===============================================================================================


   /*
    * Attempt to set a picture from a Uri. Return true if it worked.
    */
   private boolean  internalSetImageURI(Uri uri)
   {
      try
      {
         InputStream  is = getContext().getContentResolver().openInputStream(uri);
         new LoadURITask().execute(is);
         return true;
      }
      catch (FileNotFoundException e)
      {
         return false;
      }

   }


   private boolean  internalSetImageAsset(String filename)
   {
      try
      {
         InputStream  is = getContext().getAssets().open(filename);
         new LoadURITask().execute(is);
         return true;
      }
      catch (IOException e)
      {
         return false;
      }

   }


   private void setFromString(String url)
   {
      try {
         this.svg = SVG.getFromString(url);
         doRender();
      } catch (SVGParseException e) {
         // Failed to interpret url as a resource, a filename, or an actual SVG...
         Log.e("SVGImageView", "Could not find SVG at: " + url);
      }
   }


   //===============================================================================================


   @SuppressLint("StaticFieldLeak")
   private class LoadResourceTask extends AsyncTask<Integer, Integer, SVG>
   {
      private final Context  context;

      LoadResourceTask(Context context)
      {
         this.context = context;
      }

      protected SVG  doInBackground(Integer... params)
      {
         int  resourceId = params[0];
         try
         {
            return SVG.getFromResource(context, resourceId);
         }
         catch (SVGParseException e)
         {
            Log.e("SVGImageView", String.format("Error loading resource 0x%x: %s", resourceId, e.getMessage()));
         }
         return null;
      }

      protected void  onPostExecute(SVG svg)
      {
         SVGImageView.this.svg = svg;
         doRender();
      }
   }


   @SuppressLint("StaticFieldLeak")
   private class LoadURITask extends AsyncTask<InputStream, Integer, SVG>
   {
      protected SVG  doInBackground(InputStream... is)
      {
         try
         {
            return SVG.getFromInputStream(is[0]);
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

      protected void  onPostExecute(SVG svg)
      {
         SVGImageView.this.svg = svg;
         doRender();
      }
   }


   //===============================================================================================

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      boolean handled = super.onTouchEvent(event);

      if(svgElementClickListener != null && !handled && event.getAction() == MotionEvent.ACTION_DOWN) {
         ScaleType scaleType = getScaleType();
         float relativePointerX = event.getX();
         float relativePointerY = event.getY();

         float svgWidth = this.svg.getDocumentWidth();
         float svgHeight = this.svg.getDocumentHeight();

         float viewWidth = this.getWidth();
         float viewHeight = this.getHeight();

         switch (scaleType) {
            //TODO add support for CENTER_INSIDE, FIT_XY, MATRIX
            case CENTER:
               float spacingWidth = viewWidth - svgWidth;
               float spacingHeight = viewHeight - svgHeight;

               relativePointerX = (event.getX() - spacingWidth/2) / (viewWidth - spacingWidth);
               relativePointerY = (event.getY() - spacingHeight/2) / (viewHeight - spacingHeight);
               break;

            case CENTER_CROP:
               relativePointerX = event.getX() / viewWidth;
               relativePointerY = event.getY() / viewHeight;

               if (svgWidth / svgHeight < 1) {
                  float factor = svgWidth / viewWidth;
                  float spacing = ((factor * viewHeight) - svgHeight) / factor;

                  relativePointerY = (event.getY() - spacing / 2) / (viewHeight - spacing);
               } else {
                  float factor = svgHeight / viewHeight;
                  float spacing = ((factor * viewWidth) - svgWidth) / factor;

                  relativePointerX = (event.getX() - spacing / 2) / (viewWidth - spacing);
               }
               break;

            case FIT_START:
            case FIT_END:
            case FIT_CENTER:
               relativePointerX = event.getX() / viewWidth;
               relativePointerY = event.getY() / viewHeight;

               float spacing;
               if (svgWidth / svgHeight < 1) {
                  float factor = svgHeight / viewHeight;
                  spacing = ((factor * viewWidth) - svgWidth) / factor;
               } else {
                  float factor = svgWidth / viewWidth;
                  spacing = ((factor * viewHeight) - svgHeight) / factor;
               }

               switch (scaleType) {
                  case FIT_START:
                     if (svgWidth / svgHeight < 1) {
                        relativePointerX = event.getX() / (viewWidth - spacing);
                     } else {
                        relativePointerY = event.getY() / (viewHeight - spacing);
                     }
                     break;
                  case FIT_END:
                     if (svgWidth / svgHeight < 1) {
                        relativePointerX = (event.getX() - spacing) / (viewWidth - spacing);
                     } else {
                        relativePointerY = (event.getY() - spacing) / (viewHeight - spacing);
                     }
                     break;
                  case FIT_CENTER:
                     if (svgWidth / svgHeight < 1) {
                        relativePointerX = (event.getX() - spacing / 2) / (viewWidth - spacing);
                     } else {
                        relativePointerY = (event.getY() - spacing / 2) / (viewHeight - spacing);
                     }
                     break;
               }

         }

         if (relativePointerX >= 0 && relativePointerX <= 1 &&
                 relativePointerY >= 0 && relativePointerY <= 1) {
            Collection<SVGBase.SvgObject> elements = this.svg.getElementsAt(relativePointerX * svgWidth, relativePointerY * svgHeight);

            for (SVGBase.SvgObject element : elements) {
               svgElementClickListener.onClick(element);
               handled = true;
            }
         }
      }

      return handled;
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
         int  LAYER_TYPE_SOFTWARE = View.class.getField("LAYER_TYPE_SOFTWARE").getInt(new View(getContext()));
         setLayerTypeMethod.invoke(this, LAYER_TYPE_SOFTWARE, null);
      }
      catch (Exception e)
      {
         Log.w("SVGImageView", "Unexpected failure calling setLayerType", e);
      }
   }


   private void  doRender()
   {
      if (svg == null)
         return;
      Picture  picture = this.svg.renderToPicture(renderOptions);
      setSoftwareLayerType();
      setImageDrawable(new PictureDrawable(picture));
   }

   public SVGElementClickListener getSVGElementClickListener(){
      return this.svgElementClickListener;
   }

   public void setSVGElementClickListener(SVGElementClickListener listener){
      this.svgElementClickListener = listener;
   }

   public interface SVGElementClickListener {
      void onClick(SVGBase.SvgObject elementClicked);
   }
}
