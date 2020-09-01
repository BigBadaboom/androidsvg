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

/**
 * The PreserveAspectRatio class tells the renderer how to scale and position the
 * SVG document in the current viewport.  It is roughly equivalent to the
 * {@code preserveAspectRatio} attribute of an {@code <svg>} element. 
 * <p>
 * In order for scaling to happen, the SVG document must have a viewBox attribute set.
 * For example:
 * 
 * <pre>
 * {@code
 * <svg version="1.1" viewBox="0 0 200 100">
 * }
 * </pre>
 *
 * This class was previous named <code>SVGPositioning</code>. It was renamed in version 1.3
 * to reduce confusion when used as part of the {@link RenderOptions} class.
 */
public class PreserveAspectRatio
{
   private final Alignment  alignment;
   private final Scale      scale;

   /**
    * Draw document at its natural position and scale.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  UNSCALED = new PreserveAspectRatio(null, null);

   /**
    * Stretch horizontally and vertically to fill the viewport.
    * <p>
    * Equivalent to <code>preserveAspectRatio="none"</code> in an SVG.
    */
   @SuppressWarnings("WeakerAccess")
   public static final PreserveAspectRatio  STRETCH = new PreserveAspectRatio(Alignment.none, null);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be centred in the viewport and may have blank strips at either the top and
    * bottom of the viewport or at the sides.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMid meet"</code> in an SVG.
    */
   @SuppressWarnings("WeakerAccess")
   public static final PreserveAspectRatio  LETTERBOX = new PreserveAspectRatio(Alignment.xMidYMid, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports, and at the left of short
    * and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMinYMin meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  START = new PreserveAspectRatio(Alignment.xMinYMin, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports, and at the right of short
    * and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMaxYMax meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  END = new PreserveAspectRatio(Alignment.xMaxYMax, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports, and at the centre of
    * short and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMin meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  TOP = new PreserveAspectRatio(Alignment.xMidYMin, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports, and at the centre of
    * short and wide viewports.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMax meet"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  BOTTOM = new PreserveAspectRatio(Alignment.xMidYMax, Scale.meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the centre of the document will always be visible,
    * but the edges of the document may not.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMidYMid slice"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  FULLSCREEN = new PreserveAspectRatio(Alignment.xMidYMid, Scale.slice);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the top left of the document will always be visible,
    * but the right hand or bottom edge may not.
    * <p>
    * Equivalent to <code>preserveAspectRatio="xMinYMin slice"</code> in an SVG.
    */
   @SuppressWarnings("unused")
   public static final PreserveAspectRatio  FULLSCREEN_START = new PreserveAspectRatio(Alignment.xMinYMin, Scale.slice);



   /**
    * Determines how the document is to me positioned relative to the viewport (normally the canvas).
    * <p>
    * For the value {@code none}, the document is stretched to fit the viewport dimensions. For all
    * other values, the aspect ratio of the document is kept the same but the document is scaled to
    * fit the viewport. 
    * @since 1.2.0
    */
   public enum Alignment
   {
      /** Document is stretched to fit both the width and height of the viewport. When using this Alignment value, the value of Scale is not used and will be ignored. */
      none,
      /** Document is positioned at the top left of the viewport. */
      xMinYMin,
      /** Document is positioned at the centre top of the viewport. */
      xMidYMin,
      /** Document is positioned at the top right of the viewport. */
      xMaxYMin,
      /** Document is positioned at the middle left of the viewport. */
      xMinYMid,
      /** Document is centred in the viewport both vertically and horizontally. */
      xMidYMid,
      /** Document is positioned at the middle right of the viewport. */
      xMaxYMid,
      /** Document is positioned at the bottom left of the viewport. */
      xMinYMax,
      /** Document is positioned at the bottom centre of the viewport. */
      xMidYMax,
      /** Document is positioned at the bottom right of the viewport. */
      xMaxYMax
   }


   /**
    * Determine whether the scaled document fills the viewport entirely or is scaled to
    * fill the viewport without overflowing.
    * @since 1.2.0
    */
   public enum Scale
   {
      /**
       * The document is scaled so that it is as large as possible without overflowing the viewport.
       * There may be blank areas on one or more sides of the document.
       */
      meet,
      /**
       * The document is scaled so that entirely fills the viewport. That means that some of the
       * document may fall outside the viewport and will not be rendered.
       */
      slice
   }


   /*
    * Private constructor
    */
   PreserveAspectRatio(Alignment alignment, Scale scale)
   {
      this.alignment = alignment;
      this.scale = scale;
   }


   /**
    * Parse the given SVG <code>preserveAspectRation</code> attribute value and return an equivalent
    * instance of this class.
    * @param value a string in the same format as an SVG {@code preserveAspectRatio} attribute
    * @return a instance of this class
    */
   public static PreserveAspectRatio  of(String value)
   {
      try {
         return SVGParserImpl.parsePreserveAspectRatio(value);
      } catch (SVGParseException e) {
         throw new IllegalArgumentException(e.getMessage());
      }
   }


   /**
    * Returns the alignment value of this instance.
    * @return the alignment
    */
   @SuppressWarnings("WeakerAccess")
   public Alignment  getAlignment()
   {
      return alignment;
   }


   /**
    * Returns the scale value of this instance.
    * @return the scale
    */
   @SuppressWarnings("WeakerAccess")
   public Scale  getScale()
   {
      return scale;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      PreserveAspectRatio other = (PreserveAspectRatio) obj;
      return (alignment == other.alignment && scale == other.scale);
   }


   @Override
   public String toString()
   {
      return alignment + " " + scale;
   }
}
