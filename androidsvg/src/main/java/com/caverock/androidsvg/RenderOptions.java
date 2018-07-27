/*
   Copyright 2018 Paul LeBeau, Cave Rock Software Ltd.

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

import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * Fluent builder class that provides a render configuration object for the
 * {@link SVG#renderToCanvas(Canvas,RenderOptions)} (Canvas,RenderOptions)} and {@link SVG#renderToPicture(int,int,RenderOptions)} methods.
 */

public class RenderOptions
{
   CSSParser.Ruleset    css = null;
   //String               id = null;
   PreserveAspectRatio  preserveAspectRatio = null;
   String               targetId = null;
   SVG.Box              viewBox = null;
   String               viewId = null;
   SVG.Box              viewPort = null;


   public RenderOptions()
   {
   }


   /**
    * Create a new <code>RenderOptions</code> instance.  This is just an alternative to <code>new RenderOptions()</code>.
    * @return new instance of this class.
    */
   public static RenderOptions  create()
   {
      return new RenderOptions();
   }


   /**
    * Creates a copy of the given <code>RenderOptions</code> object.
    * @param other the object to copy
    */
   public RenderOptions(RenderOptions other)
   {
      if (other == null)
         return;
      this.css = other.css;
      //this.id = other.id;
      this.preserveAspectRatio = other.preserveAspectRatio;
      this.viewBox = other.viewBox;
      this.viewId = other.viewId;
      this.viewPort = other.viewPort;
   }


   /**
    * Specifies some additional CSS rules that will be applied during render in addition to
    * any specified in the file itself.
    * @param css CSS rules to apply
    * @return this RenderOptions instance
    */
   public RenderOptions  css(String css)
   {
      CSSParser  parser = new CSSParser(CSSParser.Source.RenderOptions);
      this.css = parser.parse(css);
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had CSS set with {@link #css(String)}.
    */
   public boolean hasCss()
   {
//      return this.css != null && this.css.ruleCount() > 0;
      return this.css != null;
   }


   /**
    * Specifies that only the element with the matching {@code id} attribute, and its descendants,
    * should be rendered.
    * @param id {@code id} of the subtree to render
    * @return this RenderOptions instance
    */
/*
   public RenderOptions  id(String id)
   {
      this.id = id;
      return this;
   }
*/


   /**
    * Returns true if this RenderOptions instance has had an id set with {@link #id(String)}.
    */
/*
   public boolean hasId()
   {
      return this.id != null;
   }
*/


   /**
    *
    * @param preserveAspectRatio
    * @return
    */
   public RenderOptions  preserveAspectRatio(PreserveAspectRatio preserveAspectRatio)
   {
      this.preserveAspectRatio = preserveAspectRatio;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a preserveAspectRatio value set with {@link #preserveAspectRatio(PreserveAspectRatio)} .
    */
   public boolean hasPreserveAspectRatio()
   {
      return this.preserveAspectRatio != null;
   }


   /**
    * Specifies the {@code id} of a {@code <view>} element in the SVG.  A {@code <view>}
    * element is a way to specify a predetermined view of the document, that differs from the default view.
    * For example it can allow you to focus in on a small detail of the document.
    *
    * Note: setting this option will override any {@link #viewBox(float,float,float,float)} or {@link #preserveAspectRatio(PreserveAspectRatio)} settings.
    *
    * @param viewId the id attribute of the view that should be used for rendering
    * @return this RenderOptions instance
    */
   public RenderOptions  view(String viewId)
   {
      this.viewId = viewId;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a view set with {@link #view(String)}.
    */
   public boolean hasView()
   {
      return this.viewId != null;
   }


   /**
    * Specifies alternative values to use for the root element {@code viewBox}. Any existing {@code viewBox}
    * attribute value will be ignored.
    *
    * Note: will be overridden if a {@link #view(String)} is set.
    *
    * @param minX The left X coordinate of the viewBox
    * @param minY The top Y coordinate of the viewBox
    * @param width The width of the viewBox
    * @param height The height of the viewBox
    * @return this RenderOptions instance
    */
   public RenderOptions  viewBox(float minX, float minY, float width, float height)
   {
      this.viewBox = new SVG.Box(minX, minY, width, height);
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a viewBox set with {@link #viewBox(float,float,float,float)}.
    */
   public boolean hasViewBox()
   {
      return this.viewBox != null;
   }


   /**
    * Describes the viewport into which the SVG should be rendered.  If this is not specified,
    * then the whole of the canvas will be used as the viewport.  If rendering to a <code>Picture</code>
    * then a default viewport width and height will be used.

    * @param minX The left X coordinate of the viewport
    * @param minY The top Y coordinate of the viewport
    * @param width The width of the viewport
    * @param height The height of the viewport
    * @return this RenderOptions instance
    */
   public RenderOptions  viewPort(float minX, float minY, float width, float height)
   {
      this.viewPort = new SVG.Box(minX, minY, width, height);
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a viewPort set with {@link #viewPort(float,float,float,float)}.
    */
   public boolean hasViewPort()
   {
      return this.viewPort != null;
   }


   /**
    * Specifies the {@code id} of an element, in the SVG, to treat as the target element when
    * using the {@code :target} CSS pseudo class.
    *
    * @param targetId the id attribute of an element
    * @return this RenderOptions instance
    */
   public RenderOptions  target(String targetId)
   {
      this.targetId = targetId;
      return this;
   }


   /**
    * Returns true if this RenderOptions instance has had a view set with {@link #view(String)}.
    */
   public boolean hasTarget()
   {
      return this.targetId != null;
   }


}
