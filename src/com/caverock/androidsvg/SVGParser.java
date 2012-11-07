package com.caverock.androidsvg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Matrix;
import android.util.Log;

import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.Length;
import com.caverock.androidsvg.SVG.Style;
import com.caverock.androidsvg.SVG.SvgElement;
import com.caverock.androidsvg.SVG.Unit;

/**
 * SVG parser code. Used by SVG class. Should not be called directly.
 * @author Paul
 *
 */
public class SVGParser extends DefaultHandler
{
   private static final String  TAG = "SVGParser";

   private static final String  NAMESPACE = "http://www.w3.org/2000/svg";

   // Parser settings
   private float    dpi = 96f;   // inches to pixels conversion
   SVG              svgDocument = null;
   SVG.SvgContainer currentElement = null;

   // Define SVG tags
   private static final String  TAG_SVG            = "svg";
   private static final String  TAG_DEFS           = "defs";
   private static final String  TAG_G              = "g";
   private static final String  TAG_LINE           = "line";
   private static final String  TAG_LINEARGRADIENT = "linearGradient";
   private static final String  TAG_PATH           = "path";
   private static final String  TAG_POLYGON        = "polygon";
   private static final String  TAG_RADIALGRADIENT = "radialGradient";
   private static final String  TAG_RECT           = "rect";
   private static final String  TAG_STOP           = "stop";
   private static final String  TAG_TEXT           = "text";
   private static final String  TAG_TEXTPATH       = "textPath";
   private static final String  TAG_TSPAN          = "tspan";

   // Supported SVG attributes
   private enum  SVGAttr
   {
      cx, cy,
      fx, fy,
      d,
      fill,
      fill_opacity,
      // Font properties
      font, font_family, font_size, // font_size_adjust, font_stretch, font_style, font_variant, font_weight,  
      gradientTransform,
      height,
      href,
      id,
      opacity,
      points,
      r,
      rx, ry,
      stroke,
      stroke_fill,
      stroke_linecap,
      stroke_opacity,
      stroke_width,
      style,
      transform,
      viewBox,
      width,
      x, y,
      x1, y1,
      x2, y2,
      unsupported;

      public static SVGAttr  fromString(String str)
      {
         try
         {
            return valueOf(str.replace('-', '_'));
         } 
         catch (Exception e)
         {
            return unsupported;
         }
      }

   }


   private static HashMap<String, Integer> colourKeywords = new HashMap<String, Integer>();

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
         xr.parse(new InputSource(is));
      }
      catch (IOException e)
      {
         throw new SVGParseException("File error", e);
      }
      catch (SAXException e)
      {
         throw new SVGParseException("Invalid SVG file", e);
      }
      catch (ParserConfigurationException e)
      {
         throw new SVGParseException("SVG Parser problem", e);
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
/**/Log.d(TAG, "startDocument");
      svgDocument = new SVG();
   }


   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
   {
      super.startElement(uri, localName, qName, attributes);
/**/Log.d(TAG, "startElement: "+localName);
      
      if (localName.equalsIgnoreCase(TAG_SVG)) {
         svg(attributes);
      } else if (localName.equalsIgnoreCase(TAG_G)) {
         g(attributes);
      } else if (localName.equalsIgnoreCase(TAG_RECT)) {
         rect(attributes);
      } else if (localName.equalsIgnoreCase(TAG_LINE)) {
         line(attributes);
      }
   }


   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      // TODO Auto-generated method stub
      super.characters(ch, start, length);
   }


   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (localName.equalsIgnoreCase(TAG_SVG) ||
          localName.equalsIgnoreCase(TAG_G)) {
         currentElement = currentElement.parent;
         //styleStack.pop();
      }

   }

   
   @Override
   public void endDocument() throws SAXException
   {
      super.endDocument();
/**/Log.d(TAG, "<svg> DOCUMENT END!");
// Dump document
dumpNode(svgDocument.getRootElement(), "");
   }


   private void dumpNode(SVG.SvgElement elem, String indent)
   {
      Log.d(TAG, indent+elem);
      if (elem instanceof SVG.SvgContainer) {
         indent = indent+"  ";
         for (SVG.SvgElement child: ((SVG.SvgContainer) elem).children) {
            dumpNode(child, indent);
         }
      }
   }


   //=========================================================================
   // <svg> element

   private void  svg(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<svg>");
      SVG.Svg  obj = new SVG.Svg();
      obj.parent = currentElement;
      obj.style = (obj.parent == null) ? new Style() : new Style(obj.parent.style);
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
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
            case width:
               obj.width = parseLength(val);
/**/Log.d(TAG, "<svg> width="+obj.width);
               break;
            case height:
               obj.height = parseLength(val);
               break;
            case viewBox:
               obj.viewBox = parseViewBox(val);
               break;
         }
      }
   }


   //=========================================================================
   // <g> group element


   private void  g(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<g>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Group  obj = new SVG.Group();
      obj.parent = currentElement;
      obj.style = new Style(obj.parent.style);
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesG(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesG(SVG.Group obj, Attributes attributes) throws SAXException
   {
   }


   //=========================================================================
   // <rect> element


   private void  rect(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<rect>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Rect  obj = new SVG.Rect();
      obj.parent = currentElement;
      obj.style = new Style(obj.parent.style);
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
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
                  throw new SAXException("Invalid <rect> element. Width cannot be negative");
               break;
            case height:
               obj.height = parseLength(val);
               if (obj.height.isNegative())
                  throw new SAXException("Invalid <rect> element. Height cannot be negative");
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
         }
      }
   }


   //=========================================================================
   // <line> element


   private void  line(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<line>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Line  obj = new SVG.Line();
      obj.parent = currentElement;
      obj.style = new Style(obj.parent.style);
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
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
/**/Log.d(TAG, "<line> x1="+obj.x1);
               break;
            case y1:
               obj.y1 = parseLength(val);
/**/Log.d(TAG, "<line> y1="+obj.y1);
               break;
            case x2:
               obj.x2 = parseLength(val);
/**/Log.d(TAG, "<line> x2="+obj.x2);
               break;
            case y2:
               obj.y2 = parseLength(val);
/**/Log.d(TAG, "<line> y2="+obj.y2);
               break;
         }
      }
   }


   //=========================================================================
   // Attribute parsing
   //=========================================================================


   private void  parseAttributesCore(SVG.SvgElement obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.id) {
               obj.id = attributes.getValue(i).trim();
/**/Log.d(TAG, "<svg> id="+obj.id);
               break;
         }
      }
   }


   private void  parseAttributesStyle(SVG.SvgElement obj, Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "parseAttributesStyle "+obj.style);
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case fill:
               obj.style.specifiedFlags |= SVG.SPECIFIED_FILL;
               if (val.equals("none")) {
                  obj.style.hasFill = false;
                  break;
               } else  if (val.startsWith("url")) {
                  //gradient
               } else {
                  obj.style.fill = parseColour(val);
               }
/**/Log.d(TAG, "<?> style.fill="+obj.style.fill);
               obj.style.hasFill = true;
               break;

            case fill_opacity:
               obj.style.fillOpacity = parseFloat(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_FILL_OPACITY;
               break;

            case stroke:
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE;
               if (val.equals("none")) {
                  obj.style.hasStroke = false;
                  break;
               } else  if (val.startsWith("url")) {
                  //gradient
               } else {
                  obj.style.stroke = parseColour(val);
               }
/**/Log.d(TAG, "<?> style.stroke="+obj.style.stroke);
               obj.style.hasStroke = true;
               break;

            case stroke_opacity:
               obj.style.strokeOpacity = parseFloat(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE_OPACITY;
               break;

            case stroke_width:
               obj.style.strokeWidth = parseLength(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE_WIDTH;
               break;
         }
      }
   }


   private void  parseAttributesTransform(SVG.Transformable obj, Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "parseAttributesTransform");
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.transform) {
            ListTokeniser tok = new ListTokeniser(attributes.getValue(i).trim());
            Matrix  matrix = new Matrix();

            while (tok.hasMoreTokens())
            {
               String  val = tok.nextToken();
               int bracket1 = val.indexOf('(');
               int bracket2 = val.indexOf(')');
               if (bracket1 > bracket2) {
                  throw new SAXException("Invalid transform attribute: "+val);
               }
               String fn = val.substring(0, bracket1).trim();
               String pars = val.substring(bracket1+1, bracket2).trim();
/**/Log.d(TAG, ">>>"+fn+"("+pars+")");
               ListTokeniser parstok = new ListTokeniser(pars);

               if (fn.equals("matrix")) {
                  if (parstok.countTokens() == 6) {
                     float[] vals = new float[9];
                     vals[0] = Float.parseFloat(parstok.nextToken());
                     vals[3] = Float.parseFloat(parstok.nextToken());
                     vals[1] = Float.parseFloat(parstok.nextToken());
                     vals[4] = Float.parseFloat(parstok.nextToken());
                     vals[2] = Float.parseFloat(parstok.nextToken());
                     vals[5] = Float.parseFloat(parstok.nextToken());
                     vals[6] = 0f;
                     vals[7] = 0f;
                     vals[8] = 1f;
                     Matrix m = new Matrix();
                     m.getValues(vals);
                     matrix.preConcat(m);
                     continue;
                  }
               } else if (fn.equals("translate")) {
                  if (parstok.countTokens() == 1) {
                     matrix.preTranslate(Float.parseFloat(parstok.nextToken()), 0f);
                     continue;
                  } else if (parstok.countTokens() == 2) {
                     float tx = Float.parseFloat(parstok.nextToken());
                     matrix.preTranslate(tx, Float.parseFloat(parstok.nextToken()));
                     continue;
                  }
               } else if (fn.equals("scale")) {
                  if (parstok.countTokens() == 1) {
                     float sx = Float.parseFloat(parstok.nextToken());
                     matrix.preScale(sx, sx);
                     continue;
                  } else if (parstok.countTokens() == 2) {
                     float sx = Float.parseFloat(parstok.nextToken());
                     matrix.preScale(sx, Float.parseFloat(parstok.nextToken()));
                     continue;
                  }
               } else if (fn.equals("rotate")) {
                  if (parstok.countTokens() == 1) {
                     matrix.preRotate(Float.parseFloat(parstok.nextToken()));
                     continue;
                  } else if (parstok.countTokens() == 3) {
                     float ang = Float.parseFloat(parstok.nextToken());
                     float cx = Float.parseFloat(parstok.nextToken());
                     float cy = Float.parseFloat(parstok.nextToken());
                     matrix.preRotate(ang, cx, cy);
                     continue;
                  }
               } else if (fn.equals("skewX")) {
                  if (parstok.countTokens() == 1) {
                     float ang = Float.parseFloat(parstok.nextToken());
                     matrix.preSkew((float) Math.tan(Math.toRadians(ang)), 0f);
/**/Log.d(TAG, "skewX "+matrix);
                     continue;
                  }
               } else if (fn.equals("skewY")) {
                  if (parstok.countTokens() == 1) {
                     float ang = Float.parseFloat(parstok.nextToken());
                     matrix.preSkew(0f, (float) Math.tan(Math.toRadians(ang)));
/**/Log.d(TAG, "skewY "+matrix);
                  }
               }
               else
                  throw new SAXException("Invalid transform attribute: "+val);
            }
            obj.setTransform(matrix);
         }
      }
   }


   /*
    * Parse an SVG 'Length' value (usually a coordinate).
    * Spec says: length ::= number ("em" | "ex" | "px" | "in" | "cm" | "mm" | "pt" | "pc" | "%")?
    */
   private Length  parseLength(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid length value (empty string)");
      int   end = val.length();
      Unit  unit = null;
      char  lastChar = val.charAt(end-1);

      if (lastChar == '%') {
         end -= 1;
         unit = Unit.percent;
      } else if (end > 2 && Character.isLetter(lastChar) && Character.isLetter(val.charAt(end-2))) {
         String unitStr = val.substring(end-2);
         if ("px|em|ex|in|cm|mm|pt|pc".indexOf(unitStr) >= 0) {
           end -= 2;
           unit = Unit.fromString(unitStr);
         } else {
            throw new SAXException("Invalid length unit specifier: "+val);
         }
      }
      try
      {
         float scalar = Float.parseFloat(val.substring(0, end));
         if (unit == null)
            unit = Unit.none;
         return new Length(scalar, unit);
      }
      catch (NumberFormatException e)
      {
         throw new SAXException("Invalid length value: "+val, e);
      }
   }


   /*
    * Parse a generic float value (eg. opacity).
    */
   private float  parseFloat(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid float value  (empty string)");
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
    * Tokenises an SVG list. Lists are a set of values seperated by whitespace and/or a comma.
    * Unlike StringTokenizer, this tokeniser only returns the values. No delimiters are returned.
    * Note: trim() the string before you pass it in.
    */
   private class ListTokeniser
   {
      private int nextToken = 0;
      private ArrayList<String>  list = new ArrayList<String>();
      
      public ListTokeniser(String src)
      {
         int start = 0;
         int pos = 0;
         boolean skipWs = false;
         boolean inBrackets = false;
         while (pos < src.length())
         {
            char c = src.charAt(pos);
            if (inBrackets) {
               if (c == ')') {
                  inBrackets = false;
               }
               pos++;
               continue;
            }
            if (c == '(') {
               inBrackets = true;
               pos++;
               continue;
            }
            if (c == ',') {
               list.add(src.substring(start, pos));
               skipWs = true;
            } else if (Character.isWhitespace(c)) {
               if (!skipWs) {
                  list.add(src.substring(start, pos));
                  skipWs = true;
               }
            } else if (skipWs) {
               skipWs = false;
               start = pos;
            }
            pos++;
         }
         if (!skipWs) {
            list.add(src.substring(start, pos));
         }
      }
      
      public boolean hasMoreTokens()
      {
         return nextToken < list.size();
      }
      
      public String nextToken()
      {
         if (nextToken == list.size())
            throw new NoSuchElementException();
//Log.d(TAG, "ListTokenizer.nextToken = "+list.get(nextToken));
         return list.get(nextToken++);
      }

      public int countTokens()
      {
         return list.size() - nextToken;
      }
   }


   /*
    * Parse a viewBox attribute.
    */
   private Box  parseViewBox(String val) throws SAXException
   {
      ListTokeniser tok = new ListTokeniser(val);
      try
      {
         SVG.Box b = new SVG.Box();
         b.minX = Float.parseFloat(tok.nextToken());
         b.minY = Float.parseFloat(tok.nextToken());
         b.width = Float.parseFloat(tok.nextToken());
         b.height = Float.parseFloat(tok.nextToken());
         return b;
      }
      catch (Exception e)
      {
         throw new SAXException("Invalid viewBox definition - should have four numbers");
      }
   }


   /*
    * Parse a colour specifier.
    */
   private Colour  parseColour(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid colour value \""+val+"\"");
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
               return new Colour(h1<<20|h1<<16|h2<<12|h2<<8|h3<<4|h3);
            } else {
               throw new SAXException("Invalid hex colour value");
            }
         }
         catch (NumberFormatException e)
         {
            throw new SAXException("Invalid colour value");
         }
      }
      val = val.toLowerCase();
      if (val.startsWith("rgb("))
      {
         int end = val.indexOf(')');
         ListTokeniser tok = new ListTokeniser(val.substring(4, val.indexOf(')')));
         try
         {
            int red = parseColourComponent(tok.nextToken());
            int green = parseColourComponent(tok.nextToken());
            int blue = parseColourComponent(tok.nextToken());
            return new Colour(red<<16 | green<<8 | blue);
         }
         catch (Exception e)
         {
            throw new SAXException("Invalid viewBox definition - should have four numbers");
         }
         
      }
      // Must be a colour keyword
      else
         return parseColourKeyword(val);
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private int  parseColourComponent(String val) throws SAXException
   {
      try
      {
         if (val.endsWith("%")) {
            int pct = Integer.parseInt(val.substring(0, val.length()-1));
            if (pct < 0 || pct > 100)
               throw new NumberFormatException();
            return (pct * 255 / 100);
         } else {
            int comp = Integer.parseInt(val);
            if (comp < 0 || comp > 255)
               throw new NumberFormatException();
            return comp;
         }
      }
      catch (NumberFormatException e)
      {
         throw new SAXException("Invalid rgb() colour component: "+val);
      }
   }


   // Parse a colour component value (0..255 or 0%-100%)
   private Colour  parseColourKeyword(String name) throws SAXException
   {
//Log.d(TAG, "parseColourKeyword: "+name);
      Integer  col = colourKeywords.get(name);
      if (col == null)
         throw new SAXException("Invalid colour keyword: "+name);
      return new Colour(col.intValue());
   }


   //=========================================================================










}
