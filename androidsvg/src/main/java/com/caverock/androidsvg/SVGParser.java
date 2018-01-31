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

import android.graphics.Matrix;
import android.util.Log;
import android.util.Xml;

import com.caverock.androidsvg.CSSParser.MediaType;
import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.CSSClipRect;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.CurrentColor;
import com.caverock.androidsvg.SVG.GradientSpread;
import com.caverock.androidsvg.SVG.Length;
import com.caverock.androidsvg.SVG.PaintReference;
import com.caverock.androidsvg.SVG.Style;
import com.caverock.androidsvg.SVG.Style.RenderQuality;
import com.caverock.androidsvg.SVG.Style.TextDecoration;
import com.caverock.androidsvg.SVG.Style.TextDirection;
import com.caverock.androidsvg.SVG.Style.VectorEffect;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVG.SvgPaint;
import com.caverock.androidsvg.SVG.TextChild;
import com.caverock.androidsvg.SVG.TextPositionedContainer;
import com.caverock.androidsvg.SVG.TextRoot;
import com.caverock.androidsvg.SVG.Unit;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/*
 * SVG parser code. Used by SVG class. Should not be called directly.
 */

class SVGParser
{
   private static final String  TAG = "SVGParser";

   private static final String  SVG_NAMESPACE = "http://www.w3.org/2000/svg";
   private static final String  XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
   private static final String  FEATURE_STRING_PREFIX = "http://www.w3.org/TR/SVG11/feature#";

   // Used by the automatic XML parser switching code.
   // This value defines how much of the SVG file preamble will we keep in order to check for
   // a doctype definition that has internal entities defined.
   public static final int  ENTITY_WATCH_BUFFER_SIZE = 4096;

   // SVG parser
   private SVG               svgDocument = null;
   private SVG.SvgContainer  currentElement = null;

   // For handling elements we don't support
   private boolean   ignoring = false;
   private int       ignoreDepth;

   // For handling <title> and <desc>
   private boolean        inMetadataElement = false;
   private SVGElem        metadataTag = null;
   private StringBuilder  metadataElementContents = null;

   // For handling <style>
   private boolean        inStyleElement = false;
   private StringBuilder  styleElementContents = null;


   // Define SVG tags
   private enum  SVGElem
   {
      svg,
      a,
      circle,
      clipPath,
      defs,
      desc,
      ellipse,
      g,
      image,
      line,
      linearGradient,
      marker,
      mask,
      path,
      pattern,
      polygon,
      polyline,
      radialGradient,
      rect,
      solidColor,
      stop,
      style,
      SWITCH,
      symbol,
      text,
      textPath,
      title,
      tref,
      tspan,
      use,
      view,
      UNSUPPORTED;
      
      private static final Map<String,SVGElem>  cache = new HashMap<>();
      
      public static SVGElem  fromString(String str)
      {
         // First check cache to see if it is there
         SVGElem  elem = cache.get(str);
         if (elem != null)
            return elem;
         // Manual check for "switch" which is in upper case because it's a Java reserved identifier
         if (str.equals("switch")) {
            cache.put(str, SWITCH);
            return SWITCH;
         }
         // Do the (slow) Enum.valueOf()
         try
         {
            elem = valueOf(str);
            if (elem != SWITCH) {  // Don't allow matches with "SWITCH"
               cache.put(str, elem);
               return elem;
            }
         } 
         catch (IllegalArgumentException e)
         {
            // Do nothing
         }
         // Unknown element name
         cache.put(str, UNSUPPORTED);
         return UNSUPPORTED;
      }
   }

   // Element types that we don't support. Those that are containers have their
   // contents ignored.
   //private static final String  TAG_ANIMATECOLOR        = "animateColor";
   //private static final String  TAG_ANIMATEMOTION       = "animateMotion";
   //private static final String  TAG_ANIMATETRANSFORM    = "animateTransform";
   //private static final String  TAG_ALTGLYPH            = "altGlyph";
   //private static final String  TAG_ALTGLYPHDEF         = "altGlyphDef";
   //private static final String  TAG_ALTGLYPHITEM        = "altGlyphItem";
   //private static final String  TAG_ANIMATE             = "animate";
   //private static final String  TAG_COLORPROFILE        = "color-profile";
   //private static final String  TAG_CURSOR              = "cursor";
   //private static final String  TAG_FEBLEND             = "feBlend";
   //private static final String  TAG_FECOLORMATRIX       = "feColorMatrix";
   //private static final String  TAG_FECOMPONENTTRANSFER = "feComponentTransfer";
   //private static final String  TAG_FECOMPOSITE         = "feComposite";
   //private static final String  TAG_FECONVOLVEMATRIX    = "feConvolveMatrix";
   //private static final String  TAG_FEDIFFUSELIGHTING   = "feDiffuseLighting";
   //private static final String  TAG_FEDISPLACEMENTMAP   = "feDisplacementMap";
   //private static final String  TAG_FEDISTANTLIGHT      = "feDistantLight";
   //private static final String  TAG_FEFLOOD             = "feFlood";
   //private static final String  TAG_FEFUNCA             = "feFuncA";
   //private static final String  TAG_FEFUNCB             = "feFuncB";
   //private static final String  TAG_FEFUNCG             = "feFuncG";
   //private static final String  TAG_FEFUNCR             = "feFuncR";
   //private static final String  TAG_FEGAUSSIANBLUR      = "feGaussianBlur";
   //private static final String  TAG_FEIMAGE             = "feImage";
   //private static final String  TAG_FEMERGE             = "feMerge";
   //private static final String  TAG_FEMERGENODE         = "feMergeNode";
   //private static final String  TAG_FEMORPHOLOGY        = "feMorphology";
   //private static final String  TAG_FEOFFSET            = "feOffset";
   //private static final String  TAG_FEPOINTLIGHT        = "fePointLight";
   //private static final String  TAG_FESPECULARLIGHTING  = "feSpecularLighting";
   //private static final String  TAG_FESPOTLIGHT         = "feSpotLight";
   //private static final String  TAG_FETILE              = "feTile";
   //private static final String  TAG_FETURBULENCE        = "feTurbulence";
   //private static final String  TAG_FILTER              = "filter";
   //private static final String  TAG_FONT                = "font";
   //private static final String  TAG_FONTFACE            = "font-face";
   //private static final String  TAG_FONTFACEFORMAT      = "font-face-format";
   //private static final String  TAG_FONTFACENAME        = "font-face-name";
   //private static final String  TAG_FONTFACESRC         = "font-face-src";
   //private static final String  TAG_FONTFACEURI         = "font-face-uri";
   //private static final String  TAG_FOREIGNOBJECT       = "foreignObject";
   //private static final String  TAG_GLYPH               = "glyph";
   //private static final String  TAG_GLYPHREF            = "glyphRef";
   //private static final String  TAG_HKERN               = "hkern";
   //private static final String  TAG_MASK                = "mask";
   //private static final String  TAG_METADATA            = "metadata";
   //private static final String  TAG_MISSINGGLYPH        = "missing-glyph";
   //private static final String  TAG_MPATH               = "mpath";
   //private static final String  TAG_SCRIPT              = "script";
   //private static final String  TAG_SET                 = "set";
   //private static final String  TAG_STYLE               = "style";
   //private static final String  TAG_VKERN               = "vkern";


   // Supported SVG attributes
   private enum  SVGAttr
   {
      CLASS,    // Upper case because 'class' is a reserved word. Handled as a special case.
      clip,
      clip_path,
      clipPathUnits,
      clip_rule,
      color,
      cx, cy,
      direction,
      dx, dy,
      fx, fy,
      d,
      display,
      fill,
      fill_rule,
      fill_opacity,
      font,
      font_family,
      font_size,
      font_weight,
      font_style,
      // font_size_adjust, font_stretch, font_variant,  
      gradientTransform,
      gradientUnits,
      height,
      href,
      // id,
      image_rendering,
      marker,
      marker_start, marker_mid, marker_end,
      markerHeight, markerUnits, markerWidth,
      mask,
      maskContentUnits, maskUnits,
      media,
      offset,
      opacity,
      orient,
      overflow,
      pathLength,
      patternContentUnits, patternTransform, patternUnits,
      points,
      preserveAspectRatio,
      r,
      refX,
      refY,
      requiredFeatures, requiredExtensions, requiredFormats, requiredFonts,
      rx, ry,
      solid_color, solid_opacity,
      spreadMethod,
      startOffset,
      stop_color, stop_opacity,
      stroke,
      stroke_dasharray,
      stroke_dashoffset,
      stroke_linecap,
      stroke_linejoin,
      stroke_miterlimit,
      stroke_opacity,
      stroke_width,
      style,
      systemLanguage,
      text_anchor,
      text_decoration,
      transform,
      type,
      vector_effect,
      version,
      viewBox,
      width,
      x, y,
      x1, y1,
      x2, y2,
      viewport_fill, viewport_fill_opacity,
      visibility,
      UNSUPPORTED;

      private static final Map<String,SVGAttr>  cache = new HashMap<>();
      
      public static SVGAttr  fromString(String str)
      {
         // First check cache to see if it is there
         SVGAttr  attr = cache.get(str);
         if (attr != null)
            return attr;
         // Do the (slow) Enum.valueOf()
         if (str.equals("class")) {
            cache.put(str, CLASS);
            return CLASS;
         }
         // Check for underscore in attribute - it could potentially confuse us
         if (str.indexOf('_') != -1) {
            cache.put(str, UNSUPPORTED);
            return UNSUPPORTED;
         }
         try
         {
            attr = valueOf(str.replace('-', '_'));
            if (attr != CLASS) {
               cache.put(str, attr);
               return attr;
            }
         } 
         catch (IllegalArgumentException e)
         {
            // Do nothing
         }
         // Unknown attribute name
         cache.put(str, UNSUPPORTED);
         return UNSUPPORTED;
      }

   }


   // Special attribute keywords
   private static final String  NONE = "none";
   private static final String  CURRENTCOLOR = "currentColor";

   private static final String VALID_DISPLAY_VALUES = "|inline|block|list-item|run-in|compact|marker|table|inline-table"+
                                                      "|table-row-group|table-header-group|table-footer-group|table-row"+
                                                      "|table-column-group|table-column|table-cell|table-caption|none|";
   private static final String VALID_VISIBILITY_VALUES = "|visible|hidden|collapse|";

   // These static inner classes are only loaded/initialized when first used and are thread safe
   private static class ColourKeywords {
      private static final Map<String, Integer> colourKeywords = new HashMap<>(47);
      static {
         colourKeywords.put("aliceblue", 0xfff0f8ff);
         colourKeywords.put("antiquewhite", 0xfffaebd7);
         colourKeywords.put("aqua", 0xff00ffff);
         colourKeywords.put("aquamarine", 0xff7fffd4);
         colourKeywords.put("azure", 0xfff0ffff);
         colourKeywords.put("beige", 0xfff5f5dc);
         colourKeywords.put("bisque", 0xffffe4c4);
         colourKeywords.put("black", 0xff000000);
         colourKeywords.put("blanchedalmond", 0xffffebcd);
         colourKeywords.put("blue", 0xff0000ff);
         colourKeywords.put("blueviolet", 0xff8a2be2);
         colourKeywords.put("brown", 0xffa52a2a);
         colourKeywords.put("burlywood", 0xffdeb887);
         colourKeywords.put("cadetblue", 0xff5f9ea0);
         colourKeywords.put("chartreuse", 0xff7fff00);
         colourKeywords.put("chocolate", 0xffd2691e);
         colourKeywords.put("coral", 0xffff7f50);
         colourKeywords.put("cornflowerblue", 0xff6495ed);
         colourKeywords.put("cornsilk", 0xfffff8dc);
         colourKeywords.put("crimson", 0xffdc143c);
         colourKeywords.put("cyan", 0xff00ffff);
         colourKeywords.put("darkblue", 0xff00008b);
         colourKeywords.put("darkcyan", 0xff008b8b);
         colourKeywords.put("darkgoldenrod", 0xffb8860b);
         colourKeywords.put("darkgray", 0xffa9a9a9);
         colourKeywords.put("darkgreen", 0xff006400);
         colourKeywords.put("darkgrey", 0xffa9a9a9);
         colourKeywords.put("darkkhaki", 0xffbdb76b);
         colourKeywords.put("darkmagenta", 0xff8b008b);
         colourKeywords.put("darkolivegreen", 0xff556b2f);
         colourKeywords.put("darkorange", 0xffff8c00);
         colourKeywords.put("darkorchid", 0xff9932cc);
         colourKeywords.put("darkred", 0xff8b0000);
         colourKeywords.put("darksalmon", 0xffe9967a);
         colourKeywords.put("darkseagreen", 0xff8fbc8f);
         colourKeywords.put("darkslateblue", 0xff483d8b);
         colourKeywords.put("darkslategray", 0xff2f4f4f);
         colourKeywords.put("darkslategrey", 0xff2f4f4f);
         colourKeywords.put("darkturquoise", 0xff00ced1);
         colourKeywords.put("darkviolet", 0xff9400d3);
         colourKeywords.put("deeppink", 0xffff1493);
         colourKeywords.put("deepskyblue", 0xff00bfff);
         colourKeywords.put("dimgray", 0xff696969);
         colourKeywords.put("dimgrey", 0xff696969);
         colourKeywords.put("dodgerblue", 0xff1e90ff);
         colourKeywords.put("firebrick", 0xffb22222);
         colourKeywords.put("floralwhite", 0xfffffaf0);
         colourKeywords.put("forestgreen", 0xff228b22);
         colourKeywords.put("fuchsia", 0xffff00ff);
         colourKeywords.put("gainsboro", 0xffdcdcdc);
         colourKeywords.put("ghostwhite", 0xfff8f8ff);
         colourKeywords.put("gold", 0xffffd700);
         colourKeywords.put("goldenrod", 0xffdaa520);
         colourKeywords.put("gray", 0xff808080);
         colourKeywords.put("green", 0xff008000);
         colourKeywords.put("greenyellow", 0xffadff2f);
         colourKeywords.put("grey", 0xff808080);
         colourKeywords.put("honeydew", 0xfff0fff0);
         colourKeywords.put("hotpink", 0xffff69b4);
         colourKeywords.put("indianred", 0xffcd5c5c);
         colourKeywords.put("indigo", 0xff4b0082);
         colourKeywords.put("ivory", 0xfffffff0);
         colourKeywords.put("khaki", 0xfff0e68c);
         colourKeywords.put("lavender", 0xffe6e6fa);
         colourKeywords.put("lavenderblush", 0xfffff0f5);
         colourKeywords.put("lawngreen", 0xff7cfc00);
         colourKeywords.put("lemonchiffon", 0xfffffacd);
         colourKeywords.put("lightblue", 0xffadd8e6);
         colourKeywords.put("lightcoral", 0xfff08080);
         colourKeywords.put("lightcyan", 0xffe0ffff);
         colourKeywords.put("lightgoldenrodyellow", 0xfffafad2);
         colourKeywords.put("lightgray", 0xffd3d3d3);
         colourKeywords.put("lightgreen", 0xff90ee90);
         colourKeywords.put("lightgrey", 0xffd3d3d3);
         colourKeywords.put("lightpink", 0xffffb6c1);
         colourKeywords.put("lightsalmon", 0xffffa07a);
         colourKeywords.put("lightseagreen", 0xff20b2aa);
         colourKeywords.put("lightskyblue", 0xff87cefa);
         colourKeywords.put("lightslategray", 0xff778899);
         colourKeywords.put("lightslategrey", 0xff778899);
         colourKeywords.put("lightsteelblue", 0xffb0c4de);
         colourKeywords.put("lightyellow", 0xffffffe0);
         colourKeywords.put("lime", 0xff00ff00);
         colourKeywords.put("limegreen", 0xff32cd32);
         colourKeywords.put("linen", 0xfffaf0e6);
         colourKeywords.put("magenta", 0xffff00ff);
         colourKeywords.put("maroon", 0xff800000);
         colourKeywords.put("mediumaquamarine", 0xff66cdaa);
         colourKeywords.put("mediumblue", 0xff0000cd);
         colourKeywords.put("mediumorchid", 0xffba55d3);
         colourKeywords.put("mediumpurple", 0xff9370db);
         colourKeywords.put("mediumseagreen", 0xff3cb371);
         colourKeywords.put("mediumslateblue", 0xff7b68ee);
         colourKeywords.put("mediumspringgreen", 0xff00fa9a);
         colourKeywords.put("mediumturquoise", 0xff48d1cc);
         colourKeywords.put("mediumvioletred", 0xffc71585);
         colourKeywords.put("midnightblue", 0xff191970);
         colourKeywords.put("mintcream", 0xfff5fffa);
         colourKeywords.put("mistyrose", 0xffffe4e1);
         colourKeywords.put("moccasin", 0xffffe4b5);
         colourKeywords.put("navajowhite", 0xffffdead);
         colourKeywords.put("navy", 0xff000080);
         colourKeywords.put("oldlace", 0xfffdf5e6);
         colourKeywords.put("olive", 0xff808000);
         colourKeywords.put("olivedrab", 0xff6b8e23);
         colourKeywords.put("orange", 0xffffa500);
         colourKeywords.put("orangered", 0xffff4500);
         colourKeywords.put("orchid", 0xffda70d6);
         colourKeywords.put("palegoldenrod", 0xffeee8aa);
         colourKeywords.put("palegreen", 0xff98fb98);
         colourKeywords.put("paleturquoise", 0xffafeeee);
         colourKeywords.put("palevioletred", 0xffdb7093);
         colourKeywords.put("papayawhip", 0xffffefd5);
         colourKeywords.put("peachpuff", 0xffffdab9);
         colourKeywords.put("peru", 0xffcd853f);
         colourKeywords.put("pink", 0xffffc0cb);
         colourKeywords.put("plum", 0xffdda0dd);
         colourKeywords.put("powderblue", 0xffb0e0e6);
         colourKeywords.put("purple", 0xff800080);
         colourKeywords.put("rebeccapurple", 0xff663399);
         colourKeywords.put("red", 0xffff0000);
         colourKeywords.put("rosybrown", 0xffbc8f8f);
         colourKeywords.put("royalblue", 0xff4169e1);
         colourKeywords.put("saddlebrown", 0xff8b4513);
         colourKeywords.put("salmon", 0xfffa8072);
         colourKeywords.put("sandybrown", 0xfff4a460);
         colourKeywords.put("seagreen", 0xff2e8b57);
         colourKeywords.put("seashell", 0xfffff5ee);
         colourKeywords.put("sienna", 0xffa0522d);
         colourKeywords.put("silver", 0xffc0c0c0);
         colourKeywords.put("skyblue", 0xff87ceeb);
         colourKeywords.put("slateblue", 0xff6a5acd);
         colourKeywords.put("slategray", 0xff708090);
         colourKeywords.put("slategrey", 0xff708090);
         colourKeywords.put("snow", 0xfffffafa);
         colourKeywords.put("springgreen", 0xff00ff7f);
         colourKeywords.put("steelblue", 0xff4682b4);
         colourKeywords.put("tan", 0xffd2b48c);
         colourKeywords.put("teal", 0xff008080);
         colourKeywords.put("thistle", 0xffd8bfd8);
         colourKeywords.put("tomato", 0xffff6347);
         colourKeywords.put("turquoise", 0xff40e0d0);
         colourKeywords.put("violet", 0xffee82ee);
         colourKeywords.put("wheat", 0xfff5deb3);
         colourKeywords.put("white", 0xffffffff);
         colourKeywords.put("whitesmoke", 0xfff5f5f5);
         colourKeywords.put("yellow", 0xffffff00);
         colourKeywords.put("yellowgreen", 0xff9acd32);
         colourKeywords.put("transparent", 0x00000000);
      }

      static Integer get(String colourName) {
         return colourKeywords.get(colourName);
      }
   }

   private static class FontSizeKeywords {
      private static final Map<String, Length> fontSizeKeywords = new HashMap<>(9);
      static {
         fontSizeKeywords.put("xx-small", new Length(0.694f, Unit.pt));
         fontSizeKeywords.put("x-small", new Length(0.833f, Unit.pt));
         fontSizeKeywords.put("small", new Length(10.0f, Unit.pt));
         fontSizeKeywords.put("medium", new Length(12.0f, Unit.pt));
         fontSizeKeywords.put("large", new Length(14.4f, Unit.pt));
         fontSizeKeywords.put("x-large", new Length(17.3f, Unit.pt));
         fontSizeKeywords.put("xx-large", new Length(20.7f, Unit.pt));
         fontSizeKeywords.put("smaller", new Length(83.33f, Unit.percent));
         fontSizeKeywords.put("larger", new Length(120f, Unit.percent));
      }

      static Length get(String fontSize) {
         return fontSizeKeywords.get(fontSize);
      }
   }

   private static class FontWeightKeywords {
      private static final Map<String, Integer> fontWeightKeywords = new HashMap<>(13);
      static {
         fontWeightKeywords.put("normal", SVG.Style.FONT_WEIGHT_NORMAL);
         fontWeightKeywords.put("bold", SVG.Style.FONT_WEIGHT_BOLD);
         fontWeightKeywords.put("bolder", SVG.Style.FONT_WEIGHT_BOLDER);
         fontWeightKeywords.put("lighter", SVG.Style.FONT_WEIGHT_LIGHTER);
         fontWeightKeywords.put("100", 100);
         fontWeightKeywords.put("200", 200);
         fontWeightKeywords.put("300", 300);
         fontWeightKeywords.put("400", 400);
         fontWeightKeywords.put("500", 500);
         fontWeightKeywords.put("600", 600);
         fontWeightKeywords.put("700", 700);
         fontWeightKeywords.put("800", 800);
         fontWeightKeywords.put("900", 900);
      }

      static Integer get(String fontWeight) {
         return fontWeightKeywords.get(fontWeight);
      }
   }

   private static class AspectRatioKeywords {
      private static final Map<String, PreserveAspectRatio.Alignment> aspectRatioKeywords = new HashMap<>(10);
      static {
         aspectRatioKeywords.put(NONE, PreserveAspectRatio.Alignment.None);
         aspectRatioKeywords.put("xMinYMin", PreserveAspectRatio.Alignment.XMinYMin);
         aspectRatioKeywords.put("xMidYMin", PreserveAspectRatio.Alignment.XMidYMin);
         aspectRatioKeywords.put("xMaxYMin", PreserveAspectRatio.Alignment.XMaxYMin);
         aspectRatioKeywords.put("xMinYMid", PreserveAspectRatio.Alignment.XMinYMid);
         aspectRatioKeywords.put("xMidYMid", PreserveAspectRatio.Alignment.XMidYMid);
         aspectRatioKeywords.put("xMaxYMid", PreserveAspectRatio.Alignment.XMaxYMid);
         aspectRatioKeywords.put("xMinYMax", PreserveAspectRatio.Alignment.XMinYMax);
         aspectRatioKeywords.put("xMidYMax", PreserveAspectRatio.Alignment.XMidYMax);
         aspectRatioKeywords.put("xMaxYMax", PreserveAspectRatio.Alignment.XMaxYMax);
      }

      static PreserveAspectRatio.Alignment get(String aspectRatio) {
         return aspectRatioKeywords.get(aspectRatio);
      }
   }


   //=========================================================================
   // Main parser invocation methods
   //=========================================================================


   SVG  parse(InputStream is, boolean enableInternalEntities) throws SVGParseException
   {
      // Transparently handle zipped files (.svgz)
      if (!is.markSupported()) {
         // We need a a buffered stream so we can use mark() and reset()
         is = new BufferedInputStream(is);
      }
      try
      {
         is.mark(3);
         int  firstTwoBytes = is.read() + (is.read() << 8);
         is.reset();
         if (firstTwoBytes == GZIPInputStream.GZIP_MAGIC) {
            // Looks like a zipped file.
            is = new BufferedInputStream( new GZIPInputStream(is) );
         }
      }
      catch (IOException ioe)
      {
         // Not a zipped SVG. Fall through and try parsing it normally.
      }

      try
      {
         // Mark the start in case we need to restart the parsing due to switching XML parser
         // 4096 chars is hopefully enough to capture most doctype declarations that have entities.
         is.mark(ENTITY_WATCH_BUFFER_SIZE);

         // Use XmlPullParser by default, which is faster, but doesn't support entity expansion.
         // In this parser we watch for capture doctype declarations, and then switch to the SAX
         // parser if any entities are defined in the doctype.
         parseUsingXmlPullParser(is, enableInternalEntities);
      }
      finally
      {
         try {
            is.close();
         } catch (IOException e) {
            Log.e(TAG, "Exception thrown closing input stream");
         }
      }
      return svgDocument;
   }


   //=========================================================================
   // XmlPullParser parsing
   //=========================================================================


   /*
    * Implements the SAX Attributes class so that our parser can share a common attributes object
    */
   private class  XPPAttributesWrapper  implements Attributes
   {
      private XmlPullParser  parser;

      public XPPAttributesWrapper(XmlPullParser parser)
      {
         this.parser = parser;
      }

      @Override
      public int getLength()
      {
         return parser.getAttributeCount();
      }

      @Override
      public String getURI(int index)
      {
         return parser.getAttributeNamespace(index);
      }

      @Override
      public String getLocalName(int index)
      {
         return parser.getAttributeName(index);
      }

      @Override
      public String getQName(int index)
      {
         String qName = parser.getAttributeName(index);
         if (parser.getAttributePrefix(index) != null)
            qName = parser.getAttributePrefix(index) + ':' + qName;
         return qName;
      }

      @Override
      public String getValue(int index)
      {
         return parser.getAttributeValue(index);
      }

      // Not used, and not implemented
      @Override
      public String getType(int index) { return null; }
      @Override
      public int getIndex(String uri, String localName) { return -1; }
      @Override
      public int getIndex(String qName) { return -1; }
      @Override
      public String getType(String uri, String localName) { return null; }
      @Override
      public String getType(String qName) { return null; }
      @Override
      public String getValue(String uri, String localName) { return null; }
      @Override
      public String getValue(String qName) { return null; }
   };


   private void parseUsingXmlPullParser(InputStream is, boolean enableInternalEntities) throws SVGParseException
   {
      try
      {
         XmlPullParser         parser = Xml.newPullParser();
         XPPAttributesWrapper  attributes = new XPPAttributesWrapper(parser);


         parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
         parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
         parser.setInput(is, null);

         int  eventType = parser.getEventType();
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            switch(eventType) {
               case XmlPullParser.START_DOCUMENT:
                  startDocument();
                  break;
               case XmlPullParser.START_TAG:
                  String qName = parser.getName();
                  if (parser.getPrefix() != null)
                     qName = parser.getPrefix() + ':' + qName;
                  startElement(parser.getNamespace(), parser.getName(), qName, attributes);
                  break;
               case XmlPullParser.END_TAG:
                  qName = parser.getName();
                  if (parser.getPrefix() != null)
                     qName = parser.getPrefix() + ':' + qName;
                  endElement(parser.getNamespace(), parser.getName(), qName);
                  break;
               case XmlPullParser.TEXT:
                  int[] startAndLength = new int[2];
                  char[] text = parser.getTextCharacters(startAndLength);
                  text(text, startAndLength[0], startAndLength[1]);
                  break;
               //case XmlPullParser.COMMENT:
               //   text(parser.getText());
               //   break;

               case XmlPullParser.DOCDECL:
                  if (enableInternalEntities &&                  // entities are enabled
                      svgDocument.getRootElement() == null &&    // and we haven't already parsed the root element
                      parser.getText().contains("<!ENTITY ")) {  // and doctype seems to contain an entity definition
                     // File uses internal entities. Switch to the SAX parser.
                     try {
                        Log.d(TAG,"Switching to SAX parser to process entities");
                        is.reset();
                        parseUsingSAX(is);
                     } catch (IOException e) {
                        // reset() failed
                        Log.w(TAG, "Detected internal entity definitions, but could not parse them.");
                        // All we can do is just continue using the XmlPullParser.
                        // Entities will not be parsed properly :(
                     }
                     return;
                  }
            }
            eventType = parser.nextToken();
         }
         endDocument();

      }
      catch (XmlPullParserException e)
      {
         throw new SVGParseException("XML parser problem", e);
      }
      catch (IOException e)
      {
         throw new SVGParseException("Stream error", e);
      }
   }


   //=========================================================================
   // SAX parsing method and handler class
   //=========================================================================


   private void parseUsingSAX(InputStream is) throws SVGParseException
   {
      try
      {
         // Invoke the SAX XML parser on the input.
         SAXParserFactory  spf = SAXParserFactory.newInstance();

         // Disable external entity resolving
         spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
         spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

         SAXParser sp = spf.newSAXParser();
         XMLReader xr = sp.getXMLReader();

         SAXHandler  handler = new SAXHandler();
         xr.setContentHandler(handler);
         xr.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

         xr.parse(new InputSource(is));
      }
      catch (ParserConfigurationException e)
      {
         throw new SVGParseException("XML parser problem", e);
      }
      catch (SAXException e)
      {
         throw new SVGParseException("SVG parse error", e);
      }
      catch (IOException e)
      {
         throw new SVGParseException("Stream error", e);
      }
   }


   private class  SAXHandler  extends DefaultHandler2
   {
      @Override
      public void startDocument() throws SAXException
      {
         SVGParser.this.startDocument();
      }


      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
      {
         SVGParser.this.startElement(uri, localName, qName, attributes);
      }


      @Override
      public void characters(char[] ch, int start, int length) throws SAXException
      {
         SVGParser.this.text(new String(ch, start, length));
      }


      /*
      @Override
      public void comment(char[] ch, int start, int length) throws SAXException
      {
         SVGParser.this.text(new String(ch, start, length));
      }
      */


      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException
      {
         SVGParser.this.endElement(uri, localName, qName);
      }


      @Override
      public void endDocument() throws SAXException
      {
         SVGParser.this.endDocument();
      }

   }


   //=========================================================================
   // Parser event classes used by both XML parser implementations
   //=========================================================================


   public void startDocument()
   {
      SVGParser.this.svgDocument = new SVG();
   }


   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SVGParseException
   {
      if (ignoring) {
         ignoreDepth++;
         return;
      }
      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      String tag = (localName.length() > 0) ? localName : qName;

      SVGElem  elem = SVGElem.fromString(tag);
      switch (elem)
      {
         case svg:
            svg(attributes); break;
         case g:
         case a: // <a> treated like a group element
            g(attributes); break;
         case defs:
            defs(attributes); break;
         case use:
            use(attributes); break;
         case path:
            path(attributes); break;
         case rect:
            rect(attributes); break;
         case circle:
            circle(attributes); break;
         case ellipse:
            ellipse(attributes); break;
         case line:
            line(attributes); break;
         case polyline:
            polyline(attributes); break;
         case polygon:
            polygon(attributes); break;
         case text:
            text(attributes); break;
         case tspan:
            tspan(attributes); break;
         case tref:
            tref(attributes); break;
         case SWITCH:
            zwitch(attributes); break;
         case symbol:
            symbol(attributes); break;
         case marker:
            marker(attributes); break;
         case linearGradient:
            linearGradient(attributes); break;
         case radialGradient:
            radialGradient(attributes); break;
         case stop:
            stop(attributes); break;
         case title:
         case desc:
            inMetadataElement = true;
            metadataTag = elem;
            break;
         case clipPath:
            clipPath(attributes); break;
         case textPath:
            textPath(attributes); break;
         case pattern:
            pattern(attributes); break;
         case image:
            image(attributes); break;
         case view:
            view(attributes); break;
         case mask:
            mask(attributes); break;
         case style:
            style(attributes); break;
         case solidColor:
            solidColor(attributes); break;
         default:
            ignoring = true;
            ignoreDepth = 1;
            break;
      }
   }


   public void  text(String characters) throws SVGParseException
   {
      if (ignoring)
         return;

      if (inMetadataElement)
      {
         if (metadataElementContents == null)
            metadataElementContents = new StringBuilder(characters.length());
         metadataElementContents.append(characters);
      }
      else if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(characters.length());
         styleElementContents.append(characters);
      }
      else if (currentElement instanceof SVG.TextContainer)
      {
         appendToTextContainer(characters);
      }
   }


   public void  text(char[] ch, int start, int length) throws SVGParseException
   {
      if (ignoring)
         return;

      if (inMetadataElement)
      {
         if (metadataElementContents == null)
            metadataElementContents = new StringBuilder(length);
         metadataElementContents.append(ch, start, length);
      }
      else if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(length);
         styleElementContents.append(ch, start, length);
      }
      else if (currentElement instanceof SVG.TextContainer)
      {
         appendToTextContainer(new String(ch, start, length));
      }

   }


   private void  appendToTextContainer(String characters) throws SVGParseException
   {
      // The parser can pass us several text nodes in a row. If this happens, we
      // want to collapse them all into one SVG.TextSequence node
      SVG.SvgConditionalContainer  parent = (SVG.SvgConditionalContainer) currentElement;
      int  numOlderSiblings = parent.children.size();
      SVG.SvgObject  previousSibling = (numOlderSiblings == 0) ? null : parent.children.get(numOlderSiblings-1);
      if (previousSibling instanceof SVG.TextSequence) {
         // Last sibling was a TextSequence also, so merge them.
         ((SVG.TextSequence) previousSibling).text += characters;
      } else {
         // Add a new TextSequence to the child node list
         currentElement.addChild(new SVG.TextSequence( characters ));
      }
   }


   public void  endElement(String uri, String localName, String qName) throws SVGParseException
   {
      if (ignoring) {
         if (--ignoreDepth == 0) {
            ignoring = false;
            return;
         }
      }

      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      String tag = (localName.length() > 0) ? localName : qName;
      switch (SVGElem.fromString(tag))
      {
         case title:
         case desc:
            inMetadataElement = false;
            if (metadataElementContents != null)
            {
               if (metadataTag == SVGElem.title)
                  svgDocument.setTitle(metadataElementContents.toString());
               else if (metadataTag == SVGElem.desc)
                  svgDocument.setDesc(metadataElementContents.toString());
               metadataElementContents.setLength(0);
            }
            return;

         case style:
            if (styleElementContents != null) {
               inStyleElement = false;
               parseCSSStyleSheet(styleElementContents.toString());
               styleElementContents.setLength(0);
               return;
            }
            break;

         case svg:
         case defs:
         case g:
         case use:
         case image:
         case text:
         case tspan:
         case SWITCH:
         case symbol:
         case marker:
         case linearGradient:
         case radialGradient:
         case stop:
         case clipPath:
         case textPath:
         case pattern:
         case view:
         case mask:
         case solidColor:
            currentElement = ((SvgObject) currentElement).parent;
            break;

         default:
            // no action
      }

   }


   public void  endDocument()
   {
      // Dump document
      if (LibConfig.DEBUG)
         dumpNode(svgDocument.getRootElement(), "");
   }


   //=========================================================================


   private void  dumpNode(SVG.SvgObject elem, String indent)
   {
      Log.d(TAG, indent+elem);
      if (elem instanceof SVG.SvgConditionalContainer) {
         indent = indent+"  ";
         for (SVG.SvgObject child: ((SVG.SvgConditionalContainer) elem).children) {
            dumpNode(child, indent);
         }
      }
   }


   private void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }


   //=========================================================================
   // Handlers for each SVG element
   //=========================================================================
   // <svg> element

   private void  svg(Attributes attributes) throws SVGParseException
   {
      debug("<svg>");

      SVG.Svg  obj = new SVG.Svg();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesSVG(obj, attributes);
      if (currentElement == null) {
         svgDocument.setRootElement(obj);
      } else {
         currentElement.addChild(obj);
      }
      currentElement = obj;
   }

   
   private void  parseAttributesSVG(SVG.Svg obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <svg> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <svg> element. height cannot be negative");
               break;
            case version:
               obj.version = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <g> group element


   private void  g(Attributes attributes) throws SVGParseException
   {
      debug("<g>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Group  obj = new SVG.Group();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <defs> group element


   private void  defs(Attributes attributes) throws SVGParseException
   {
      debug("<defs>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Defs  obj = new SVG.Defs();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <use> group element


   private void  use(Attributes attributes) throws SVGParseException
   {
      debug("<use>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Use  obj = new SVG.Use();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesUse(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesUse(SVG.Use obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <image> element


   private void  image(Attributes attributes) throws SVGParseException
   {
      debug("<image>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Image  obj = new SVG.Image();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesImage(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesImage(SVG.Image obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            case preserveAspectRatio:
               parsePreserveAspectRatio(obj, val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <path> element


   private void  path(Attributes attributes) throws SVGParseException
   {
      debug("<path>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Path  obj = new SVG.Path();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPath(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesPath(SVG.Path obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case d:
               obj.d = parsePath(val);
               break;
            case pathLength:
               obj.pathLength = parseFloat(val);
               if (obj.pathLength < 0f)
                  throw new SVGParseException("Invalid <path> element. pathLength cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <rect> element


   private void  rect(Attributes attributes) throws SVGParseException
   {
      debug("<rect>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Rect  obj = new SVG.Rect();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesRect(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesRect(SVG.Rect obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <rect> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <rect> element. height cannot be negative");
               break;
            case rx:
               obj.rx = parseLength(val);
               if (obj.rx.isNegative())
                  throw new SVGParseException("Invalid <rect> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SVGParseException("Invalid <rect> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <circle> element


   private void  circle(Attributes attributes) throws SVGParseException
   {
      debug("<circle>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Circle  obj = new SVG.Circle();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesCircle(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesCircle(SVG.Circle obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case r:
               obj.r = parseLength(val);
               if (obj.r.isNegative())
                  throw new SVGParseException("Invalid <circle> element. r cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <ellipse> element


   private void  ellipse(Attributes attributes) throws SVGParseException
   {
      debug("<ellipse>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Ellipse  obj = new SVG.Ellipse();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesEllipse(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesEllipse(SVG.Ellipse obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case rx:
               obj.rx = parseLength(val);
               if (obj.rx.isNegative())
                  throw new SVGParseException("Invalid <ellipse> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SVGParseException("Invalid <ellipse> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <line> element


   private void  line(Attributes attributes) throws SVGParseException
   {
      debug("<line>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Line  obj = new SVG.Line();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesLine(obj, attributes);
      currentElement.addChild(obj);     
   }


   private void  parseAttributesLine(SVG.Line obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x1:
               obj.x1 = parseLength(val);
               break;
            case y1:
               obj.y1 = parseLength(val);
               break;
            case x2:
               obj.x2 = parseLength(val);
               break;
            case y2:
               obj.y2 = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <polyline> element


   private void  polyline(Attributes attributes) throws SVGParseException
   {
      debug("<polyline>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.PolyLine  obj = new SVG.PolyLine();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPolyLine(obj, attributes, "polyline");
      currentElement.addChild(obj);     
   }


   /*
    *  Parse the "points" attribute. Used by both <polyline> and <polygon>.
    */
   private void  parseAttributesPolyLine(SVG.PolyLine obj, Attributes attributes, String tag) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.points)
         {
            TextScanner scan = new TextScanner(attributes.getValue(i));
            List<Float> points = new ArrayList<>();
            scan.skipWhitespace();

            while (!scan.empty()) {
               float x = scan.nextFloat();
               if (Float.isNaN(x))
                  throw new SVGParseException("Invalid <"+tag+"> points attribute. Non-coordinate content found in list.");
               scan.skipCommaWhitespace();
               float y = scan.nextFloat();
               if (Float.isNaN(y))
                  throw new SVGParseException("Invalid <"+tag+"> points attribute. There should be an even number of coordinates.");
               scan.skipCommaWhitespace();
               points.add(x);
               points.add(y);
            }
            obj.points = new float[points.size()];
            int j = 0;
            for (float f: points) {
               obj.points[j++] = f;
            }
         }
      }
   }


   //=========================================================================
   // <polygon> element


   private void  polygon(Attributes attributes) throws SVGParseException
   {
      debug("<polygon>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Polygon  obj = new SVG.Polygon();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesPolyLine(obj, attributes, "polygon"); // reuse of polyline "points" parser
      currentElement.addChild(obj);     
   }


   //=========================================================================
   // <text> element


   private void  text(Attributes attributes) throws SVGParseException
   {
      debug("<text>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Text  obj = new SVG.Text();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPosition(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesTextPosition(TextPositionedContainer obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x:
               obj.x = parseLengthList(val);
               break;
            case y:
               obj.y = parseLengthList(val);
               break;
            case dx:
               obj.dx = parseLengthList(val);
               break;
            case dy:
               obj.dy = parseLengthList(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <tspan> element


   private void  tspan(Attributes attributes) throws SVGParseException
   {
      debug("<tspan>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.TextContainer))
         throw new SVGParseException("Invalid document. <tspan> elements are only valid inside <text> or other <tspan> elements.");
      SVG.TSpan  obj = new SVG.TSpan();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPosition(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   //=========================================================================
   // <tref> element


   private void  tref(Attributes attributes) throws SVGParseException
   {
      debug("<tref>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.TextContainer))
         throw new SVGParseException("Invalid document. <tref> elements are only valid inside <text> or <tspan> elements.");
      SVG.TRef  obj = new SVG.TRef();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTRef(obj, attributes);
      currentElement.addChild(obj);
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTRef(SVG.TRef obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <switch> element


   private void  zwitch(Attributes attributes) throws SVGParseException
   {
      debug("<switch>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Switch  obj = new SVG.Switch();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesConditional(SVG.SvgConditional obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case requiredFeatures:
               obj.setRequiredFeatures(parseRequiredFeatures(val));
               break;
            case requiredExtensions:
               obj.setRequiredExtensions(val);
               break;
            case systemLanguage:
               obj.setSystemLanguage(parseSystemLanguage(val));
               break;
            case requiredFormats:
               obj.setRequiredFormats(parseRequiredFormats(val));
               break;
            case requiredFonts:
               List<String>  fonts = parseFontFamily(val);
               Set<String>  fontSet = (fonts != null) ? new HashSet<>(fonts) : new HashSet<String>(0);
               obj.setRequiredFonts(fontSet);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <symbol> element


   private void  symbol(Attributes attributes) throws SVGParseException
   {
      debug("<symbol>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Symbol  obj = new SVG.Symbol();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }

   
   //=========================================================================
   // <marker> element


   private void  marker(Attributes attributes) throws SVGParseException
   {
      debug("<marker>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Marker  obj = new SVG.Marker();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesMarker(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesMarker(SVG.Marker obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case refX:
               obj.refX = parseLength(val);
               break;
            case refY:
               obj.refY = parseLength(val);
               break;
            case markerWidth:
               obj.markerWidth = parseLength(val);
               if (obj.markerWidth.isNegative())
                  throw new SVGParseException("Invalid <marker> element. markerWidth cannot be negative");
               break;
            case markerHeight:
               obj.markerHeight = parseLength(val);
               if (obj.markerHeight.isNegative())
                  throw new SVGParseException("Invalid <marker> element. markerHeight cannot be negative");
               break;
            case markerUnits:
               if ("strokeWidth".equals(val)) {
                  obj.markerUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.markerUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute markerUnits");
               } 
               break;
            case orient:
               if ("auto".equals(val)) {
                  obj.orient = Float.NaN;
               } else {
                  obj.orient = parseFloat(val);
               }
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <linearGradient> element


   private void  linearGradient(Attributes attributes) throws SVGParseException
   {
      debug("<linearGradient>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.SvgLinearGradient  obj = new SVG.SvgLinearGradient();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesGradient(obj, attributes);
      parseAttributesLinearGradient(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesGradient(SVG.GradientElement obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case gradientUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.gradientUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.gradientUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute gradientUnits");
               } 
               break;
            case gradientTransform:
               obj.gradientTransform = parseTransformList(val);
               break;
            case spreadMethod:
               try
               {
                  obj.spreadMethod = GradientSpread.valueOf(val);
               } 
               catch (IllegalArgumentException e)
               {
                  throw new SVGParseException("Invalid spreadMethod attribute. \""+val+"\" is not a valid value.");
               }
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   private void  parseAttributesLinearGradient(SVG.SvgLinearGradient obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case x1:
               obj.x1 = parseLength(val);
               break;
            case y1:
               obj.y1 = parseLength(val);
               break;
            case x2:
               obj.x2 = parseLength(val);
               break;
            case y2:
               obj.y2 = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <radialGradient> element


   private void  radialGradient(Attributes attributes) throws SVGParseException
   {
      debug("<radialGradient>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.SvgRadialGradient  obj = new SVG.SvgRadialGradient();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesGradient(obj, attributes);
      parseAttributesRadialGradient(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesRadialGradient(SVG.SvgRadialGradient obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case cx:
               obj.cx = parseLength(val);
               break;
            case cy:
               obj.cy = parseLength(val);
               break;
            case r:
               obj.r = parseLength(val);
               if (obj.r.isNegative())
                  throw new SVGParseException("Invalid <radialGradient> element. r cannot be negative");
               break;
            case fx:
               obj.fx = parseLength(val);
               break;
            case fy:
               obj.fy = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // Gradient <stop> element


   private void  stop(Attributes attributes) throws SVGParseException
   {
      debug("<stop>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.GradientElement))
         throw new SVGParseException("Invalid document. <stop> elements are only valid inside <linearGradient> or <radialGradient> elements.");
      SVG.Stop  obj = new SVG.Stop();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesStop(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesStop(SVG.Stop obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case offset:
               obj.offset = parseGradientOffset(val);
               break;
            default:
               break;
         }
      }
   }


   private Float  parseGradientOffset(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid offset value in <stop> (empty string)");
      int      end = val.length();
      boolean  isPercent = false;

      if (val.charAt(val.length()-1) == '%') {
         end -= 1;
         isPercent = true;
      }
      try
      {
         float scalar = parseFloat(val, 0, end);
         if (isPercent)
            scalar /= 100f;
         return (scalar < 0) ? 0 : (scalar > 100) ? 100 : scalar;
      }
      catch (NumberFormatException e)
      {
         throw new SVGParseException("Invalid offset value in <stop>: "+val, e);
      }
   }


   //=========================================================================
   // <solidColor> element


   private void  solidColor(Attributes attributes) throws SVGParseException
   {
      debug("<solidColor>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.SolidColor  obj = new SVG.SolidColor();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <clipPath> element


   private void  clipPath(Attributes attributes) throws SVGParseException
   {
      debug("<clipPath>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.ClipPath  obj = new SVG.ClipPath();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesClipPath(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesClipPath(SVG.ClipPath obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case clipPathUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.clipPathUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.clipPathUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute clipPathUnits");
               }
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <textPath> element


   private void textPath(Attributes attributes) throws SVGParseException
   {
      debug("<textPath>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.TextPath  obj = new SVG.TextPath();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPath(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof TextRoot)
         obj.setTextRoot((TextRoot) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTextPath(SVG.TextPath obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            case startOffset:
               obj.startOffset = parseLength(val);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <pattern> element


   private void pattern(Attributes attributes) throws SVGParseException
   {
      debug("<pattern>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Pattern  obj = new SVG.Pattern();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      parseAttributesPattern(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesPattern(SVG.Pattern obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case patternUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.patternUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.patternUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute patternUnits");
               } 
               break;
            case patternContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.patternContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.patternContentUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute patternContentUnits");
               } 
               break;
            case patternTransform:
               obj.patternTransform = parseTransformList(val);
               break;
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <pattern> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <pattern> element. height cannot be negative");
               break;
            case href:
               if ("".equals(attributes.getURI(i)) || XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <view> element


   private void  view(Attributes attributes) throws SVGParseException
   {
      debug("<view>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.View  obj = new SVG.View();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesViewBox(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }

   
   //=========================================================================
   // <mask> element


   private void mask(Attributes attributes) throws SVGParseException
   {
      debug("<mask>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");
      SVG.Mask  obj = new SVG.Mask();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesMask(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesMask(SVG.Mask obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case maskUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.maskUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.maskUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute maskUnits");
               } 
               break;
            case maskContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.maskContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.maskContentUnitsAreUser = true;
               } else {
                  throw new SVGParseException("Invalid value for attribute maskContentUnits");
               } 
               break;
            case x:
               obj.x = parseLength(val);
               break;
            case y:
               obj.y = parseLength(val);
               break;
            case width:
               obj.width = parseLength(val);
               if (obj.width.isNegative())
                  throw new SVGParseException("Invalid <mask> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SVGParseException("Invalid <mask> element. height cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // String tokeniser
   //=========================================================================


   static class TextScanner
   {
      String   input;
      int      position = 0;
      int      inputLength = 0;

      private   NumberParser  numberParser = new NumberParser();


      TextScanner(String input)
      {
         this.input = input.trim();
         this.inputLength = this.input.length();
      }

      /**
       * Returns true if we have reached the end of the input.
       */
      boolean  empty()
      {
         return (position == inputLength);
      }

      boolean  isWhitespace(int c)
      {
         return (c==' ' || c=='\n' || c=='\r' || c =='\t');
      }

      void  skipWhitespace()
      {
         while (position < inputLength) {
            if (!isWhitespace(input.charAt(position)))
               break;
            position++;
         }
      }

      boolean  isEOL(int c)
      {
         return (c=='\n' || c=='\r');
      }

      // Skip the sequence: <space>*(<comma><space>)?
      // Returns true if we found a comma in there.
      boolean  skipCommaWhitespace()
      {
         skipWhitespace();
         if (position == inputLength)
            return false;
         if (!(input.charAt(position) == ','))
            return false;
         position++;
         skipWhitespace();
         return true;
      }


      float  nextFloat()
      {
         float  val = numberParser.parseNumber(input, position, inputLength);
         if (!Float.isNaN(val))
            position = numberParser.getEndPos();
         return val;
      }

      /*
       * Scans for a comma-whitespace sequence with a float following it.
       * If found, the float is returned. Otherwise null is returned and
       * the scan position left as it was.
       */
      float  possibleNextFloat()
      {
         skipCommaWhitespace();
         float  val = numberParser.parseNumber(input, position, inputLength);
         if (!Float.isNaN(val))
            position = numberParser.getEndPos();
         return val;
      }

      /*
       * Scans for comma-whitespace sequence with a float following it.
       * But only if the provided 'lastFloat' (representing the last coord
       * scanned was non-null (ie parsed correctly).
       */
      float  checkedNextFloat(float lastRead)
      {
         if (Float.isNaN(lastRead)) {
            return Float.NaN;
         }
         skipCommaWhitespace();
         return nextFloat();
      }

      float  checkedNextFloat(Boolean lastRead)
      {
         if (lastRead == null) {
            return Float.NaN;
         }
         skipCommaWhitespace();
         return nextFloat();
      }

      /*
      public Integer  nextInteger()
      {
         IntegerParser  ip = IntegerParser.parseInt(input, position, inputLength);
         if (ip == null)
            return null;
         position = ip.getEndPos();
         return ip.value();
      }
      */

      Integer  nextChar()
      {
         if (position == inputLength)
            return null;
         return (int) input.charAt(position++);
      }

      Length  nextLength()
      {
         float  scalar = nextFloat();
         if (Float.isNaN(scalar))
            return null;
         Unit  unit = nextUnit();
         if (unit == null)
            return new Length(scalar, Unit.px);
         else
            return new Length(scalar, unit);
      }

      /*
       * Scan for a 'flag'. A flag is a '0' or '1' digit character.
       */
      Boolean  nextFlag()
      {
         if (position == inputLength)
            return null;
         char  ch = input.charAt(position);
         if (ch == '0' || ch == '1') {
            position++;
            return (ch == '1');
         }
         return null;
      }

      /*
       * Like checkedNextFloat, but reads a flag (see path definition parser)
       */
      Boolean  checkedNextFlag(Object lastRead)
      {
         if (lastRead == null) {
            return null;
         }
         skipCommaWhitespace();
         return nextFlag();
      }

      boolean  consume(char ch)
      {
         boolean  found = (position < inputLength && input.charAt(position) == ch);
         if (found)
            position++;
         return found;
      }


      boolean  consume(String str)
      {
         int  len = str.length();
         boolean  found = (position <= (inputLength - len) && input.substring(position,position+len).equals(str));
         if (found)
            position += len;
         return found;
      }


      int  advanceChar()
      {
         if (position == inputLength)
            return -1;
         position++;
         if (position < inputLength)
            return input.charAt(position);
         else
            return -1;
      }


      /*
       * Scans the input starting immediately at 'position' for the next token.
       * A token is a sequence of characters terminating at a whitespace character.
       * Note that this routine only checks for whitespace characters.  Use nextToken(char)
       * if token might end with another character.
       */
      String  nextToken()
      {
         return nextToken(' ', false);
      }

      /*
       * Scans the input starting immediately at 'position' for the next token.
       * A token is a sequence of characters terminating at either a whitespace character
       * or the supplied terminating character.
       */
      String  nextToken(char terminator)
      {
         return nextToken(terminator, false);
      }

      /*
       * Scans the input starting immediately at 'position' for the next token.
       * A token is a sequence of characters terminating at either a the supplied terminating
       * character.  Whitespaces are allowed.
       */
      String  nextTokenWithWhitespace(char terminator)
      {
         return nextToken(terminator, true);
      }

      /*
       * Scans the input starting immediately at 'position' for the next token.
       * A token is a sequence of characters terminating at either the supplied terminating
       * character, or (optionally) a whitespace character.
       */
      String  nextToken(char terminator, boolean allowWhitespace)
      {
         if (empty())
            return null;

         int  ch = input.charAt(position);
         if ((!allowWhitespace && isWhitespace(ch)) || ch == terminator)
            return null;
         
         int  start = position;
         ch = advanceChar();
         while (ch != -1) {
            if (ch == terminator)
               break;
            if (!allowWhitespace && isWhitespace(ch))
               break;
            ch = advanceChar();
         }
         return input.substring(start, position);
      }

      /*
       * Scans the input starting immediately at 'position' for the a sequence
       * of letter characters terminated by an open bracket.  The function
       * name is returned.
       */
      String  nextFunction()
      {
         if (empty())
            return null;
         int  start = position;

         int  ch = input.charAt(position);
         while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))
            ch = advanceChar();
         int end = position;
         while (isWhitespace(ch))
            ch = advanceChar();
         if (ch == '(') {
            position++;
            return input.substring(start, end);
         }
         position = start;
         return null;
      }

      /*
       * Get the next few chars. Mainly used for error messages.
       */
      String  ahead()
      {
         int start = position;
         while (!empty() && !isWhitespace(input.charAt(position)))
            position++;
         String  str = input.substring(start, position);
         position = start;
         return str;
      }

      Unit  nextUnit()
      {
         if (empty())
            return null;
         int  ch = input.charAt(position);
         if (ch == '%') {
            position++;
            return Unit.percent;
         }
         if (position > (inputLength - 2))
            return null;
         try {
            Unit  result = Unit.valueOf(input.substring(position, position + 2).toLowerCase(Locale.US));
            position +=2;
            return result;
         } catch (IllegalArgumentException e) {
            return null;
         }
      }

      /*
       * Check whether the next character is a letter.
       */
      boolean  hasLetter()
      {
         if (position == inputLength)
            return false;
         char  ch = input.charAt(position);
         return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
      }

      /*
       * Extract a quoted string from the input.
       */
      String  nextQuotedString()
      {
         if (empty())
            return null;
         int  start = position;
         int  ch = input.charAt(position);
         int  endQuote = ch;
         if (ch != '\'' && ch!='"')
            return null;
         ch = advanceChar();
         while (ch != -1 && ch != endQuote)
            ch = advanceChar();
         if (ch == -1) {
            position = start;
            return null;
         }
         position++;
         return input.substring(start+1, position-1);
      }

      /*
       * Return the remaining input as a string.
       */
      String  restOfText()
      {
         if (empty())
            return null;

         int  start = position;
         position = inputLength;
         return input.substring(start);
      }

   }


   //=========================================================================
   // Attribute parsing
   //=========================================================================


   private void  parseAttributesCore(SvgElementBase obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String  qname = attributes.getQName(i);
         if (qname.equals("id") || qname.equals("xml:id"))
         {
            obj.id = attributes.getValue(i).trim();
            break;
         }
         else if (qname.equals("xml:space")) {
            String  val = attributes.getValue(i).trim();
            if ("default".equals(val)) {
               obj.spacePreserve = Boolean.FALSE;
            } else if ("preserve".equals(val)) {
               obj.spacePreserve = Boolean.TRUE;
            } else {
               throw new SVGParseException("Invalid value for \"xml:space\" attribute: "+val);
            }
            break;
         }
      }
   }


   /*
    * Parse the style attributes for an element.
    */
   private void  parseAttributesStyle(SvgElementBase obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String  val = attributes.getValue(i).trim();
         if (val.length() == 0) { // The spec doesn't say how to handle empty style attributes.
            continue;             // Our strategy is just to ignore them.
         }
         //boolean  inherit = val.equals("inherit");

         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case style:
               parseStyle(obj, val);
               break;

            case CLASS:
               obj.classNames = CSSParser.parseClassAttribute(val);
               break;

            default:
               if (obj.baseStyle == null)
                  obj.baseStyle = new Style();
               processStyleProperty(obj.baseStyle, attributes.getLocalName(i), attributes.getValue(i).trim());
               break;
         }
      }
   }


   /*
    * Parse the 'style' attribute.
    */
   private static void  parseStyle(SvgElementBase obj, String style) throws SVGParseException
   {
      TextScanner  scan = new TextScanner(style.replaceAll("/\\*.*?\\*/", ""));  // regex strips block comments

      while (true)
      {
         String  propertyName = scan.nextToken(':');
         scan.skipWhitespace();
         if (!scan.consume(':'))
            break;  // Syntax error. Stop processing CSS rules.
         scan.skipWhitespace();
         String  propertyValue = scan.nextTokenWithWhitespace(';');
         if (propertyValue == null)
            break;  // Syntax error
         scan.skipWhitespace();
         if (scan.empty() || scan.consume(';'))
         {
            if (obj.style == null)
               obj.style = new Style();
            processStyleProperty(obj.style, propertyName, propertyValue);
            scan.skipWhitespace();
         }
      }
   }


   static void  processStyleProperty(Style style, String localName, String val) throws SVGParseException
   {
      if (val.length() == 0) { // The spec doesn't say how to handle empty style attributes.
         return;               // Our strategy is just to ignore them.
      }
      if (val.equals("inherit"))
         return;

      switch (SVGAttr.fromString(localName))
      {
         case fill:
            try {
               style.fill = parsePaintSpecifier(val, "fill");
               style.specifiedFlags |= SVG.SPECIFIED_FILL;
            } catch (SVGParseException e) {
               // Error: Ignore property
               Log.w(TAG, e.getMessage());
            }
            break;

         case fill_rule:
            style.fillRule = parseFillRule(val);
            style.specifiedFlags |= SVG.SPECIFIED_FILL_RULE;
            break;

         case fill_opacity:
            style.fillOpacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_FILL_OPACITY;
            break;

         case stroke:
            try {
               style.stroke = parsePaintSpecifier(val, "stroke");
               style.specifiedFlags |= SVG.SPECIFIED_STROKE;
            } catch (SVGParseException e) {
               // Error: Ignore property
               Log.w(TAG, e.getMessage());
            }
            break;

         case stroke_opacity:
            style.strokeOpacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_OPACITY;
            break;

         case stroke_width:
            style.strokeWidth = parseLength(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_WIDTH;
            break;

         case stroke_linecap:
            style.strokeLineCap = parseStrokeLineCap(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_LINECAP;
            break;

         case stroke_linejoin:
            style.strokeLineJoin = parseStrokeLineJoin(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_LINEJOIN;
            break;

         case stroke_miterlimit:
            style.strokeMiterLimit = parseFloat(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_MITERLIMIT;
            break;

         case stroke_dasharray:
            if (NONE.equals(val))
               style.strokeDashArray = null;
            else
               style.strokeDashArray = parseStrokeDashArray(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_DASHARRAY;
            break;

         case stroke_dashoffset:
            style.strokeDashOffset = parseLength(val);
            style.specifiedFlags |= SVG.SPECIFIED_STROKE_DASHOFFSET;
            break;

         case opacity:
            style.opacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_OPACITY;
            break;

         case color:
            try {
               style.color = parseColour(val);
               style.specifiedFlags |= SVG.SPECIFIED_COLOR;
            } catch (SVGParseException e) {
               // Error: Ignore property
               Log.w(TAG, e.getMessage());
            }
            break;

         case font:
            parseFont(style, val);
            break;

         case font_family:
            style.fontFamily = parseFontFamily(val);
            style.specifiedFlags |= SVG.SPECIFIED_FONT_FAMILY;
            break;

         case font_size:
            style.fontSize = parseFontSize(val);
            style.specifiedFlags |= SVG.SPECIFIED_FONT_SIZE;
            break;

         case font_weight:
            style.fontWeight = parseFontWeight(val);
            style.specifiedFlags |= SVG.SPECIFIED_FONT_WEIGHT;
            break;

         case font_style:
            style.fontStyle = parseFontStyle(val);
            style.specifiedFlags |= SVG.SPECIFIED_FONT_STYLE;
            break;

         case text_decoration:
            style.textDecoration = parseTextDecoration(val);
            style.specifiedFlags |= SVG.SPECIFIED_TEXT_DECORATION;
            break;

         case direction:
            style.direction = parseTextDirection(val);
            style.specifiedFlags |= SVG.SPECIFIED_DIRECTION;
            break;

         case text_anchor:
            style.textAnchor = parseTextAnchor(val);
            style.specifiedFlags |= SVG.SPECIFIED_TEXT_ANCHOR;
            break;

         case overflow:
            style.overflow = parseOverflow(val);
            style.specifiedFlags |= SVG.SPECIFIED_OVERFLOW;
            break;

         case marker:
            style.markerStart = parseFunctionalIRI(val, localName);
            style.markerMid = style.markerStart;
            style.markerEnd = style.markerStart;
            style.specifiedFlags |= (SVG.SPECIFIED_MARKER_START | SVG.SPECIFIED_MARKER_MID | SVG.SPECIFIED_MARKER_END);
            break;

         case marker_start:
            style.markerStart = parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_MARKER_START;
            break;

         case marker_mid:
            style.markerMid = parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_MARKER_MID;
            break;

         case marker_end:
            style.markerEnd = parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_MARKER_END;
            break;

         case display:
            if (val.indexOf('|') >= 0 || !VALID_DISPLAY_VALUES.contains('|'+val+'|'))
               throw new SVGParseException("Invalid value for \"display\" attribute: "+val);
            style.display = !val.equals(NONE);
            style.specifiedFlags |= SVG.SPECIFIED_DISPLAY;
            break;

         case visibility:
            if (val.indexOf('|') >= 0 || !VALID_VISIBILITY_VALUES.contains('|'+val+'|'))
               throw new SVGParseException("Invalid value for \"visibility\" attribute: "+val);
            style.visibility = val.equals("visible");
            style.specifiedFlags |= SVG.SPECIFIED_VISIBILITY;
            break;

         case stop_color:
            if (val.equals(CURRENTCOLOR)) {
               style.stopColor = CurrentColor.getInstance();
            } else {
               try {
                  style.stopColor = parseColour(val);
               } catch (SVGParseException e) {
                  // Error: Ignore property
                  Log.w(TAG, e.getMessage());
                  break;
               }
            }
            style.specifiedFlags |= SVG.SPECIFIED_STOP_COLOR;
            break;

         case stop_opacity:
            style.stopOpacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_STOP_OPACITY;
            break;

         case clip:
            style.clip = parseClip(val);
            style.specifiedFlags |= SVG.SPECIFIED_CLIP;
            break;

         case clip_path:
            style.clipPath = parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_CLIP_PATH;
            break;

         case clip_rule:
            style.clipRule = parseFillRule(val);
            style.specifiedFlags |= SVG.SPECIFIED_CLIP_RULE;
            break;

         case mask:
            style.mask = parseFunctionalIRI(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_MASK;
            break;

         case solid_color:
            if (val.equals(CURRENTCOLOR)) {
               style.solidColor = CurrentColor.getInstance();
            } else {
               try {
                  style.solidColor = parseColour(val);
               } catch (SVGParseException e) {
                  // Error: Ignore property
                  Log.w(TAG, e.getMessage());
                  break;
               }
            }
            style.specifiedFlags |= SVG.SPECIFIED_SOLID_COLOR;
            break;

         case solid_opacity:
            style.solidOpacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_SOLID_OPACITY;
            break;

         case viewport_fill:
            if (val.equals(CURRENTCOLOR)) {
               style.viewportFill = CurrentColor.getInstance();
            } else {
               try {
                  style.viewportFill = parseColour(val);
               } catch (SVGParseException e) {
                  // Error: Ignore property
                  Log.w(TAG, e.getMessage());
                  break;
               }
            }
            style.specifiedFlags |= SVG.SPECIFIED_VIEWPORT_FILL;
            break;

         case viewport_fill_opacity:
            style.viewportFillOpacity = parseOpacity(val);
            style.specifiedFlags |= SVG.SPECIFIED_VIEWPORT_FILL_OPACITY;
            break;

         case vector_effect:
            style.vectorEffect = parseVectorEffect(val);
            style.specifiedFlags |= SVG.SPECIFIED_VECTOR_EFFECT;
            break;

         case image_rendering:
            style.imageRendering = parseRenderQuality(val, localName);
            style.specifiedFlags |= SVG.SPECIFIED_IMAGE_RENDERING;
            break;

         default:
            break;
      }
   }


   private void  parseAttributesViewBox(SVG.SvgViewBoxContainer obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case viewBox:
               obj.viewBox = parseViewBox(val);
               break;
            case preserveAspectRatio:
               parsePreserveAspectRatio(obj, val);
               break;
            default:
               break;
         }
      }
   }


   private void  parseAttributesTransform(SVG.HasTransform obj, Attributes attributes) throws SVGParseException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.transform)
         {
            obj.setTransform( parseTransformList(attributes.getValue(i)) );
         }
      }
   }


   private Matrix  parseTransformList(String val) throws SVGParseException
   {
      Matrix  matrix = new Matrix();

      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         String  cmd = scan.nextFunction();

         if (cmd == null)
            throw new SVGParseException("Bad transform function encountered in transform list: "+val);

         switch (cmd) {
            case "matrix":
               scan.skipWhitespace();
               float a = scan.nextFloat();
               scan.skipCommaWhitespace();
               float b = scan.nextFloat();
               scan.skipCommaWhitespace();
               float c = scan.nextFloat();
               scan.skipCommaWhitespace();
               float d = scan.nextFloat();
               scan.skipCommaWhitespace();
               float e = scan.nextFloat();
               scan.skipCommaWhitespace();
               float f = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(f) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               Matrix m = new Matrix();
               m.setValues(new float[]{a, c, e, b, d, f, 0, 0, 1});
               matrix.preConcat(m);
               break;

            case "translate":
               scan.skipWhitespace();
               float tx = scan.nextFloat();
               float ty = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(tx) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(ty))
                  matrix.preTranslate(tx, 0f);
               else
                  matrix.preTranslate(tx, ty);
               break;

            case "scale":
               scan.skipWhitespace();
               float sx = scan.nextFloat();
               float sy = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(sx) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(sy))
                  matrix.preScale(sx, sx);
               else
                  matrix.preScale(sx, sy);
               break;

            case "rotate": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               float cx = scan.possibleNextFloat();
               float cy = scan.possibleNextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               if (Float.isNaN(cx)) {
                  matrix.preRotate(ang);
               } else if (!Float.isNaN(cy)) {
                  matrix.preRotate(ang, cx, cy);
               } else {
                  throw new SVGParseException("Invalid transform list: " + val);
               }
               break;
            }

            case "skewX": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               matrix.preSkew((float) Math.tan(Math.toRadians(ang)), 0f);
               break;
            }

            case "skewY": {
               scan.skipWhitespace();
               float ang = scan.nextFloat();
               scan.skipWhitespace();

               if (Float.isNaN(ang) || !scan.consume(')'))
                  throw new SVGParseException("Invalid transform list: " + val);

               matrix.preSkew(0f, (float) Math.tan(Math.toRadians(ang)));
               break;
            }

            default:
               throw new SVGParseException("Invalid transform list fn: " + cmd + ")");
         }

         if (scan.empty())
            break;
         scan.skipCommaWhitespace();
      }

      return matrix;
   }


   //=========================================================================
   // Parsing various SVG value types
   //=========================================================================


   /*
    * Parse an SVG 'Length' value (usually a coordinate).
    * Spec says: length ::= number ("em" | "ex" | "px" | "in" | "cm" | "mm" | "pt" | "pc" | "%")?
    */
   static Length  parseLength(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid length value (empty string)");
      int   end = val.length();
      Unit  unit = Unit.px;
      char  lastChar = val.charAt(end-1);

      if (lastChar == '%') {
         end -= 1;
         unit = Unit.percent;
      } else if (end > 2 && Character.isLetter(lastChar) && Character.isLetter(val.charAt(end-2))) {
         end -= 2;
         String unitStr = val.substring(end);
         try {
            unit = Unit.valueOf(unitStr.toLowerCase(Locale.US));
         } catch (IllegalArgumentException e) {
            throw new SVGParseException("Invalid length unit specifier: "+val);
         }
      }
      try
      {
         float scalar = parseFloat(val, 0, end);
         return new Length(scalar, unit);
      }
      catch (NumberFormatException e)
      {
         throw new SVGParseException("Invalid length value: "+val, e);
      }
   }


   /*
    * Parse a list of Length/Coords
    */
   private static List<Length>  parseLengthList(String val) throws SVGParseException
   {
      if (val.length() == 0)
         throw new SVGParseException("Invalid length list (empty string)");

      List<Length>  coords = new ArrayList<>(1);

      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         float scalar = scan.nextFloat();
         if (Float.isNaN(scalar))
            throw new SVGParseException("Invalid length list value: "+scan.ahead());
         Unit  unit = scan.nextUnit();
         if (unit == null)
            unit = Unit.px;
         coords.add(new Length(scalar, unit));
         scan.skipCommaWhitespace();
      }
      return coords;
   }


   /*
    * Parse a generic float value.
    */
   private static float  parseFloat(String val) throws SVGParseException
   {
      int  len = val.length();
      if (len == 0)
         throw new SVGParseException("Invalid float value (empty string)");
      return parseFloat(val, 0, len);
   }

   private static float  parseFloat(String val, int offset, int len) throws SVGParseException
   {
      NumberParser np = new NumberParser();
      float  num = np.parseNumber(val, offset, len);
      if (!Float.isNaN(num)) {
         return num;
      } else {
         throw new SVGParseException("Invalid float value: "+val);
      }
   }


   /*
    * Parse an opacity value (a float clamped to the range 0..1).
    */
   private static float  parseOpacity(String val) throws SVGParseException
   {
      float  o = parseFloat(val);
      return (o < 0f) ? 0f : (o > 1f) ? 1f : o;
   }


   /*
    * Parse a viewBox attribute.
    */
   private static Box  parseViewBox(String val) throws SVGParseException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      float minX = scan.nextFloat();
      scan.skipCommaWhitespace();
      float minY = scan.nextFloat();
      scan.skipCommaWhitespace();
      float width = scan.nextFloat();
      scan.skipCommaWhitespace();
      float height = scan.nextFloat();

      if (Float.isNaN(minX) || Float.isNaN(minY) || Float.isNaN(width) || Float.isNaN(height))
         throw new SVGParseException("Invalid viewBox definition - should have four numbers");
      if (width < 0)
         throw new SVGParseException("Invalid viewBox. width cannot be negative");
      if (height < 0)
         throw new SVGParseException("Invalid viewBox. height cannot be negative");

      return new SVG.Box(minX, minY, width, height);
   }


   /*
    * 
    */
   private static void  parsePreserveAspectRatio(SVG.SvgPreserveAspectRatioContainer obj, String val) throws SVGParseException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      String  word = scan.nextToken();
      if ("defer".equals(word)) {    // Ignore defer keyword
         scan.skipWhitespace();
         word = scan.nextToken();
      }

      PreserveAspectRatio.Alignment  align = AspectRatioKeywords.get(word);
      PreserveAspectRatio.Scale      scale = null;

      scan.skipWhitespace();

      if (!scan.empty()) {
         String meetOrSlice = scan.nextToken();
         switch (meetOrSlice) {
            case "meet":
               scale = PreserveAspectRatio.Scale.Meet; break;
            case "slice":
               scale = PreserveAspectRatio.Scale.Slice; break;
            default:
               throw new SVGParseException("Invalid preserveAspectRatio definition: " + val);
         }
      }
      obj.preserveAspectRatio = new PreserveAspectRatio(align, scale);
   }


   /*
    * Parse a paint specifier such as in the fill and stroke attributes.
    */
   private static SvgPaint parsePaintSpecifier(String val, String attrName) throws SVGParseException
   {
      if (val.startsWith("url("))
      {
         int  closeBracket = val.indexOf(")"); 
         if (closeBracket == -1)
            throw new SVGParseException("Bad "+attrName+" attribute. Unterminated url() reference");

         String    href = val.substring(4, closeBracket).trim();
         SvgPaint  fallback = null;

         val = val.substring(closeBracket+1).trim();
         if (val.length() > 0)
            fallback = parseColourSpecifer(val);
         return new PaintReference(href, fallback);

      }
      return parseColourSpecifer(val);
   }


   private static SvgPaint parseColourSpecifer(String val) throws SVGParseException
   {
      switch (val) {
         case NONE:
            return null;
         case CURRENTCOLOR:
            return CurrentColor.getInstance();
         default:
            return parseColour(val);
      }
   }


   /*
    * Parse a colour definition.
    */
   private static Colour  parseColour(String val) throws SVGParseException
   {
      if (val.charAt(0) == '#')
      {
         IntegerParser  ip = IntegerParser.parseHex(val, 1, val.length());
         if (ip == null) {
            throw new SVGParseException("Bad hex colour value: "+val);
         }
         int  pos = ip.getEndPos();
         int  h1, h2, h3, h4;
         switch (pos) {
            case 4:
               int threehex = ip.value();
               h1 = threehex & 0xf00;  // r
               h2 = threehex & 0x0f0;  // g
               h3 = threehex & 0x00f;  // b
               return new Colour(0xff000000|h1<<12|h1<<8|h2<<8|h2<<4|h3<<4|h3);
            case 5:
               int fourhex = ip.value();
               h1 = fourhex & 0xf000;  // r
               h2 = fourhex & 0x0f00;  // g
               h3 = fourhex & 0x00f0;  // b
               h4 = fourhex & 0x000f;  // alpha
               return new Colour(h4<<28|h4<<24 | h1<<8|h1<<4 | h2<<4|h2 | h3|h3>>4);
            case 7:
               return new Colour(0xff000000 | ip.value());
            case 9:
               return new Colour(ip.value() << 24 | ip.value() >>> 8);
            default:
               // Hex value had bad length for a colour
               throw new SVGParseException("Bad hex colour value: "+val);
         }
      }

      String   valLowerCase = val.toLowerCase(Locale.US);
      boolean  isRGBA = valLowerCase.startsWith("rgba(");
      if (isRGBA || valLowerCase.startsWith("rgb("))
      {
         TextScanner  scan = new TextScanner(val.substring(isRGBA ? 5 : 4));
         scan.skipWhitespace();

         float  red = scan.nextFloat();
         if (!Float.isNaN(red) && scan.consume('%'))
            red = (red * 256) / 100;

         float  green = scan.checkedNextFloat(red);
         if (!Float.isNaN(green) && scan.consume('%'))
            green = (green * 256) / 100;

         float  blue = scan.checkedNextFloat(green);
         if (!Float.isNaN(blue) && scan.consume('%'))
            blue = (blue * 256) / 100;

         if (isRGBA) {
            float  alpha = scan.checkedNextFloat(blue);
            scan.skipWhitespace();
            if (Float.isNaN(alpha) || !scan.consume(')'))
               throw new SVGParseException("Bad rgba() colour value: "+val);
            return new Colour( clamp255(alpha * 256)<<24 | clamp255(red)<<16 | clamp255(green)<<8 | clamp255(blue) );
         } else {
            scan.skipWhitespace();
            if (Float.isNaN(blue) || !scan.consume(')'))
               throw new SVGParseException("Bad rgb() colour value: "+val);
            return new Colour( 0xff000000 | clamp255(red)<<16 | clamp255(green)<<8 | clamp255(blue) );
         }
      }
      else
      {
         boolean  isHSLA = valLowerCase.startsWith("hsla(");
         if (isHSLA || valLowerCase.startsWith("hsl("))
         {
            TextScanner  scan = new TextScanner(val.substring(isHSLA ? 5 : 4));
            scan.skipWhitespace();

            float  hue = scan.nextFloat();

            float  saturation = scan.checkedNextFloat(hue);
            if (!Float.isNaN(saturation))
               scan.consume('%');

            float  lightness = scan.checkedNextFloat(saturation);
            if (!Float.isNaN(lightness))
               scan.consume('%');

            if (isHSLA) {
               float alpha = scan.checkedNextFloat(lightness);
               scan.skipWhitespace();
               if (Float.isNaN(alpha) || !scan.consume(')'))
                  throw new SVGParseException("Bad hsla() colour value: "+val);
               return new Colour( clamp255(alpha * 256)<<24 | hslToRgb(hue, saturation, lightness) );
            } else {
               scan.skipWhitespace();
               if (Float.isNaN(lightness) || !scan.consume(')'))
                  throw new SVGParseException("Bad hsl() colour value: "+val);
               return new Colour( 0xff000000 | hslToRgb(hue, saturation, lightness) );
            }
         }
      }

      // Must be a colour keyword
      return parseColourKeyword(valLowerCase);
   }


   // Clamp a float to the range 0..255
   private static int  clamp255(float val)
   {
      return (val < 0) ? 0 : (val > 255) ? 255 : Math.round(val);
   }


   // Hue (degrees), saturation [0, 100], lightness [0, 100]
   private static int  hslToRgb(float hue, float sat, float light)
   {
      hue = (hue >= 0f) ? hue % 360f : (hue % 360f) + 360f;  // positive modulo (ie. -10 => 350)
      hue /= 60f;    // [0, 360] -> [0, 6]
      sat /= 100;   // [0, 100] -> [0, 1]
      light /= 100; // [0, 100] -> [0, 1]
      sat = (sat < 0f) ? 0f : (sat > 1f) ? 1f : sat;
      light = (light < 0f) ? 0f : (light > 1f) ? 1f : light;
      float  t1, t2;
      if (light <= 0.5f) {
         t2 = light * (sat + 1f);
      } else {
         t2 = light + sat - (light * sat);
      }
      t1 = light * 2f - t2;
      float  r = hueToRgb(t1, t2, hue + 2f);
      float  g = hueToRgb(t1, t2, hue);
      float  b = hueToRgb(t1, t2, hue - 2f);
      return clamp255(r * 256f)<<16 | clamp255(g * 256f)<<8 | clamp255(b * 256f);
   }

   private static float  hueToRgb(float t1, float t2, float hue) {
      if (hue < 0f) hue += 6f;
      if (hue >= 6f) hue -= 6f;

      if (hue < 1) return (t2 - t1) * hue + t1;
      else if (hue < 3f) return t2;
      else if (hue < 4f) return (t2 - t1) * (4f - hue) + t1;
      else return t1;
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private static Colour  parseColourKeyword(String nameLowerCase) throws SVGParseException
   {
      Integer  col = ColourKeywords.get(nameLowerCase);
      if (col == null) {
         throw new SVGParseException("Invalid colour keyword: "+nameLowerCase);
      }
      return new Colour(col);
   }


   // Parse a font attribute
   // [ [ <'font-style'> || <'font-variant'> || <'font-weight'> ]? <'font-size'> [ / <'line-height'> ]? <'font-family'> ] | caption | icon | menu | message-box | small-caption | status-bar | inherit
   private static void  parseFont(Style style, String val) throws SVGParseException
   {
      Integer          fontWeight = null;
      Style.FontStyle  fontStyle = null;
      String           fontVariant = null;

      // Start by checking for the fixed size standard system font names (which we don't support)
      if (!"|caption|icon|menu|message-box|small-caption|status-bar|".contains('|'+val+'|'))
         return;
         
      // Fist part: style/variant/weight (opt - one or more)
      TextScanner  scan = new TextScanner(val);
      String       item;
      while (true)
      {
         item = scan.nextToken('/');
         scan.skipWhitespace();
         if (item == null)
            throw new SVGParseException("Invalid font style attribute: missing font size and family");
         if (fontWeight != null && fontStyle != null)
            break;
         if (item.equals("normal"))  // indeterminate which of these this refers to
            continue;
         if (fontWeight == null) {
            fontWeight = FontWeightKeywords.get(item);
            if (fontWeight != null)
               continue;
         }
         if (fontStyle == null) {
            fontStyle = fontStyleKeyword(item);
            if (fontStyle != null)
               continue;
         }
         // Must be a font-variant keyword?
         if (fontVariant == null && item.equals("small-caps")) {
            fontVariant = item;
            continue;
         }
         // Not any of these. Break and try next section
         break;
      }
      
      // Second part: font size (reqd) and line-height (opt)
      Length  fontSize = parseFontSize(item);

      // Check for line-height (which we don't support)
      if (scan.consume('/'))
      {
         scan.skipWhitespace();
         item = scan.nextToken();
         if (item == null)
            throw new SVGParseException("Invalid font style attribute: missing line-height");
         parseLength(item);
         scan.skipWhitespace();
      }
      
      // Third part: font family
      style.fontFamily = parseFontFamily(scan.restOfText());
      style.fontSize = fontSize;
      style.fontWeight = (fontWeight == null) ? Style.FONT_WEIGHT_NORMAL : fontWeight;
      style.fontStyle = (fontStyle == null) ? Style.FontStyle.Normal : fontStyle;
      style.specifiedFlags |= (SVG.SPECIFIED_FONT_FAMILY | SVG.SPECIFIED_FONT_SIZE | SVG.SPECIFIED_FONT_WEIGHT | SVG.SPECIFIED_FONT_STYLE);
   }


   // Parse a font family list
   private static List<String>  parseFontFamily(String val) throws SVGParseException
   {
      List<String> fonts = null;
      TextScanner  scan = new TextScanner(val);
      while (true)
      {
         String item = scan.nextQuotedString();
         if (item == null)
            item = scan.nextTokenWithWhitespace(',');
         if (item == null)
            break;
         if (fonts == null)
            fonts = new ArrayList<>();
         fonts.add(item);
         scan.skipCommaWhitespace();
         if (scan.empty())
            break;
      }
      return fonts;
   }


   // Parse a font size keyword or numerical value
   private static Length  parseFontSize(String val) throws SVGParseException
   {
      Length  size = FontSizeKeywords.get(val);
      if (size == null) {
         size = parseLength(val);
      }
      return size;
   }


   // Parse a font weight keyword or numerical value
   private static Integer  parseFontWeight(String val) throws SVGParseException
   {
      Integer  wt = FontWeightKeywords.get(val);
      if (wt == null) {
         throw new SVGParseException("Invalid font-weight property: "+val);
      }
      return wt;
   }


   // Parse a font style keyword
   private static Style.FontStyle  parseFontStyle(String val) throws SVGParseException
   {
      Style.FontStyle  fs = fontStyleKeyword(val);
      if (fs != null)
         return fs;
      else
         throw new SVGParseException("Invalid font-style property: "+val);
   }


   // Parse a font style keyword
   private static Style.FontStyle  fontStyleKeyword(String val)
   {
      // Italic is probably the most common, so test that first :)
      if ("italic".equals(val))
         return Style.FontStyle.Italic;
      else if ("normal".equals(val))
         return Style.FontStyle.Normal;
      else if ("oblique".equals(val))
         return Style.FontStyle.Oblique;
      else
         return null;
   }


   // Parse a text decoration keyword
   private static TextDecoration  parseTextDecoration(String val) throws SVGParseException
   {
      if (NONE.equals(val))
         return Style.TextDecoration.None;
      if ("underline".equals(val))
         return Style.TextDecoration.Underline;
      if ("overline".equals(val))
         return Style.TextDecoration.Overline;
      if ("line-through".equals(val))
         return Style.TextDecoration.LineThrough;
      if ("blink".equals(val))
         return Style.TextDecoration.Blink;
      throw new SVGParseException("Invalid text-decoration property: "+val);
   }


   // Parse a text decoration keyword
   private static TextDirection  parseTextDirection(String val) throws SVGParseException
   {
      if ("ltr".equals(val))
         return Style.TextDirection.LTR;
      if ("rtl".equals(val))
         return Style.TextDirection.RTL;
      throw new SVGParseException("Invalid direction property: "+val);
   }


   // Parse fill rule
   private static Style.FillRule  parseFillRule(String val) throws SVGParseException
   {
      if ("nonzero".equals(val))
         return Style.FillRule.NonZero;
      if ("evenodd".equals(val))
         return Style.FillRule.EvenOdd;
      throw new SVGParseException("Invalid fill-rule property: "+val);
   }


   // Parse stroke-linecap
   private static Style.LineCaps  parseStrokeLineCap(String val) throws SVGParseException
   {
      if ("butt".equals(val))
         return Style.LineCaps.Butt;
      if ("round".equals(val))
         return Style.LineCaps.Round;
      if ("square".equals(val))
         return Style.LineCaps.Square;
      throw new SVGParseException("Invalid stroke-linecap property: "+val);
   }


   // Parse stroke-linejoin
   private static Style.LineJoin  parseStrokeLineJoin(String val) throws SVGParseException
   {
      if ("miter".equals(val))
         return Style.LineJoin.Miter;
      if ("round".equals(val))
         return Style.LineJoin.Round;
      if ("bevel".equals(val))
         return Style.LineJoin.Bevel;
      throw new SVGParseException("Invalid stroke-linejoin property: "+val);
   }


   // Parse stroke-dasharray
   private static Length[]  parseStrokeDashArray(String val) throws SVGParseException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      if (scan.empty())
         return null;
      
      Length dash = scan.nextLength();
      if (dash == null)
         return null;
      if (dash.isNegative())
         throw new SVGParseException("Invalid stroke-dasharray. Dash segemnts cannot be negative: "+val);

      float sum = dash.floatValue();

      List<Length> dashes = new ArrayList<>();
      dashes.add(dash);
      while (!scan.empty())
      {
         scan.skipCommaWhitespace();
         dash = scan.nextLength();
         if (dash == null)  // must have hit something unexpected
            throw new SVGParseException("Invalid stroke-dasharray. Non-Length content found: "+val);
         if (dash.isNegative())
            throw new SVGParseException("Invalid stroke-dasharray. Dash segemnts cannot be negative: "+val);
         dashes.add(dash);
         sum += dash.floatValue();
      }

      // Spec (section 11.4) says if the sum of dash lengths is zero, it should
      // be treated as "none" ie a solid stroke.
      if (sum == 0f)
         return null;
      
      return dashes.toArray(new Length[dashes.size()]);
   }


   // Parse a text anchor keyword
   private static Style.TextAnchor  parseTextAnchor(String val) throws SVGParseException
   {
      if ("start".equals(val))
         return Style.TextAnchor.Start;
      if ("middle".equals(val))
         return Style.TextAnchor.Middle;
      if ("end".equals(val))
         return Style.TextAnchor.End;
      throw new SVGParseException("Invalid text-anchor property: "+val);
   }


   // Parse a text anchor keyword
   private static Boolean  parseOverflow(String val) throws SVGParseException
   {
      if ("visible".equals(val) || "auto".equals(val))
         return Boolean.TRUE;
      if ("hidden".equals(val) || "scroll".equals(val))
         return Boolean.FALSE;
      throw new SVGParseException("Invalid toverflow property: "+val);
   }


   // Parse CSS clip shape (always a rect())
   private static CSSClipRect  parseClip(String val) throws SVGParseException
   {
      if ("auto".equals(val))
         return null;
      if (!val.toLowerCase(Locale.US).startsWith("rect("))
         throw new SVGParseException("Invalid clip attribute shape. Only rect() is supported.");

      TextScanner scan = new TextScanner(val.substring(5));
      scan.skipWhitespace();

      Length top = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length right = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length bottom = parseLengthOrAuto(scan);
      scan.skipCommaWhitespace();
      Length left = parseLengthOrAuto(scan);

      scan.skipWhitespace();
      if (!scan.consume(')'))
         throw new SVGParseException("Bad rect() clip definition: "+val);

      return new CSSClipRect(top, right, bottom, left);
   }


   private static Length parseLengthOrAuto(TextScanner scan)
   {
      if (scan.consume("auto"))
         return new Length(0f);

      return scan.nextLength();
   }


   // Parse a vector effect keyword
   private static VectorEffect  parseVectorEffect(String val) throws SVGParseException
   {
      if (NONE.equals(val))
         return Style.VectorEffect.None;
      if ("non-scaling-stroke".equals(val))
         return Style.VectorEffect.NonScalingStroke;
      throw new SVGParseException("Invalid vector-effect property: "+val);
   }


   // Parse a rendering quality property
   private static RenderQuality  parseRenderQuality(String val, String attrName) throws SVGParseException
   {
      if ("auto".equals(val))
         return RenderQuality.auto;
      if ("optimizeQuality".equals(val))
         return RenderQuality.optimizeQuality;
      if ("optimizeSpeed".equals(val))
         return RenderQuality.optimizeSpeed;
      throw new SVGParseException("Invalid " + attrName + " property: "+val);
   }


   //=========================================================================


   // Parse the string that defines a path.
   private static SVG.PathDefinition  parsePath(String val) throws SVGParseException
   {
      TextScanner  scan = new TextScanner(val);

      float   currentX = 0f, currentY = 0f;    // The last point visited in the subpath
      float   lastMoveX = 0f, lastMoveY = 0f;  // The initial point of current subpath
      float   lastControlX = 0f, lastControlY = 0f;  // Last control point of the just completed bezier curve.
      float   x,y, x1,y1, x2,y2;
      float   rx,ry, xAxisRotation;
      Boolean largeArcFlag, sweepFlag;

      SVG.PathDefinition  path = new SVG.PathDefinition();

      if (scan.empty())
         return path;

      int  pathCommand = scan.nextChar();

      if (pathCommand != 'M' && pathCommand != 'm')
         return path;  // Invalid path - doesn't start with a move

      while (true)
      {
         scan.skipWhitespace();

         switch (pathCommand)
         {
            // Move
            case 'M':
            case 'm':
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               // Relative moveto at the start of a path is treated as an absolute moveto.
               if (pathCommand=='m' && !path.isEmpty()) {
                  x += currentX;
                  y += currentY;
               }
               path.moveTo(x, y);
               currentX = lastMoveX = lastControlX = x;
               currentY = lastMoveY = lastControlY = y;
               // Any subsequent coord pairs should be treated as a lineto.
               pathCommand = (pathCommand=='m') ? 'l' : 'L';
               break;

               // Line
            case 'L':
            case 'l':
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='l') {
                  x += currentX;
                  y += currentY;
               }
               path.lineTo(x, y);
               currentX = lastControlX = x;
               currentY = lastControlY = y;
               break;

               // Cubic bezier
            case 'C':
            case 'c':
               x1 = scan.nextFloat();
               y1 = scan.checkedNextFloat(x1);
               x2 = scan.checkedNextFloat(y1);
               y2 = scan.checkedNextFloat(x2);
               x = scan.checkedNextFloat(y2);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='c') {
                  x += currentX;
                  y += currentY;
                  x1 += currentX;
                  y1 += currentY;
                  x2 += currentX;
                  y2 += currentY;
               }
               path.cubicTo(x1, y1, x2, y2, x, y);
               lastControlX = x2;
               lastControlY = y2;
               currentX = x;
               currentY = y;
               break;

               // Smooth curve (first control point calculated)
            case 'S':
            case 's':
               x1 = 2 * currentX - lastControlX;
               y1 = 2 * currentY - lastControlY;
               x2 = scan.nextFloat();
               y2 = scan.checkedNextFloat(x2);
               x = scan.checkedNextFloat(y2);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='s') {
                  x += currentX;
                  y += currentY;
                  x2 += currentX;
                  y2 += currentY;
               }
               path.cubicTo(x1, y1, x2, y2, x, y);
               lastControlX = x2;
               lastControlY = y2;
               currentX = x;
               currentY = y;
               break;

               // Close path
            case 'Z':
            case 'z':
               path.close();
               currentX = lastControlX = lastMoveX;
               currentY = lastControlY = lastMoveY;
               break;

               // Horizontal line
            case 'H':
            case 'h':
               x = scan.nextFloat();
               if (Float.isNaN(x)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='h') {
                  x += currentX;
               }
               path.lineTo(x, currentY);
               currentX = lastControlX = x;
               break;

               // Vertical line
            case 'V':
            case 'v':
               y = scan.nextFloat();
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='v') {
                  y += currentY;
               }
               path.lineTo(currentX, y);
               currentY = lastControlY = y;
               break;

               // Quadratic bezier
            case 'Q':
            case 'q':
               x1 = scan.nextFloat();
               y1 = scan.checkedNextFloat(x1);
               x = scan.checkedNextFloat(y1);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='q') {
                  x += currentX;
                  y += currentY;
                  x1 += currentX;
                  y1 += currentY;
               }
               path.quadTo(x1, y1, x, y);
               lastControlX = x1;
               lastControlY = y1;
               currentX = x;
               currentY = y;
               break;

               // Smooth quadratic bezier
            case 'T':
            case 't':
               x1 = 2 * currentX - lastControlX;
               y1 = 2 * currentY - lastControlY;
               x = scan.nextFloat();
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y)) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='t') {
                  x += currentX;
                  y += currentY;
               }
               path.quadTo(x1, y1, x, y);
               lastControlX = x1;
               lastControlY = y1;
               currentX = x;
               currentY = y;
               break;

               // Arc
            case 'A':
            case 'a':
               rx = scan.nextFloat();
               ry = scan.checkedNextFloat(rx);
               xAxisRotation = scan.checkedNextFloat(ry);
               largeArcFlag = scan.checkedNextFlag(xAxisRotation);
               sweepFlag = scan.checkedNextFlag(largeArcFlag);
               x = scan.checkedNextFloat(sweepFlag);
               y = scan.checkedNextFloat(x);
               if (Float.isNaN(y) || rx < 0 || ry < 0) {
                  Log.e(TAG, "Bad path coords for "+((char)pathCommand)+" path segment");
                  return path;
               }
               if (pathCommand=='a') {
                  x += currentX;
                  y += currentY;
               }
               path.arcTo(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y);
               currentX = lastControlX = x;
               currentY = lastControlY = y;
               break;

            default:
               return path;
         }

         scan.skipCommaWhitespace();
         if (scan.empty())
            break;

         // Test to see if there is another set of coords for the current path command
         if (scan.hasLetter()) {
            // Nope, so get the new path command instead
            pathCommand = scan.nextChar();
         }
      }
      return path;
   }


   //=========================================================================
   // Conditional processing (ie for <switch> element)

   
   // Parse the attribute that declares the list of SVG features that must be
   // supported if we are to render this element
   private static Set<String>  parseRequiredFeatures(String val) throws SVGParseException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String feature = scan.nextToken();
         if (feature.startsWith(FEATURE_STRING_PREFIX)) {
            result.add(feature.substring(FEATURE_STRING_PREFIX.length()));
         } else {
            // Not a feature string we recognise or support. (In order to avoid accidentally
            // matches with our truncated feature strings, we'll replace it with a string
            // we know for sure won't match anything.
            result.add("UNSUPPORTED");
         }
         scan.skipWhitespace();
      }
      return result;
   }


   // Parse the attribute that declares the list of languages, one of which
   // must be supported if we are to render this element
   private static Set<String>  parseSystemLanguage(String val) throws SVGParseException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String language = scan.nextToken();
         int  hyphenPos = language.indexOf('-'); 
         if (hyphenPos != -1) {
            language = language.substring(0, hyphenPos);
         }
         // Get canonical version of language code in case it has changed (see the JavaDoc for Locale.getLanguage())
         language = new Locale(language, "", "").getLanguage();
         result.add(language);
         scan.skipWhitespace();
      }
      return result;
   }


   // Parse the attribute that declares the list of MIME types that must be
   // supported if we are to render this element
   private static Set<String>  parseRequiredFormats(String val) throws SVGParseException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<>();

      while (!scan.empty())
      {
         String mimetype = scan.nextToken();
         result.add(mimetype);
         scan.skipWhitespace();
      }
      return result;
   }


   private static String  parseFunctionalIRI(String val, String attrName) throws SVGParseException
   {
      if (val.equals(NONE))
         return null;
      if (!val.startsWith("url(") || !val.endsWith(")"))
         throw new SVGParseException("Bad "+attrName+" attribute. Expected \"none\" or \"url()\" format");

      return val.substring(4, val.length()-1).trim();
      // Unlike CSS, the SVG spec seems to indicate that quotes are not allowed in "url()" references
   }


   //=========================================================================
   // Parsing <style> element. Very basic CSS parser.
   //=========================================================================


   private void  style(Attributes attributes) throws SVGParseException
   {
      debug("<style>");

      if (currentElement == null)
         throw new SVGParseException("Invalid document. Root element must be <svg>");

      // Check style sheet is in CSS format
      boolean  isTextCSS = true;
      String   media = "all";

      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case type:
               isTextCSS = val.equals("text/css");
               break;
            case media:
               media = val;
               break;
            default:
               break;
         }
      }

      if (isTextCSS && CSSParser.mediaMatches(media, MediaType.screen)) {
         inStyleElement = true;
      } else {
         ignoring = true;
         ignoreDepth = 1;
      }
   }


   private void  parseCSSStyleSheet(String sheet) throws SVGParseException
   {
      CSSParser  cssp = new CSSParser(MediaType.screen);
      svgDocument.addCSSRules(cssp.parse(sheet));
   }

}
