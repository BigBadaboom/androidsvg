package com.caverock.androidsvg;

import android.graphics.Bitmap;
import android.graphics.Typeface;

import com.caverock.androidsvg.SVG.Style;

/**
 * Resolver class used by the renderer when processing Text and Image elements.
 */

public abstract class SVGExternalFileResolver
{
   /**
    * Called by renderer to resolve font references in text elements.
    * 
    * @param fontFamily Font family as specified in a font-family style attribute.
    * @param fontWeight Font weight as specified in a font-weight style attribute.
    * @param fontStyle  Font style as specified in a font-style style attribute.
    * @return an Android Typeface instance
    */
   public Typeface  resolveFont(String fontFamily, int fontWeight, Style.FontStyle fontStyle)
   {
      return null;
   }

   /**
    * Called by renderer to resolve image file references in image elements.
    * 
    * @param filename Image filename as provided in the xlink:href attribute.
    * @return an Android Bitmap object.
    */
   public Bitmap  resolveImage(String filename)
   {
      return null;
   }
}
