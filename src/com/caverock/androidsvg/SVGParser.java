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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import android.graphics.Matrix;
import android.util.Log;

import com.caverock.androidsvg.CSSParser.MediaType;
import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.CSSClipRect;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.CurrentColor;
import com.caverock.androidsvg.SVG.GradientSpread;
import com.caverock.androidsvg.SVG.Length;
import com.caverock.androidsvg.SVG.PaintReference;
import com.caverock.androidsvg.SVG.Style;
import com.caverock.androidsvg.SVG.Style.TextDecoration;
import com.caverock.androidsvg.SVG.Style.TextDirection;
import com.caverock.androidsvg.SVG.Style.VectorEffect;
import com.caverock.androidsvg.SVG.SvgElementBase;
import com.caverock.androidsvg.SVG.SvgObject;
import com.caverock.androidsvg.SVG.SvgPaint;
import com.caverock.androidsvg.SVG.Text;
import com.caverock.androidsvg.SVG.TextChild;
import com.caverock.androidsvg.SVG.TextPositionedContainer;
import com.caverock.androidsvg.SVG.Unit;

/**
 * SVG parser code. Used by SVG class. Should not be called directly.
 * 
 * @hide
 */
public class SVGParser extends DefaultHandler2
{
   private static final String  TAG = "SVGParser";

   private static final String  SVG_NAMESPACE = "http://www.w3.org/2000/svg";
   private static final String  XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
   private static final String  FEATURE_STRING_PREFIX = "http://www.w3.org/TR/SVG11/feature#";
   
   // SVG parser
   private SVG               svgDocument = null;
   private SVG.SvgContainer  currentElement = null;

   // For handling elements we don't support
   private boolean   ignoring = false;
   private int       ignoreDepth;

   // For handling <title> and <desc>
   private boolean        inMetadataElement = false;
   private String         metadataTag = null;
   private StringBuilder  metadataElementContents = null;

   // For handling <style>
   private boolean        inStyleElement = false;
   private StringBuilder  styleElementContents = null;

   private HashSet<String> supportedFormats = null;


   // Define SVG tags
   private static final String  TAG_SVG            = "svg";
   private static final String  TAG_A              = "a";
   private static final String  TAG_CIRCLE         = "circle";
   private static final String  TAG_CLIPPATH       = "clipPath";
   private static final String  TAG_DEFS           = "defs";
   private static final String  TAG_DESC           = "desc";
   private static final String  TAG_ELLIPSE        = "ellipse";
   private static final String  TAG_G              = "g";
   private static final String  TAG_IMAGE          = "image";
   private static final String  TAG_LINE           = "line";
   private static final String  TAG_LINEARGRADIENT = "linearGradient";
   private static final String  TAG_MARKER         = "marker";
   private static final String  TAG_MASK           = "mask";
   private static final String  TAG_PATH           = "path";
   private static final String  TAG_PATTERN        = "pattern";
   private static final String  TAG_POLYGON        = "polygon";
   private static final String  TAG_POLYLINE       = "polyline";
   private static final String  TAG_RADIALGRADIENT = "radialGradient";
   private static final String  TAG_RECT           = "rect";
   private static final String  TAG_SOLIDCOLOR     = "solidColor";
   private static final String  TAG_STOP           = "stop";
   private static final String  TAG_STYLE          = "style";
   private static final String  TAG_SWITCH         = "switch";
   private static final String  TAG_SYMBOL         = "symbol";
   private static final String  TAG_TEXT           = "text";
   private static final String  TAG_TEXTPATH       = "textPath";
   private static final String  TAG_TITLE          = "title";
   private static final String  TAG_TREF           = "tref";
   private static final String  TAG_TSPAN          = "tspan";
   private static final String  TAG_USE            = "use";
   private static final String  TAG_VIEW           = "view";

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
      id,
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

      public static SVGAttr  fromString(String str)
      {
         if (str.equals("class"))
            return CLASS;
         if (str.indexOf('_') != -1)
            return UNSUPPORTED;
         try
         {
            return valueOf(str.replace('-', '_'));
         } 
         catch (IllegalArgumentException e)
         {
            return UNSUPPORTED;
         }
      }

   }


   // Special attribute keywords
   private static final String  NONE = "none";
   private static final String  CURRENTCOLOR = "currentColor";

   private static final String VALID_DISPLAY_VALUES = "|inline|block|list-item|run-in|compact|marker|table|inline-table"+
                                                      "|table-row-group|table-header-group|table-footer-group|table-row"+
                                                      "|table-column-group|table-column|table-cell|table-caption|none|";
   private static final String VALID_VISIBILITY_VALUES = "|visible|hidden|collapse|";


   private static HashMap<String, Integer> colourKeywords = new HashMap<String, Integer>();
   private static HashMap<String, Length> fontSizeKeywords = new HashMap<String, Length>(9);
   private static HashMap<String, Integer> fontWeightKeywords = new HashMap<String, Integer>(13);
   private static HashMap<String, Style.FontStyle>fontStyleKeywords = new HashMap<String, Style.FontStyle>(3); 
   private static HashMap<String, SVG.AspectRatioAlignment> aspectRatioKeywords = new HashMap<String, SVG.AspectRatioAlignment>();
   protected static HashSet<String> supportedFeatures = new HashSet<String>();

   static {
      colourKeywords.put("aliceblue", 0xf0f8ff);
      colourKeywords.put("antiquewhite", 0xfaebd7);
      colourKeywords.put("aqua", 0x00ffff);
      colourKeywords.put("aquamarine", 0x7fffd4);
      colourKeywords.put("azure", 0xf0ffff);
      colourKeywords.put("beige", 0xf5f5dc);
      colourKeywords.put("bisque", 0xffe4c4);
      colourKeywords.put("black", 0x000000);
      colourKeywords.put("blanchedalmond", 0xffebcd);
      colourKeywords.put("blue", 0x0000ff);
      colourKeywords.put("blueviolet", 0x8a2be2);
      colourKeywords.put("brown", 0xa52a2a);
      colourKeywords.put("burlywood", 0xdeb887);
      colourKeywords.put("cadetblue", 0x5f9ea0);
      colourKeywords.put("chartreuse", 0x7fff00);
      colourKeywords.put("chocolate", 0xd2691e);
      colourKeywords.put("coral", 0xff7f50);
      colourKeywords.put("cornflowerblue", 0x6495ed);
      colourKeywords.put("cornsilk", 0xfff8dc);
      colourKeywords.put("crimson", 0xdc143c);
      colourKeywords.put("cyan", 0x00ffff);
      colourKeywords.put("darkblue", 0x00008b);
      colourKeywords.put("darkcyan", 0x008b8b);
      colourKeywords.put("darkgoldenrod", 0xb8860b);
      colourKeywords.put("darkgray", 0xa9a9a9);
      colourKeywords.put("darkgreen", 0x006400);
      colourKeywords.put("darkgrey", 0xa9a9a9);
      colourKeywords.put("darkkhaki", 0xbdb76b);
      colourKeywords.put("darkmagenta", 0x8b008b);
      colourKeywords.put("darkolivegreen", 0x556b2f);
      colourKeywords.put("darkorange", 0xff8c00);
      colourKeywords.put("darkorchid", 0x9932cc);
      colourKeywords.put("darkred", 0x8b0000);
      colourKeywords.put("darksalmon", 0xe9967a);
      colourKeywords.put("darkseagreen", 0x8fbc8f);
      colourKeywords.put("darkslateblue", 0x483d8b);
      colourKeywords.put("darkslategray", 0x2f4f4f);
      colourKeywords.put("darkslategrey", 0x2f4f4f);
      colourKeywords.put("darkturquoise", 0x00ced1);
      colourKeywords.put("darkviolet", 0x9400d3);
      colourKeywords.put("deeppink", 0xff1493);
      colourKeywords.put("deepskyblue", 0x00bfff);
      colourKeywords.put("dimgray", 0x696969);
      colourKeywords.put("dimgrey", 0x696969);
      colourKeywords.put("dodgerblue", 0x1e90ff);
      colourKeywords.put("firebrick", 0xb22222);
      colourKeywords.put("floralwhite", 0xfffaf0);
      colourKeywords.put("forestgreen", 0x228b22);
      colourKeywords.put("fuchsia", 0xff00ff);
      colourKeywords.put("gainsboro", 0xdcdcdc);
      colourKeywords.put("ghostwhite", 0xf8f8ff);
      colourKeywords.put("gold", 0xffd700);
      colourKeywords.put("goldenrod", 0xdaa520);
      colourKeywords.put("gray", 0x808080);
      colourKeywords.put("green", 0x008000);
      colourKeywords.put("greenyellow", 0xadff2f);
      colourKeywords.put("grey", 0x808080);
      colourKeywords.put("honeydew", 0xf0fff0);
      colourKeywords.put("hotpink", 0xff69b4);
      colourKeywords.put("indianred", 0xcd5c5c);
      colourKeywords.put("indigo", 0x4b0082);
      colourKeywords.put("ivory", 0xfffff0);
      colourKeywords.put("khaki", 0xf0e68c);
      colourKeywords.put("lavender", 0xe6e6fa);
      colourKeywords.put("lavenderblush", 0xfff0f5);
      colourKeywords.put("lawngreen", 0x7cfc00);
      colourKeywords.put("lemonchiffon", 0xfffacd);
      colourKeywords.put("lightblue", 0xadd8e6);
      colourKeywords.put("lightcoral", 0xf08080);
      colourKeywords.put("lightcyan", 0xe0ffff);
      colourKeywords.put("lightgoldenrodyellow", 0xfafad2);
      colourKeywords.put("lightgray", 0xd3d3d3);
      colourKeywords.put("lightgreen", 0x90ee90);
      colourKeywords.put("lightgrey", 0xd3d3d3);
      colourKeywords.put("lightpink", 0xffb6c1);
      colourKeywords.put("lightsalmon", 0xffa07a);
      colourKeywords.put("lightseagreen", 0x20b2aa);
      colourKeywords.put("lightskyblue", 0x87cefa);
      colourKeywords.put("lightslategray", 0x778899);
      colourKeywords.put("lightslategrey", 0x778899);
      colourKeywords.put("lightsteelblue", 0xb0c4de);
      colourKeywords.put("lightyellow", 0xffffe0);
      colourKeywords.put("lime", 0x00ff00);
      colourKeywords.put("limegreen", 0x32cd32);
      colourKeywords.put("linen", 0xfaf0e6);
      colourKeywords.put("magenta", 0xff00ff);
      colourKeywords.put("maroon", 0x800000);
      colourKeywords.put("mediumaquamarine", 0x66cdaa);
      colourKeywords.put("mediumblue", 0x0000cd);
      colourKeywords.put("mediumorchid", 0xba55d3);
      colourKeywords.put("mediumpurple", 0x9370db);
      colourKeywords.put("mediumseagreen", 0x3cb371);
      colourKeywords.put("mediumslateblue", 0x7b68ee);
      colourKeywords.put("mediumspringgreen", 0x00fa9a);
      colourKeywords.put("mediumturquoise", 0x48d1cc);
      colourKeywords.put("mediumvioletred", 0xc71585);
      colourKeywords.put("midnightblue", 0x191970);
      colourKeywords.put("mintcream", 0xf5fffa);
      colourKeywords.put("mistyrose", 0xffe4e1);
      colourKeywords.put("moccasin", 0xffe4b5);
      colourKeywords.put("navajowhite", 0xffdead);
      colourKeywords.put("navy", 0x000080);
      colourKeywords.put("oldlace", 0xfdf5e6);
      colourKeywords.put("olive", 0x808000);
      colourKeywords.put("olivedrab", 0x6b8e23);
      colourKeywords.put("orange", 0xffa500);
      colourKeywords.put("orangered", 0xff4500);
      colourKeywords.put("orchid", 0xda70d6);
      colourKeywords.put("palegoldenrod", 0xeee8aa);
      colourKeywords.put("palegreen", 0x98fb98);
      colourKeywords.put("paleturquoise", 0xafeeee);
      colourKeywords.put("palevioletred", 0xdb7093);
      colourKeywords.put("papayawhip", 0xffefd5);
      colourKeywords.put("peachpuff", 0xffdab9);
      colourKeywords.put("peru", 0xcd853f);
      colourKeywords.put("pink", 0xffc0cb);
      colourKeywords.put("plum", 0xdda0dd);
      colourKeywords.put("powderblue", 0xb0e0e6);
      colourKeywords.put("purple", 0x800080);
      colourKeywords.put("red", 0xff0000);
      colourKeywords.put("rosybrown", 0xbc8f8f);
      colourKeywords.put("royalblue", 0x4169e1);
      colourKeywords.put("saddlebrown", 0x8b4513);
      colourKeywords.put("salmon", 0xfa8072);
      colourKeywords.put("sandybrown", 0xf4a460);
      colourKeywords.put("seagreen", 0x2e8b57);
      colourKeywords.put("seashell", 0xfff5ee);
      colourKeywords.put("sienna", 0xa0522d);
      colourKeywords.put("silver", 0xc0c0c0);
      colourKeywords.put("skyblue", 0x87ceeb);
      colourKeywords.put("slateblue", 0x6a5acd);
      colourKeywords.put("slategray", 0x708090);
      colourKeywords.put("slategrey", 0x708090);
      colourKeywords.put("snow", 0xfffafa);
      colourKeywords.put("springgreen", 0x00ff7f);
      colourKeywords.put("steelblue", 0x4682b4);
      colourKeywords.put("tan", 0xd2b48c);
      colourKeywords.put("teal", 0x008080);
      colourKeywords.put("thistle", 0xd8bfd8);
      colourKeywords.put("tomato", 0xff6347);
      colourKeywords.put("turquoise", 0x40e0d0);
      colourKeywords.put("violet", 0xee82ee);
      colourKeywords.put("wheat", 0xf5deb3);
      colourKeywords.put("white", 0xffffff);
      colourKeywords.put("whitesmoke", 0xf5f5f5);
      colourKeywords.put("yellow", 0xffff00);
      colourKeywords.put("yellowgreen", 0x9acd32);

      fontSizeKeywords.put("xx-small", new Length(0.694f, Unit.pt));
      fontSizeKeywords.put("x-small", new Length(0.833f, Unit.pt));
      fontSizeKeywords.put("small", new Length(10.0f, Unit.pt));
      fontSizeKeywords.put("medium", new Length(12.0f, Unit.pt));
      fontSizeKeywords.put("large", new Length(14.4f, Unit.pt));
      fontSizeKeywords.put("x-large", new Length(17.3f, Unit.pt));
      fontSizeKeywords.put("xx-large", new Length(20.7f, Unit.pt));
      fontSizeKeywords.put("smaller", new Length(83.33f, Unit.percent));
      fontSizeKeywords.put("larger", new Length(120f, Unit.percent));

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

      fontStyleKeywords.put("normal", Style.FontStyle.Normal);
      fontStyleKeywords.put("italic", Style.FontStyle.Italic);
      fontStyleKeywords.put("oblique", Style.FontStyle.Oblique);

      aspectRatioKeywords.put(NONE, SVG.AspectRatioAlignment.none);
      aspectRatioKeywords.put("xMinYMin", SVG.AspectRatioAlignment.xMinYMin);
      aspectRatioKeywords.put("xMidYMin", SVG.AspectRatioAlignment.xMidYMin);
      aspectRatioKeywords.put("xMaxYMin", SVG.AspectRatioAlignment.xMaxYMin);
      aspectRatioKeywords.put("xMinYMid", SVG.AspectRatioAlignment.xMinYMid);
      aspectRatioKeywords.put("xMidYMid", SVG.AspectRatioAlignment.xMidYMid);
      aspectRatioKeywords.put("xMaxYMid", SVG.AspectRatioAlignment.xMaxYMid);
      aspectRatioKeywords.put("xMinYMax", SVG.AspectRatioAlignment.xMinYMax);
      aspectRatioKeywords.put("xMidYMax", SVG.AspectRatioAlignment.xMidYMax);
      aspectRatioKeywords.put("xMaxYMax", SVG.AspectRatioAlignment.xMaxYMax);

      // SVG features this SVG implementation supports
      // Actual feature strings have the prefix: FEATURE_STRING_PREFIX (see above)
      // NO indicates feature will probable not ever be implemented
      // NYI indicates support is in progress, or is planned
      
      // Feature sets that represent sets of other feature strings (ie a group of features strings)
      //supportedFeatures.add("SVG");                       // NO
      //supportedFeatures.add("SVGDOM");                    // NO
      //supportedFeatures.add("SVG-static");                // NO
      //supportedFeatures.add("SVGDOM-static");             // NO
      //supportedFeatures.add("SVG-animation");             // NO
      //supportedFeatures.add("SVGDOM-animation");          // NO 
      //supportedFeatures.add("SVG-dynamic");               // NO
      //supportedFeatures.add("SVGDOM-dynamic");            // NO

      // Individual features
      //supportedFeatures.add("CoreAttribute");             // NO
      supportedFeatures.add("Structure");                   // YES (although desc title and metadata are ignored)
      supportedFeatures.add("BasicStructure");              // YES (although desc title and metadata are ignored)
      //supportedFeatures.add("ContainerAttribute");        // NO (filter related. NYI)
      supportedFeatures.add("ConditionalProcessing");       // YES
      supportedFeatures.add("Image");                       // YES (bitmaps only - not SVG files)
      supportedFeatures.add("Style");                       // YES
      supportedFeatures.add("ViewportAttribute");           // YES
      supportedFeatures.add("Shape");                       // YES
      //supportedFeatures.add("Text");                      // NO
      supportedFeatures.add("BasicText");                   // YES
      supportedFeatures.add("PaintAttribute");              // YES (except color-interpolation and color-rendering)
      supportedFeatures.add("BasicPaintAttribute");         // YES (except color-rendering)
      supportedFeatures.add("OpacityAttribute");            // YES
      //supportedFeatures.add("GraphicsAttribute");         // NO     
      supportedFeatures.add("BasicGraphicsAttribute");      // YES
      supportedFeatures.add("Marker");                      // YES
      //supportedFeatures.add("ColorProfile");              // NO
      supportedFeatures.add("Gradient");                    // YES
      supportedFeatures.add("Pattern");                     // YES
      supportedFeatures.add("Clip");                        // YES
      supportedFeatures.add("BasicClip");                   // YES
      supportedFeatures.add("Mask");                        // YES
      //supportedFeatures.add("Filter");                    // NO
      //supportedFeatures.add("BasicFilter");               // NO
      //supportedFeatures.add("DocumentEventsAttribute");   // NO
      //supportedFeatures.add("GraphicalEventsAttribute");  // NO
      //supportedFeatures.add("AnimationEventsAttribute");  // NO
      //supportedFeatures.add("Cursor");                    // NO
      //supportedFeatures.add("Hyperlinking");              // NO
      //supportedFeatures.add("XlinkAttribute");            // NO
      //supportedFeatures.add("ExternalResourcesRequired"); // NO
      supportedFeatures.add("View");                        // YES
      //supportedFeatures.add("Script");                    // NO
      //supportedFeatures.add("Animation");                 // NO
      //supportedFeatures.add("Font");                      // NO
      //supportedFeatures.add("BasicFont");                 // NO
      //supportedFeatures.add("Extensibility");             // NO

      // SVG 1.0 features - all are too general and include things we are not likely to ever support.
      // If we ever do support these, we'll need to change how FEATURE_STRING_PREFIX is used.
      //supportedFeatures.add("org.w3c.svg");
      //supportedFeatures.add("org.w3c.dom.svg");
      //supportedFeatures.add("org.w3c.svg.static");
      //supportedFeatures.add("org.w3c.dom.svg.static");
      //supportedFeatures.add("org.w3c.svg.animation");
      //supportedFeatures.add("org.w3c.dom.svg.animation");
      //supportedFeatures.add("org.w3c.svg.dynamic");
      //supportedFeatures.add("org.w3c.dom.svg.dynamic");
      //supportedFeatures.add("org.w3c.svg.all");
      //supportedFeatures.add("org.w3c.dom.svg.all" );
   }


   protected void  setSupportedFormats(String[] mimeTypes)
   {
      this.supportedFormats = new HashSet<String>(mimeTypes.length);
      Collections.addAll(this.supportedFormats, mimeTypes);
   }


   //=========================================================================
   // Main parser invocation methods
   //=========================================================================


   protected SVG  parse(InputStream is) throws SVGParseException
   {
      SAXParserFactory  spf = SAXParserFactory.newInstance();
      try
      {
         SAXParser sp = spf.newSAXParser();
         XMLReader xr = sp.getXMLReader();
         xr.setContentHandler(this);
         xr.setProperty("http://xml.org/sax/properties/lexical-handler", this);
         xr.parse(new InputSource(is));
      }
      catch (IOException e)
      {
         throw new SVGParseException("File error", e);
      }
      catch (ParserConfigurationException e)
      {
         throw new SVGParseException("XML Parser problem", e);
      }
      catch (SAXException e)
      {
         throw new SVGParseException("SVG parse error: "+e.getMessage(), e);
      }
      return svgDocument;
   }


   //=========================================================================
   // SAX methods
   //=========================================================================


   @Override
   public void startDocument() throws SAXException
   {
      super.startDocument();
      svgDocument = new SVG();
   }


   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
   {
      super.startElement(uri, localName, qName, attributes);

      if (ignoring) {
         ignoreDepth++;
         return;
      }
      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      if (localName.equals(TAG_SVG)) {
         svg(attributes);
      } else if (localName.equals(TAG_G)) {
         g(attributes);
      } else if (localName.equals(TAG_DEFS)) {
         defs(attributes);
      } else if (localName.equals(TAG_USE)) {
         use(attributes);
      } else if (localName.equals(TAG_PATH)) {
         path(attributes);
      } else if (localName.equals(TAG_RECT)) {
         rect(attributes);
      } else if (localName.equals(TAG_CIRCLE)) {
         circle(attributes);
      } else if (localName.equals(TAG_ELLIPSE)) {
         ellipse(attributes);
      } else if (localName.equals(TAG_LINE)) {
         line(attributes);
      } else if (localName.equals(TAG_POLYLINE)) {
         polyline(attributes);
      } else if (localName.equals(TAG_POLYGON)) {
         polygon(attributes);
      } else if (localName.equals(TAG_TEXT)) {
         text(attributes);
      } else if (localName.equals(TAG_TSPAN)) {
         tspan(attributes);
      } else if (localName.equals(TAG_TREF)) {
         tref(attributes);
      } else if (localName.equals(TAG_SWITCH)) {
         zwitch(attributes);
      } else if (localName.equals(TAG_SYMBOL)) {
         symbol(attributes);
      } else if (localName.equals(TAG_MARKER)) {
         marker(attributes);
      } else if (localName.equals(TAG_LINEARGRADIENT)) {
         linearGradient(attributes);
      } else if (localName.equals(TAG_RADIALGRADIENT)) {
         radialGradient(attributes);
      } else if (localName.equals(TAG_STOP)) {
         stop(attributes);
      } else if (localName.equals(TAG_A)) {
         g(attributes);    // Treat like a group element
      } else if (localName.equals(TAG_TITLE) || localName.equals(TAG_DESC)) {
         inMetadataElement = true;
         metadataTag = localName;
      } else if (localName.equals(TAG_CLIPPATH)) {
         clipPath(attributes);
      } else if (localName.equals(TAG_TEXTPATH)) {
         textPath(attributes);
      } else if (localName.equals(TAG_PATTERN)) {
         pattern(attributes);
      } else if (localName.equals(TAG_IMAGE)) {
         image(attributes);
      } else if (localName.equals(TAG_VIEW)) {
         view(attributes);
      } else if (localName.equals(TAG_MASK)) {
         mask(attributes);
      } else if (localName.equals(TAG_STYLE)) {
         style(attributes);
      } else if (localName.equals(TAG_SOLIDCOLOR)) {
         solidColor(attributes);
      } else {
         ignoring = true;
         ignoreDepth = 1;
      }
   }


   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      if (ignoring)
         return;

      if (inMetadataElement)
      {
         if (metadataElementContents == null)
            metadataElementContents = new StringBuilder(length);
         metadataElementContents.append(ch, start, length);
         return;
      }

      if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(length);
         styleElementContents.append(ch, start, length);
         return;
      }

      if (currentElement instanceof SVG.TextContainer)
      {
         // The SAX parser can pass us several text nodes in a row. If this happens, we
         // want to collapse them all into one SVG.TextSequence node
         SVG.SvgConditionalContainer  parent = (SVG.SvgConditionalContainer) currentElement;
         int  numOlderSiblings = parent.children.size();
         SVG.SvgObject  previousSibling = (numOlderSiblings == 0) ? null : parent.children.get(numOlderSiblings-1);
         if (previousSibling instanceof SVG.TextSequence) {
            // Last sibling was a TextSequence also, so merge them.
            ((SVG.TextSequence) previousSibling).text += new String(ch, start, length);
         } else {
            // Add a new TextSequence to the child node list
            ((SVG.SvgConditionalContainer) currentElement).addChild(new SVG.TextSequence( new String(ch, start, length) ));
         }
      }

   }


   @Override
   public void comment(char[] ch, int start, int length) throws SAXException
   {
      if (ignoring)
         return;

      // It is legal for the contents of the <style> element to be enclosed in XML comments (ie. "<!--" and "-->").
      // So we need to include the contents of the comment in the text sent to the CSS parser.
      if (inStyleElement)
      {
         if (styleElementContents == null)
            styleElementContents = new StringBuilder(length);
         styleElementContents.append(ch, start, length);
         return;
      }

   }


   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (ignoring) {
         if (--ignoreDepth == 0) {
            ignoring = false;
            return;
         }
      }

      if (!SVG_NAMESPACE.equals(uri) && !"".equals(uri)) {
         return;
      }

      if (localName.equals(TAG_TITLE) || localName.equals(TAG_DESC)) {
         inMetadataElement = false;
         if (metadataTag.equals(TAG_TITLE))
            svgDocument.setTitle(metadataElementContents.toString());
         else if (metadataTag.equals(TAG_DESC))
            svgDocument.setDesc(metadataElementContents.toString());
         metadataElementContents.setLength(0);
         return;
      }

      if (localName.equals(TAG_STYLE) && styleElementContents != null) {
         inStyleElement = false;
         parseCSSStyleSheet(styleElementContents.toString());
         styleElementContents.setLength(0);
         return;
      }

      if (localName.equals(TAG_TEXT)) {
         collapseSpaces((SVG.Text) currentElement);
      }

      // Yes this is ugly. May switch to using tokens in the future.
      if (localName.equals(TAG_SVG) ||
          localName.equals(TAG_DEFS) ||
          localName.equals(TAG_G) ||
          localName.equals(TAG_USE) ||
          localName.equals(TAG_IMAGE) ||
          localName.equals(TAG_TEXT) ||
          localName.equals(TAG_TSPAN) ||
          localName.equals(TAG_SWITCH) ||
          localName.equals(TAG_SYMBOL) ||
          localName.equals(TAG_MARKER) ||
          localName.equals(TAG_LINEARGRADIENT) ||
          localName.equals(TAG_RADIALGRADIENT) ||
          localName.equals(TAG_STOP) ||
          localName.equals(TAG_CLIPPATH) ||
          localName.equals(TAG_TEXTPATH) ||
          localName.equals(TAG_PATTERN) ||
          localName.equals(TAG_VIEW) ||
          localName.equals(TAG_MASK) ||
          localName.equals(TAG_SOLIDCOLOR)) {
         currentElement = ((SvgObject) currentElement).parent;
      }

   }

   
   @Override
   public void endDocument() throws SAXException
   {
      super.endDocument();

      // Dump document
      if (LibConfig.DEBUG)
         dumpNode(svgDocument.getRootElement(), "");
   }


   private void dumpNode(SVG.SvgObject elem, String indent)
   {
      Log.d(TAG, indent+elem);
      if (elem instanceof SVG.SvgConditionalContainer) {
         indent = indent+"  ";
         for (SVG.SvgObject child: ((SVG.SvgConditionalContainer) elem).children) {
            dumpNode(child, indent);
         }
      }
   }


   //==============================================================================


   private void  debug(String format, Object... args)
   {
      if (LibConfig.DEBUG)
         Log.d(TAG, String.format(format, args));
   }


   //=========================================================================
   // Handlers for each SVG element
   //=========================================================================
   // <svg> element

   private void  svg(Attributes attributes) throws SAXException
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

   
   private void  parseAttributesSVG(SVG.Svg obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <svg> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <svg> element. height cannot be negative");
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


   private void  g(Attributes attributes) throws SAXException
   {
      debug("<g>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  defs(Attributes attributes) throws SAXException
   {
      debug("<defs>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  use(Attributes attributes) throws SAXException
   {
      debug("<use>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesUse(SVG.Use obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
               obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <image> element


   private void  image(Attributes attributes) throws SAXException
   {
      debug("<image>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesImage(SVG.Image obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <use> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <use> element. height cannot be negative");
               break;
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
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


   private void  path(Attributes attributes) throws SAXException
   {
      debug("<path>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesPath(SVG.Path obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <path> element. pathLength cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <rect> element


   private void  rect(Attributes attributes) throws SAXException
   {
      debug("<rect>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesRect(SVG.Rect obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <rect> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <rect> element. height cannot be negative");
               break;
            case rx:
               obj.rx = parseLength(val);
               if (obj.rx.isNegative())
                  throw new SAXException("Invalid <rect> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SAXException("Invalid <rect> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <circle> element


   private void  circle(Attributes attributes) throws SAXException
   {
      debug("<circle>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesCircle(SVG.Circle obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <circle> element. r cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <ellipse> element


   private void  ellipse(Attributes attributes) throws SAXException
   {
      debug("<ellipse>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesEllipse(SVG.Ellipse obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <ellipse> element. rx cannot be negative");
               break;
            case ry:
               obj.ry = parseLength(val);
               if (obj.ry.isNegative())
                  throw new SAXException("Invalid <ellipse> element. ry cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <line> element


   private void  line(Attributes attributes) throws SAXException
   {
      debug("<line>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesLine(SVG.Line obj, Attributes attributes) throws SAXException
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


   private void  polyline(Attributes attributes) throws SAXException
   {
      debug("<polyline>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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
   private void  parseAttributesPolyLine(SVG.PolyLine obj, Attributes attributes, String tag) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.points)
         {
            TextScanner scan = new TextScanner(attributes.getValue(i));
            List<Float> points = new ArrayList<Float>();
            scan.skipWhitespace();

            while (!scan.empty()) {
               Float x = scan.nextFloat();
               if (x == null)
                  throw new SAXException("Invalid <"+tag+"> points attribute. Non-coordinate content found in list.");
               scan.skipCommaWhitespace();
               Float y = scan.nextFloat();
               if (y == null)
                  throw new SAXException("Invalid <"+tag+"> points attribute. There should be an even number of coordinates.");
               scan.skipCommaWhitespace();
               points.add(x);
               points.add(y);
            }
            obj.points = new float[points.size()];
            int j = 0;
            for (Float f: points) {
               obj.points[j++] = f;
            }
         }
      }
   }


   //=========================================================================
   // <polygon> element


   private void  polygon(Attributes attributes) throws SAXException
   {
      debug("<polygon>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  text(Attributes attributes) throws SAXException
   {
      debug("<text>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesTextPosition(TextPositionedContainer obj, Attributes attributes) throws SAXException
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


   /*
    * Collapse the spaces in a Text subtree using the normal HTML/XML rules.
    */
   private void collapseSpaces(SVG.Text textObj)
   {
      collapseSpaces(textObj, false,false);
   }

   private void  collapseSpaces(SVG.SvgObject obj, boolean isFirstNode, boolean isLastNode)
   {
      if (obj instanceof SVG.TextContainer)
      {
         SVG.TextContainer tspan = (SVG.TextContainer) obj; 

         isFirstNode = true;
         for (Iterator<SVG.SvgObject> iterator = tspan.children.iterator(); iterator.hasNext(); isFirstNode=false) {
            SVG.SvgObject child = iterator.next();
            collapseSpaces(child, isFirstNode, !iterator.hasNext());
         }
      }
      else if  (obj instanceof SVG.TextSequence)
      {
         SVG.TextSequence tseq = (SVG.TextSequence) obj; 
         tseq.text = trimmer(tseq.text, isFirstNode, isLastNode);
      }
      else if  (obj instanceof SVG.TRef)
      {
         // N/A
      }
   }

   // Trim whitespace chars appropriately depending on where the node is
   // relative to other text nodes.
   private String trimmer(String str, boolean isFirstNode, boolean isLastNode)
   {
      int len = str.length();
      int offset = 0;

      if (len == 0)
         return str;

      char[] chars = str.toCharArray();

      while (offset < len && chars[offset] <= ' ') {
         offset++;
      }
      len -= offset;
      while (len > 0 && chars[offset + len - 1] <= ' ') {
         len--;
      }

      // Allow one space at the start if this wasn't the first node
      if (len == 0)
      {
         if (!isFirstNode && !isLastNode)
         {
            chars[0] = ' ';
            offset = 0;
            len = 1;
         }
      }
      else
      {
         if (offset > 0 && !isFirstNode)
         {
            chars[--offset] = ' ';
            len++;
         }
         // Allow one space at the end if this wasn't the last node
         if ((offset + len) < str.length() && !isLastNode)
         {
            chars[offset + len] = ' ';
            len++;
         }
      }

      return new String(chars, offset, len);
   }


   //=========================================================================
   // <tspan> element


   private void  tspan(Attributes attributes) throws SAXException
   {
      debug("<tspan>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.TextContainer))
         throw new SAXException("Invalid document. <tspan> elements are only valid inside <text> or other <tspan> elements.");
      SVG.TSpan  obj = new SVG.TSpan();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPosition(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof Text)
         obj.setTextRoot((Text) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   //=========================================================================
   // <tref> element


   private void  tref(Attributes attributes) throws SAXException
   {
      debug("<tref>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.TextContainer))
         throw new SAXException("Invalid document. <tref> elements are only valid inside <text> or <tspan> elements.");
      SVG.TRef  obj = new SVG.TRef();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTRef(obj, attributes);
      currentElement.addChild(obj);
      if (obj.parent instanceof Text)
         obj.setTextRoot((Text) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTRef(SVG.TRef obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
               obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <switch> element


   private void  zwitch(Attributes attributes) throws SAXException
   {
      debug("<switch>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesConditional(SVG.SvgConditional obj, Attributes attributes) throws SAXException
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
               Set<String>  fontSet = (fonts != null) ? new HashSet<String>(fonts) : new HashSet<String>(0);
               obj.setRequiredFonts(fontSet);
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <symbol> element


   private void  symbol(Attributes attributes) throws SAXException
   {
      debug("<symbol>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  marker(Attributes attributes) throws SAXException
   {
      debug("<marker>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesMarker(SVG.Marker obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <marker> element. markerWidth cannot be negative");
               break;
            case markerHeight:
               obj.markerHeight = parseLength(val);
               if (obj.markerHeight.isNegative())
                  throw new SAXException("Invalid <marker> element. markerHeight cannot be negative");
               break;
            case markerUnits:
               if ("strokeWidth".equals(val)) {
                  obj.markerUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.markerUnitsAreUser = true;
               } else {
                  throw new SAXException("Invalid value for attribute markerUnits");
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


   private void  linearGradient(Attributes attributes) throws SAXException
   {
      debug("<linearGradiant>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesGradient(SVG.GradientElement obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid value for attribute gradientUnits");
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
                  throw new SAXException("Invalid spreadMethod attribute. \""+val+"\" is not a valid value.");
               }
               break;
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
               obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   private void  parseAttributesLinearGradient(SVG.SvgLinearGradient obj, Attributes attributes) throws SAXException
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


   private void  radialGradient(Attributes attributes) throws SAXException
   {
      debug("<radialGradient>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesRadialGradient(SVG.SvgRadialGradient obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid <radialGradient> element. r cannot be negative");
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
   // Gradiant <stop> element


   private void  stop(Attributes attributes) throws SAXException
   {
      debug("<stop>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.GradientElement))
         throw new SAXException("Invalid document. <stop> elements are only valid inside <linearGradiant> or <radialGradient> elements.");
      SVG.Stop  obj = new SVG.Stop();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesStop(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesStop(SVG.Stop obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case offset:
               obj.offset = parseGradiantOffset(val);
               break;
            default:
               break;
         }
      }
   }


   private Float parseGradiantOffset(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid offset value in <stop> (empty string)");
      int      end = val.length();
      boolean  isPercent = false;

      if (val.charAt(val.length()-1) == '%') {
         end -= 1;
         isPercent = true;
      }
      try
      {
         float scalar = Float.parseFloat(val.substring(0, end));
         if (isPercent)
            scalar /= 100f;
         return (scalar < 0) ? 0 : (scalar > 100) ? 100 : scalar;
      }
      catch (NumberFormatException e)
      {
         throw new SAXException("Invalid offset value in <stop>: "+val, e);
      }
   }


   //=========================================================================
   // <solidColor> element


   private void  solidColor(Attributes attributes) throws SAXException
   {
      debug("<solidColor>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  clipPath(Attributes attributes) throws SAXException
   {
      debug("<clipPath>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesClipPath(SVG.ClipPath obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid value for attribute clipPathUnits");
               }
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <textPath> element


   private void textPath(Attributes attributes) throws SAXException
   {
      debug("<textPath>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.TextPath  obj = new SVG.TextPath();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesConditional(obj, attributes);
      parseAttributesTextPath(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
      if (obj.parent instanceof Text)
         obj.setTextRoot((Text) obj.parent);
      else
         obj.setTextRoot(((TextChild) obj.parent).getTextRoot());
   }


   private void  parseAttributesTextPath(SVG.TextPath obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
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


   private void pattern(Attributes attributes) throws SAXException
   {
      debug("<pattern>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesPattern(SVG.Pattern obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid value for attribute patternUnits");
               } 
               break;
            case patternContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.patternContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.patternContentUnitsAreUser = true;
               } else {
                  throw new SAXException("Invalid value for attribute patternContentUnits");
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
                  throw new SAXException("Invalid <pattern> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <pattern> element. height cannot be negative");
               break;
            case href:
               if (!XLINK_NAMESPACE.equals(attributes.getURI(i)))
                  break;
               obj.href = val;
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <view> element


   private void  view(Attributes attributes) throws SAXException
   {
      debug("<view>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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
   // <pattern> element


   private void mask(Attributes attributes) throws SAXException
   {
      debug("<mask>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
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


   private void  parseAttributesMask(SVG.Mask obj, Attributes attributes) throws SAXException
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
                  throw new SAXException("Invalid value for attribute maskUnits");
               } 
               break;
            case maskContentUnits:
               if ("objectBoundingBox".equals(val)) {
                  obj.maskContentUnitsAreUser = false;
               } else if ("userSpaceOnUse".equals(val)) {
                  obj.maskContentUnitsAreUser = true;
               } else {
                  throw new SAXException("Invalid value for attribute maskContentUnits");
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
                  throw new SAXException("Invalid <mask> element. width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <mask> element. height cannot be negative");
               break;
            default:
               break;
         }
      }
   }


   //=========================================================================
   // String tokeniser
   //=========================================================================


   protected static class TextScanner
   {
      protected String   input;
      protected int      position = 0;


      public TextScanner(String input)
      {
         this.input = input.trim();
      }

      /**
       * Returns true if we have reached the end of the input.
       */
      public boolean  empty()
      {
         return (position == input.length());
      }

      protected boolean  isWhitespace(int c)
      {
         return (c==' ' || c=='\n' || c=='\r' || c =='\t');
      }

      public void  skipWhitespace()
      {
         while (position < input.length()) {
            if (!isWhitespace(input.charAt(position)))
               break;
            position++;
         }
      }

      protected boolean  isEOL(int c)
      {
         return (c=='\n' || c=='\r');
      }

      // Skip the sequence: <space>*(<comma><space>)?
      // Returns true if we found a comma in there.
      public boolean  skipCommaWhitespace()
      {
         skipWhitespace();
         if (position == input.length())
            return false;
         if (!(input.charAt(position) == ','))
            return false;
         position++;
         skipWhitespace();
         return true;
      }


      public Float  nextFloat()
      {
         int  floatEnd = scanForFloat();
         if (floatEnd == position)
            return null;
         Float  result = Float.parseFloat(input.substring(position, floatEnd));
         position = floatEnd;
         return result;
      }

      /*
       * Scans for a comma-whitespace sequence with a float following it.
       * If found, the float is returned. Otherwise null is returned and
       * the scan position left as it was.
       */
      public Float  possibleNextFloat()
      {
         int  start = position;
         skipCommaWhitespace();
         Float  result = nextFloat();
         if (result != null)
            return result;
         position = start;
         return null;
      }

      public Integer  nextInteger()
      {
         int  intEnd = scanForInteger();
         //System.out.println("nextFloat: "+position+" "+floatEnd);
         if (intEnd == position)
            return null;
         Integer  result = Integer.parseInt(input.substring(position, intEnd));
         position = intEnd;
         return result;
      }

      public Integer  nextChar()
      {
         if (position == input.length())
            return null;
         return Integer.valueOf(input.charAt(position++));
      }

      public Length  nextLength()
      {
         Float  scalar = nextFloat();
         if (scalar == null)
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
      public Boolean  nextFlag()
      {
         if (position == input.length())
            return null;
         char  ch = input.charAt(position);
         if (ch == '0' || ch == '1') {
            position++;
            return Boolean.valueOf(ch == '1');
         }
         return null;
      }


      public boolean  consume(char ch)
      {
         boolean  found = (position < input.length() && input.charAt(position) == ch);
         if (found)
            position++;
         return found;
      }


      public boolean  consume(String str)
      {
         int  len = str.length();
         boolean  found = (position <= (input.length() - len) && input.substring(position,position+len).equals(str));
         if (found)
            position += len;
         return found;
      }


      protected int  advanceChar()
      {
         if (position == input.length())
            return -1;
         position++;
         if (position < input.length())
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
      public String  nextToken()
      {
         return nextToken(' ');
      }

      /*
       * Scans the input starting immediately at 'position' for the next token.
       * A token is a sequence of characters terminating at either a whitespace character
       * or the supplied terminating character.
       */
      public String  nextToken(char terminator)
      {
         if (empty())
            return null;

         int  ch = input.charAt(position);
         if (isWhitespace(ch) || ch == terminator)
            return null;
         
         int  start = position;
         ch = advanceChar();
         while (ch != -1 && ch != terminator && !isWhitespace(ch)) {
            ch = advanceChar();
         }
         return input.substring(start, position);
      }

      /*
       * Scans the input starting immediately at 'position' for the a sequence
       * of letter characters terminated by an open bracket.  The function
       * name is returned.
       */
      public String  nextFunction()
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
       * Scans the input starting immediately at 'position' for a floating point number.
       * If one is found, the end position of the float will be returned.
       * If the returned value is the same as 'position' then no float was found.
       */
      private int  scanForFloat()
      {
         if (empty())
            return position;
         int  lastValidPos = position;
         int  start = position;

         int  ch = input.charAt(position);
         // Check whole part of mantissa
         if (ch == '-' || ch == '+')
            ch = advanceChar();
         if (Character.isDigit(ch)) {
            lastValidPos = position + 1;
            ch = advanceChar();
            while (Character.isDigit(ch)) {
               lastValidPos = position + 1;
               ch = advanceChar();
            }
         }
         // Fraction or exponent starts here
         if (ch == '.') {
            lastValidPos = position + 1;
            ch = advanceChar();
            while (Character.isDigit(ch)) {
               lastValidPos = position + 1;
               ch = advanceChar();
            }
         }
         // Exponent
         if (ch == 'e' || ch == 'E') {
            ch = advanceChar();
            if (ch == '-' || ch == '+')
               ch = advanceChar();
            if (Character.isDigit(ch)) {
               lastValidPos = position + 1;
               ch = advanceChar();
               while (Character.isDigit(ch)) {
                  lastValidPos = position + 1;
                  ch = advanceChar();
               }
            }
         }

         position = start;
         return lastValidPos;
      }

      /*
       * Scans the input starting immediately at 'position' for an integer number.
       * If one is found, the end position of the float will be returned.
       * If the returned value is the same as 'position' then no number was found.
       */
      private int  scanForInteger()
      {
         if (empty())
            return position;
         int  lastValidPos = position;
         int  start = position;

         int  ch = input.charAt(position);
         // Check whole part of mantissa
         if (ch == '-' || ch == '+')
            ch = advanceChar();
         if (Character.isDigit(ch)) {
            lastValidPos = position + 1;
            ch = advanceChar();
            while (Character.isDigit(ch)) {
               lastValidPos = position + 1;
               ch = advanceChar();
            }
         }

         position = start;
         return lastValidPos;
      }

      /*
       * Get the next few chars. Mainly used for error messages.
       */
      public String  ahead()
      {
         int start = position;
         while (!empty() && !isWhitespace(input.charAt(position)))
            position++;
         String  str = input.substring(start, position);
         position = start;
         return str;
      }

      public Unit  nextUnit()
      {
         if (empty())
            return null;
         int  ch = input.charAt(position);
         if (ch == '%') {
            position++;
            return Unit.percent;
         }
         if (position > (input.length() - 2))
            return null;
         try {
            Unit  result = Unit.valueOf(input.substring(position, position + 2));
            position +=2;
            return result;
         } catch (IllegalArgumentException e) {
            return null;
         }
      }

      /*
       * Check whether the next character is a letter.
       */
      public boolean  hasLetter()
      {
         if (position == input.length())
            return false;
         char  ch = input.charAt(position);
         return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'));
      }

      /*
       * Extract a quoted string from the input.
       */
      public String  nextQuotedString()
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
      public String  restOfText()
      {
         if (empty())
            return null;

         int  start = position;
         position = input.length();
         return input.substring(start);
      }

   }


   //=========================================================================
   // Attribute parsing
   //=========================================================================


   private void  parseAttributesCore(SvgElementBase obj, Attributes attributes)
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         String  qname = attributes.getQName(i);
         if (qname.equals("id") || qname.equals("xml:id")) {
            obj.id = attributes.getValue(i).trim();
            break;
         }
      }
   }


   /*
    * Parse the style attributes for an element.
    */
   private void  parseAttributesStyle(SvgElementBase obj, Attributes attributes) throws SAXException
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
   private static void  parseStyle(SvgElementBase obj, String style) throws SAXException
   {
      TextScanner  scan = new TextScanner(style.replaceAll("/\\*.*?\\*/", ""));  // regex strips block comments

      while (true)
      {
         String  propertyName = scan.nextToken(':');
         scan.skipWhitespace();
         if (!scan.consume(':'))
            break;  // Syntax error. Stop processing CSS rules.
         scan.skipWhitespace();
         String  propertyValue = scan.nextToken(';');
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


   protected static void  processStyleProperty(Style style, String localName, String val) throws SAXException
   {
      if (val.length() == 0) { // The spec doesn't say how to handle empty style attributes.
         return;               // Our strategy is just to ignore them.
      }
      if (val.equals("inherit"))
         return;

      switch (SVGAttr.fromString(localName))
      {
         case fill:
            style.fill = parsePaintSpecifier(val, "fill");
            style.specifiedFlags |= SVG.SPECIFIED_FILL;
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
            style.stroke = parsePaintSpecifier(val, "stroke");
            style.specifiedFlags |= SVG.SPECIFIED_STROKE;
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
            style.color = parseColour(val);
            style.specifiedFlags |= SVG.SPECIFIED_COLOR;
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
            if (val.indexOf('|') >= 0 || (VALID_DISPLAY_VALUES.indexOf('|'+val+'|') == -1))
               throw new SAXException("Invalid value for \"display\" attribute: "+val);
            style.display = !val.equals(NONE);
            style.specifiedFlags |= SVG.SPECIFIED_DISPLAY;
            break;

         case visibility:
            if (val.indexOf('|') >= 0 || (VALID_VISIBILITY_VALUES.indexOf('|'+val+'|') == -1))
               throw new SAXException("Invalid value for \"visibility\" attribute: "+val);
            style.visibility = val.equals("visible");
            style.specifiedFlags |= SVG.SPECIFIED_VISIBILITY;
            break;

         case stop_color:
            if (val.equals(CURRENTCOLOR)) {
               style.stopColor = CurrentColor.getInstance();
            } else {
               style.stopColor = parseColour(val);
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
               style.solidColor = parseColour(val);
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
               style.viewportFill = parseColour(val);
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

         default:
            break;
      }
   }


   private void  parseAttributesViewBox(SVG.SvgViewBoxContainer obj, Attributes attributes) throws SAXException
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


   private void  parseAttributesTransform(SVG.HasTransform obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.transform)
         {
            obj.setTransform( parseTransformList(attributes.getValue(i)) );
         }
      }
   }


   private Matrix  parseTransformList(String val) throws SAXException
   {
      Matrix  matrix = new Matrix();

      TextScanner  scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         String  cmd = scan.nextFunction();

         if (cmd == null)
            throw new SAXException("Bad transform function encountered in transform list: "+val);

         if (cmd.equals("matrix"))
         {
            scan.skipWhitespace();
            Float a = scan.nextFloat();
            scan.skipCommaWhitespace();
            Float b = scan.nextFloat();
            scan.skipCommaWhitespace();
            Float c = scan.nextFloat();
            scan.skipCommaWhitespace();
            Float d = scan.nextFloat();
            scan.skipCommaWhitespace();
            Float e = scan.nextFloat();
            scan.skipCommaWhitespace();
            Float f = scan.nextFloat();
            scan.skipWhitespace();

            if (f == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            Matrix m = new Matrix();
            m.setValues(new float[] {a, c, e, b, d, f, 0, 0, 1});
            matrix.preConcat(m);
         }
         else if (cmd.equals("translate"))
         {
            scan.skipWhitespace();
            Float  tx = scan.nextFloat();
            Float  ty = scan.possibleNextFloat();
            scan.skipWhitespace();

            if (tx == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            if (ty == null)
               matrix.preTranslate(tx, 0f);
            else
               matrix.preTranslate(tx, ty);
         }
         else if (cmd.equals("scale"))
         {
            scan.skipWhitespace();
            Float  sx = scan.nextFloat();
            Float  sy = scan.possibleNextFloat();
            scan.skipWhitespace();

            if (sx == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            if (sy == null)
               matrix.preScale(sx, sx);
            else
               matrix.preScale(sx, sy);
         }
         else if (cmd.equals("rotate"))
         {
            scan.skipWhitespace();
            Float  ang = scan.nextFloat();
            Float  cx = scan.possibleNextFloat();
            Float  cy = scan.possibleNextFloat();
            scan.skipWhitespace();

            if (ang == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            if (cx == null) {
               matrix.preRotate(ang);
            } else if (cy != null) {
               matrix.preRotate(ang, cx, cy);
            } else {
               throw new SAXException("Invalid transform list: "+val);
            }
         }
         else if (cmd.equals("skewX"))
         {
            scan.skipWhitespace();
            Float  ang = scan.nextFloat();
            scan.skipWhitespace();

            if (ang == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            matrix.preSkew((float) Math.tan(Math.toRadians(ang)), 0f);
         }
         else if (cmd.equals("skewY"))
         {
            scan.skipWhitespace();
            Float  ang = scan.nextFloat();
            scan.skipWhitespace();

            if (ang == null || !scan.consume(')'))
               throw new SAXException("Invalid transform list: "+val);

            matrix.preSkew(0f, (float) Math.tan(Math.toRadians(ang)));
         }
         else if (cmd != null) {
            throw new SAXException("Invalid transform list fn: "+cmd+")");
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
   private static Length  parseLength(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid length value (empty string)");
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
            unit = Unit.valueOf(unitStr);
         } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid length unit specifier: "+val);
         }
      }
      try
      {
         float scalar = Float.parseFloat(val.substring(0, end));
         return new Length(scalar, unit);
      }
      catch (NumberFormatException e)
      {
         throw new SAXException("Invalid length value: "+val, e);
      }
   }


   /*
    * Parse a list of Length/Coords
    */
   private static List<Length>  parseLengthList(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid length list (empty string)");

      List<Length>  coords = new ArrayList<Length>(1);

      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      while (!scan.empty())
      {
         Float scalar = scan.nextFloat();
         if (scalar == null)
            throw new SAXException("Invalid length list value: "+scan.ahead());
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
   private static float  parseFloat(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid float value (empty string)");
      try
      {
         return Float.parseFloat(val);
      }
      catch (NumberFormatException e)
      {
         throw new SAXException("Invalid float value: "+val, e);
      }
   }


   /*
    * Parse an opacity value (a float clamped to the range 0..1).
    */
   private static float  parseOpacity(String val) throws SAXException
   {
      float  o = parseFloat(val);
      return (o < 0f) ? 0f : (o > 1f) ? 1f : o;
   }


   /*
    * Parse a viewBox attribute.
    */
   private static Box  parseViewBox(String val) throws SAXException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      Float minX = scan.nextFloat();
      scan.skipCommaWhitespace();
      Float minY = scan.nextFloat();
      scan.skipCommaWhitespace();
      Float width = scan.nextFloat();
      scan.skipCommaWhitespace();
      Float height = scan.nextFloat();

      if (minX==null || minY==null || width == null || height == null)
         throw new SAXException("Invalid viewBox definition - should have four numbers");
      if (width < 0)
         throw new SAXException("Invalid viewBox. width cannot be negative");
      if (height < 0)
         throw new SAXException("Invalid viewBox. height cannot be negative");

      return new SVG.Box(minX, minY, width, height);
   }


   /*
    * 
    */
   private static void  parsePreserveAspectRatio(SVG.SvgPreserveAspectRatioContainer obj, String val) throws SAXException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      SVG.AspectRatioAlignment  align = null;
      SVG.AspectRatioScale      scale = null;

      String  word = scan.nextToken();
      if ("defer".equals(word)) {    // Ignore defer keyword
         scan.skipWhitespace();
         word = scan.nextToken();
      }
      align = aspectRatioKeywords.get(word);
      scan.skipWhitespace();

      if (!scan.empty()) {
         String meetOrSlice = scan.nextToken();
         if (meetOrSlice.equals("meet")) {
            scale = SVG.AspectRatioScale.MEET;
         } else if (meetOrSlice.equals("slice")) {
            scale = SVG.AspectRatioScale.SLICE;
         } else {
            throw new SAXException("Invalid preserveAspectRatio definition: "+val);
         }
      }
      obj.preserveAspectRatioAlignment = align;
      obj.preserveAspectRatioScale = scale;
   }


   /*
    * Parse a paint specifier such as in the fill and stroke attributes.
    */
   private static SvgPaint parsePaintSpecifier(String val, String attrName) throws SAXException
   {
      if (val.startsWith("url("))
      {
         int  closeBracket = val.indexOf(")"); 
         if (closeBracket == -1)
            throw new SAXException("Bad "+attrName+" attribute. Unterminated url() reference");

         String    href = val.substring(4, closeBracket).trim();
         SvgPaint  fallback = null;

         val = val.substring(closeBracket+1).trim();
         if (val.length() > 0)
            fallback = parseColourSpecifer(val);
         return new PaintReference(href, fallback);

      }
      return parseColourSpecifer(val);
   }


   private static SvgPaint parseColourSpecifer(String val) throws SAXException
   {
      if (val.equals(NONE)) {
         return null;
      } else if (val.equals(CURRENTCOLOR)) {
         return CurrentColor.getInstance();
      } else {
         return parseColour(val);
      }
   }


   /*
    * Parse a colour definition.
    */
   private static Colour  parseColour(String val) throws SAXException
   {
      if (val.charAt(0) == '#')
      {
         try
         {
            if (val.length() == 7) {
               return new Colour(Integer.parseInt(val.substring(1), 16));
            } else if (val.length() == 4) {
               int threehex = Integer.parseInt(val.substring(1), 16);
               int h1 = threehex & 0xf00;
               int h2 = threehex & 0x0f0;
               int h3 = threehex & 0x00f;
               return new Colour(h1<<16|h1<<12|h2<<8|h2<<4|h3<<4|h3);
            } else {
               throw new SAXException("Bad hex colour value: "+val);
            }
         }
         catch (NumberFormatException e)
         {
            throw new SAXException("Bad colour value: "+val);
         }
      }
      if (val.toLowerCase(Locale.US).startsWith("rgb("))
      {
         TextScanner scan = new TextScanner(val.substring(4));
         scan.skipWhitespace();

         int red = parseColourComponent(scan);
         scan.skipCommaWhitespace();
         int green = parseColourComponent(scan);
         scan.skipCommaWhitespace();
         int blue = parseColourComponent(scan);

         scan.skipWhitespace();
         if (!scan.consume(')'))
            throw new SAXException("Bad rgb() colour value: "+val);
         return new Colour(red<<16 | green<<8 | blue);
      }
      // Must be a colour keyword
      else
         return parseColourKeyword(val);
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private static int  parseColourComponent(TextScanner scan) throws SAXException
   {
      int  comp = scan.nextInteger();
      if (scan.consume('%')) {
         // Spec says to follow CSS rules, which says out-of-range values should be clamped.
         comp = (comp < 0) ? 0 : (comp > 100) ? 100 : comp;
         return (comp * 255 / 100);
      } else {
         comp = (comp < 0) ? 0 : (comp > 255) ? 255 : comp;
         return comp;
      }
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private static Colour  parseColourKeyword(String name) throws SAXException
   {
      Integer  col = colourKeywords.get(name.toLowerCase(Locale.US));
      if (col == null) {
         throw new SAXException("Invalid colour keyword: "+name);
      }
      return new Colour(col.intValue());
   }


   // Parse a font attribute
   // [ [ <'font-style'> || <'font-variant'> || <'font-weight'> ]? <'font-size'> [ / <'line-height'> ]? <'font-family'> ] | caption | icon | menu | message-box | small-caption | status-bar | inherit
   private static void  parseFont(Style style, String val) throws SAXException
   {
      List<String>     fontFamily = null;
      Length           fontSize = null;
      Integer          fontWeight = null;
      Style.FontStyle  fontStyle = null;
      String           fontVariant = null;

      // Start by checking for the fixed size standard system font names (which we don't support)
      if ("|caption|icon|menu|message-box|small-caption|status-bar|".indexOf('|'+val+'|') != -1)
         return;
         
      // Fist part: style/variant/weight (opt - one or more)
      TextScanner  scan = new TextScanner(val);
      String item = null;
      while (true)
      {
         item = scan.nextToken('/');
         scan.skipWhitespace();
         if (item == null)
            throw new SAXException("Invalid font style attribute: missing font size and family");
         if (fontWeight != null && fontStyle != null)
            break;
         if (item.equals("normal"))  // indeterminate which of these this refers to
            continue;
         if (fontWeight == null) {
            fontWeight = fontWeightKeywords.get(item);
            if (fontWeight != null)
               continue;
         }
         if (fontStyle == null) {
            fontStyle = fontStyleKeywords.get(item);
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
      fontSize = parseFontSize(item);

      // Check for line-height (which we don't support)
      if (scan.consume('/'))
      {
         scan.skipWhitespace();
         item = scan.nextToken();
         if (item == null)
            throw new SAXException("Invalid font style attribute: missing line-height");
         parseLength(item);
         scan.skipWhitespace();
      }
      
      // Third part: font family
      fontFamily = parseFontFamily(scan.restOfText());

      style.fontFamily = fontFamily;
      style.fontSize = fontSize;
      style.fontWeight = (fontWeight == null) ? Style.FONT_WEIGHT_NORMAL : fontWeight;
      style.fontStyle = (fontStyle == null) ? Style.FontStyle.Normal : fontStyle;
      style.specifiedFlags |= (SVG.SPECIFIED_FONT_FAMILY | SVG.SPECIFIED_FONT_SIZE | SVG.SPECIFIED_FONT_WEIGHT | SVG.SPECIFIED_FONT_STYLE);
   }


   // Parse a font family list
   private static List<String>  parseFontFamily(String val) throws SAXException
   {
      List<String> fonts = null;
      TextScanner  scan = new TextScanner(val);
      while (true)
      {
         String item = scan.nextQuotedString();
         if (item == null)
            item = scan.nextToken(',');
         if (item == null)
            break;
         if (fonts == null)
            fonts = new ArrayList<String>();
         fonts.add(item);
         scan.skipCommaWhitespace();
         if (scan.empty())
            break;
      }
      return fonts;
   }


   // Parse a font size keyword or numerical value
   private static Length  parseFontSize(String val) throws SAXException
   {
      Length  size = fontSizeKeywords.get(val);
      if (size == null) {
         size = parseLength(val);
      }
      return size;
   }


   // Parse a font weight keyword or numerical value
   private static Integer  parseFontWeight(String val) throws SAXException
   {
      Integer  wt = fontWeightKeywords.get(val);
      if (wt == null) {
         throw new SAXException("Invalid font-weight property: "+val);
      }
      return wt;
   }


   // Parse a font style keyword
   private static Style.FontStyle  parseFontStyle(String val) throws SAXException
   {
      Style.FontStyle  fs = fontStyleKeywords.get(val);
      if (fs == null) {
         throw new SAXException("Invalid font-style property: "+val);
      }
      return fs;
   }


   // Parse a text decoration keyword
   private static TextDecoration  parseTextDecoration(String val) throws SAXException
   {
      if ("none".equals(val))
         return Style.TextDecoration.None;
      if ("underline".equals(val))
         return Style.TextDecoration.Underline;
      if ("overline".equals(val))
         return Style.TextDecoration.Overline;
      if ("line-through".equals(val))
         return Style.TextDecoration.LineThrough;
      if ("blink".equals(val))
         return Style.TextDecoration.Blink;
      throw new SAXException("Invalid text-decoration property: "+val);
   }


   // Parse a text decoration keyword
   private static TextDirection  parseTextDirection(String val) throws SAXException
   {
      if ("ltr".equals(val))
         return Style.TextDirection.LTR;
      if ("rtl".equals(val))
         return Style.TextDirection.RTL;
      throw new SAXException("Invalid direction property: "+val);
   }


   // Parse fill rule
   private static Style.FillRule  parseFillRule(String val) throws SAXException
   {
      if ("nonzero".equals(val))
         return Style.FillRule.NonZero;
      if ("evenodd".equals(val))
         return Style.FillRule.EvenOdd;
      throw new SAXException("Invalid fill-rule property: "+val);
   }


   // Parse stroke-linecap
   private static Style.LineCaps  parseStrokeLineCap(String val) throws SAXException
   {
      if ("butt".equals(val))
         return Style.LineCaps.Butt;
      if ("round".equals(val))
         return Style.LineCaps.Round;
      if ("square".equals(val))
         return Style.LineCaps.Square;
      throw new SAXException("Invalid stroke-linecap property: "+val);
   }


   // Parse stroke-linejoin
   private static Style.LineJoin  parseStrokeLineJoin(String val) throws SAXException
   {
      if ("miter".equals(val))
         return Style.LineJoin.Miter;
      if ("round".equals(val))
         return Style.LineJoin.Round;
      if ("bevel".equals(val))
         return Style.LineJoin.Bevel;
      throw new SAXException("Invalid stroke-linejoin property: "+val);
   }


   // Parse stroke-dasharray
   private static Length[]  parseStrokeDashArray(String val) throws SAXException
   {
      TextScanner scan = new TextScanner(val);
      scan.skipWhitespace();

      if (scan.empty())
         return null;
      
      Length dash = scan.nextLength();
      if (dash == null)
         return null;
      if (dash.isNegative())
         throw new SAXException("Invalid stroke-dasharray. Dash segemnts cannot be negative: "+val);

      float sum = dash.floatValue();

      List<Length> dashes = new ArrayList<Length>();
      dashes.add(dash);
      while (!scan.empty())
      {
         scan.skipCommaWhitespace();
         dash = scan.nextLength();
         if (dash == null)  // must have hit something unexpected
            throw new SAXException("Invalid stroke-dasharray. Non-Length content found: "+val);
         if (dash.isNegative())
            throw new SAXException("Invalid stroke-dasharray. Dash segemnts cannot be negative: "+val);
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
   private static Style.TextAnchor  parseTextAnchor(String val) throws SAXException
   {
      if ("start".equals(val))
         return Style.TextAnchor.Start;
      if ("middle".equals(val))
         return Style.TextAnchor.Middle;
      if ("end".equals(val))
         return Style.TextAnchor.End;
      throw new SAXException("Invalid text-anchor property: "+val);
   }


   // Parse a text anchor keyword
   private static Boolean  parseOverflow(String val) throws SAXException
   {
      if ("visible".equals(val) || "auto".equals(val))
         return Boolean.TRUE;
      if ("hidden".equals(val) || "scroll".equals(val))
         return Boolean.FALSE;
      throw new SAXException("Invalid toverflow property: "+val);
   }


   // Parse CSS clip shape (always a rect())
   private static CSSClipRect  parseClip(String val) throws SAXException
   {
      if ("auto".equals(val))
         return null;
      if (!val.toLowerCase(Locale.US).startsWith("rect("))
         throw new SAXException("Invalid clip attribute shape. Only rect() is supported.");

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
         throw new SAXException("Bad rect() clip definition: "+val);

      return new CSSClipRect(top, right, bottom, left);
   }


   private static Length parseLengthOrAuto(TextScanner scan)
   {
      if (scan.consume("auto"))
         return new Length(0f);

      return scan.nextLength();
   }


   // Parse a vector effect keyword
   private static VectorEffect  parseVectorEffect(String val) throws SAXException
   {
      if ("none".equals(val))
         return Style.VectorEffect.None;
      if ("non-scaling-stroke".equals(val))
         return Style.VectorEffect.NonScalingStroke;
      throw new SAXException("Invalid vector-effect property: "+val);
   }


   //=========================================================================


   // Parse the string that defines a path.
   private static SVG.PathDefinition  parsePath(String val) throws SAXException
   {
      TextScanner  scan = new TextScanner(val);

      int     pathCommand = '?';
      float   currentX = 0f, currentY = 0f;    // The last point visited in the subpath
      float   lastMoveX = 0f, lastMoveY = 0f;  // The initial point of current subpath
      float   lastControlX = 0f, lastControlY = 0f;  // Last control point of the just completed bezier curve.
      Float   x,y, x1,y1, x2,y2;
      Float   rx,ry, xAxisRotation;
      Boolean largeArcFlag, sweepFlag;

      SVG.PathDefinition  path = new SVG.PathDefinition();

      if (scan.empty())
         return path;

      pathCommand = scan.nextChar();

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
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               y1 = scan.nextFloat();
               scan.skipCommaWhitespace();
               x2 = scan.nextFloat();
               scan.skipCommaWhitespace();
               y2 = scan.nextFloat();
               scan.skipCommaWhitespace();
               x = scan.nextFloat();
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               y2 = scan.nextFloat();
               scan.skipCommaWhitespace();
               x = scan.nextFloat();
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               if (x == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               y1 = scan.nextFloat();
               scan.skipCommaWhitespace();
               x = scan.nextFloat();
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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
               scan.skipCommaWhitespace();
               ry = scan.nextFloat();
               scan.skipCommaWhitespace();
               xAxisRotation = scan.nextFloat();
               scan.skipCommaWhitespace();
               largeArcFlag = scan.nextFlag();
               scan.skipCommaWhitespace();
               sweepFlag = scan.nextFlag();
               scan.skipCommaWhitespace();
               x = scan.nextFloat();
               scan.skipCommaWhitespace();
               y = scan.nextFloat();
               if (y == null || rx < 0 || ry < 0) {
                  Log.e(TAG, "Bad path coords for "+pathCommand+" path segment");
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

         scan.skipWhitespace();
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
   private static Set<String>  parseRequiredFeatures(String val) throws SAXException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<String>();

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
   private static Set<String>  parseSystemLanguage(String val) throws SAXException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<String>();

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
   private static Set<String>  parseRequiredFormats(String val) throws SAXException
   {
      TextScanner      scan = new TextScanner(val);
      HashSet<String>  result = new HashSet<String>();

      while (!scan.empty())
      {
         String mimetype = scan.nextToken();
         result.add(mimetype);
         scan.skipWhitespace();
      }
      return result;
   }


   private static String  parseFunctionalIRI(String val, String attrName) throws SAXException
   {
      if (val.equals(NONE))
         return null;
      if (!val.startsWith("url(") || !val.endsWith(")"))
         throw new SAXException("Bad "+attrName+" attribute. Expected \"none\" or \"url()\" format");

      return val.substring(4, val.length()-1).trim();
      // Unlike CSS, the SVG spec seems to indicate that quotes are not allowed in "url()" references
   }


   //=========================================================================
   // Parsing <style> element. Very basic CSS parser.
   //=========================================================================


   private void  style(Attributes attributes) throws SAXException
   {
      debug("<style>");

      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");

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


   private void  parseCSSStyleSheet(String sheet) throws SAXException
   {
      CSSParser  cssp = new CSSParser(MediaType.screen);
      svgDocument.addCSSRules(cssp.parse(sheet));
   }

}
