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

import android.graphics.Bitmap;
import android.graphics.Typeface;

/**
 * Resolver class used by the renderer when resolving font, image, and external CSS references.
 * <p>
 * The default behaviour is to tell AndroidSVG that the reference could not be found.
 * <p>
 * Extend this class and override the methods if you want to customise how AndroidSVG treats font, image, and external CSS references.
 */

public abstract class SVGExternalFileResolver
{
   /**
    * Called by renderer to resolve font references in &lt;text&gt; elements.
    * <p>
    * An implementation of this method should return a {@code Typeface} instance, or null
    * if you want the renderer to ignore this font request.
    * <p>
    * Note that AndroidSVG does not attempt to cache Typeface references.  If you want
    * them cached, for speed or memory reasons, you should do so yourself.
    * 
    * @param fontFamily Font family as specified in a font-family style attribute.
    * @param fontWeight Font weight as specified in a font-weight style attribute.
    * @param fontStyle  Font style as specified in a font-style style attribute.
    * @return an Android Typeface instance, or null
    */
   public Typeface  resolveFont(String fontFamily, int fontWeight, String fontStyle)
   {
      return null;
   }

   /**
    * Called by renderer to resolve image file references in &lt;image&gt; elements.
    * <p>
    * An implementation of this method should return a {@code Bitmap} instance, or null if
    * you want the renderer to ignore this image.
    * <p>
    * Note that AndroidSVG does not attempt to cache Bitmap references.  If you want
    * them cached, for speed or memory reasons, you should do so yourself.
    * 
    * @param filename the filename as provided in the xlink:href attribute of a &lt;image&gt; element.
    * @return an Android Bitmap object, or null if the image could not be found.
    */
   public Bitmap  resolveImage(String filename)
   {
      return null;
   }

   /**
    * Called by the parser to resolve CSS stylesheet file references in &lt;?xml-stylesheet?&gt;
    * processing instructions.
    * <p>
    * An implementation of this method should return a {@code String} whose contents
    * correspond to the URL passed in.
    * <p>
    * Note that AndroidSVG does not attempt to cache stylesheet references.  If you want
    * them cached, for speed or memory reasons, you should do so yourself.
    *
    * @param url the URL of the CSS file as it appears in the SVG file.
    * @return a AndroidSVG CSSStyleSheet object, or null if the stylesheet could not be found.
    */
   public String  resolveCSSStyleSheet(String url)
   {
      return null;
   }

   /**
    * Called by renderer to determine whether a particular format is supported.  In particular,
    * this method is used in &lt;switch&gt; elements when processing {@code requiredFormats}
    * conditionals.
    * 
    * @param mimeType A MIME type (such as "image/jpeg").
    * @return true if your {@code resolveImage()} implementation supports this file format.
    */
   public boolean  isFormatSupported(String mimeType)
   {
      return false;
   }
}
