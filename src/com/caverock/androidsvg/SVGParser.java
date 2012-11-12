package com.caverock.androidsvg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Matrix;
import android.graphics.Path;
import android.util.Log;

import com.caverock.androidsvg.SVG.Box;
import com.caverock.androidsvg.SVG.Colour;
import com.caverock.androidsvg.SVG.Length;
import com.caverock.androidsvg.SVG.Style;
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
   private static final String  TAG_CIRCLE         = "circle";
   private static final String  TAG_DEFS           = "defs";
   private static final String  TAG_ELLIPSE        = "ellipse";
   private static final String  TAG_G              = "g";
   private static final String  TAG_LINE           = "line";
   private static final String  TAG_LINEARGRADIENT = "linearGradient";
   private static final String  TAG_PATH           = "path";
   private static final String  TAG_POLYGON        = "polygon";
   private static final String  TAG_POLYLINE       = "polyline";
   private static final String  TAG_RADIALGRADIENT = "radialGradient";
   private static final String  TAG_RECT           = "rect";
   private static final String  TAG_STOP           = "stop";
   private static final String  TAG_TEXT           = "text";
   private static final String  TAG_TEXTPATH       = "textPath";
   private static final String  TAG_TSPAN          = "tspan";
   private static final String  TAG_USE            = "use";

   // Supported SVG attributes
   private enum  SVGAttr
   {
      cx, cy,
      fx, fy,
      d,
      fill,
      fill_rule,
      fill_opacity,
      // Font properties
      font_family, font_size, font_weight, font_style, // font, font_size_adjust, font_stretch, font_variant,  
      gradientTransform,
      height,
      href,
      id,
      opacity,
      pathLength,
      points,
      r,
      rx, ry,
      stroke,
      //stroke_fill,
      stroke_linecap,
      stroke_opacity,
      stroke_width,
      //style,
      text_decoration,
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
   private static HashMap<String, Length> fontSizeKeywords = new HashMap<String, Length>();
   private static HashMap<String, String> fontWeightKeywords = new HashMap<String, String>();

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
      fontSizeKeywords.put("smaller", new Length(0.833f, Unit.percent));
      fontSizeKeywords.put("larger", new Length(1.2f, Unit.percent));

      fontWeightKeywords.put("normal", "normal");
      fontWeightKeywords.put("bold", "bold");
      fontWeightKeywords.put("bolder", "bold");
      fontWeightKeywords.put("lighter", "normal");
      fontWeightKeywords.put("100", "normal");
      fontWeightKeywords.put("200", "normal");
      fontWeightKeywords.put("300", "normal");
      fontWeightKeywords.put("400", "normal");
      fontWeightKeywords.put("500", "normal");
      fontWeightKeywords.put("600", "bold");
      fontWeightKeywords.put("700", "bold");
      fontWeightKeywords.put("800", "bold");
      fontWeightKeywords.put("900", "bold");
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
         throw new SVGParseException("Invalid SVG file: "+e.getMessage(), e);
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
      } else if (localName.equalsIgnoreCase(TAG_DEFS)) {
         defs(attributes);
      } else if (localName.equalsIgnoreCase(TAG_USE)) {
         use(attributes);
      } else if (localName.equalsIgnoreCase(TAG_PATH)) {
         path(attributes);
      } else if (localName.equalsIgnoreCase(TAG_RECT)) {
         rect(attributes);
      } else if (localName.equalsIgnoreCase(TAG_CIRCLE)) {
         circle(attributes);
      } else if (localName.equalsIgnoreCase(TAG_ELLIPSE)) {
         ellipse(attributes);
      } else if (localName.equalsIgnoreCase(TAG_LINE)) {
         line(attributes);
      } else if (localName.equalsIgnoreCase(TAG_POLYLINE)) {
         polyline(attributes);
      } else if (localName.equalsIgnoreCase(TAG_POLYGON)) {
         polygon(attributes);
      } else if (localName.equalsIgnoreCase(TAG_TEXT)) {
         text(attributes);
      } else if (localName.equalsIgnoreCase(TAG_TSPAN)) {
         tspan(attributes);
      }
   }


   @Override
   public void characters(char[] ch, int start, int length) throws SAXException
   {
      super.characters(ch, start, length);

      if (currentElement instanceof SVG.Text || currentElement instanceof SVG.TSpan) {

         // The SAX parser can pass us several text nodes in a row. If this happens, we
         // want to collapse them all into one SVG.TextSequence node
         SVG.SvgContainer  parent = (SVG.SvgContainer) currentElement;
         int  numOlderSiblings = parent.children.size();
         SVG.SvgObject  previousSibling = (numOlderSiblings == 0) ? null : parent.children.get(numOlderSiblings-1);
         if (previousSibling instanceof SVG.TextSequence) {
            // Last sibling was a TextSequence also, so merge them.
            ((SVG.TextSequence) previousSibling).text += new String(ch, start, length);
         } else {
            // Add a new TextSequence to the child node list
            ((SVG.SvgContainer) currentElement).addChild(new SVG.TextSequence( new String(ch, start, length) ));
         }
      }

   }


   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (localName.equalsIgnoreCase(TAG_SVG) ||
          localName.equalsIgnoreCase(TAG_DEFS) ||
          localName.equalsIgnoreCase(TAG_G) ||
          localName.equalsIgnoreCase(TAG_TEXT) ||
          localName.equalsIgnoreCase(TAG_TSPAN)) {
         currentElement = currentElement.parent;
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


   private void dumpNode(SVG.SvgObject elem, String indent)
   {
      Log.d(TAG, indent+elem);
      if (elem instanceof SVG.SvgContainer) {
         indent = indent+"  ";
         for (SVG.SvgObject child: ((SVG.SvgContainer) elem).children) {
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
      obj.document = svgDocument;
      obj.parent = currentElement;
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
               break;
            case height:
               obj.height = parseLength(val);
               break;
            case viewBox:
               obj.viewBox = parseViewBox(val);
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
/**/Log.d(TAG, "<g>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Group  obj = new SVG.Group();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   //=========================================================================
   // <defs> group element


   private void  defs(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<defs>");
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
/**/Log.d(TAG, "<use>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Use  obj = new SVG.Use();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesUse(obj, attributes);
      currentElement.addChild(obj);
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
/**/Log.d(TAG,"***Use/href: "+attributes.getQName(i)+" "+attributes.getURI(i));
               obj.href = val;
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
/**/Log.d(TAG, "<path>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Path  obj = new SVG.Path();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
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
               obj.path = parsePath(val);
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
/**/Log.d(TAG, "<rect>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Rect  obj = new SVG.Rect();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
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
/**/Log.d(TAG, "<circle>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Circle  obj = new SVG.Circle();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
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
/**/Log.d(TAG, "<ellipse>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Ellipse  obj = new SVG.Ellipse();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
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
/**/Log.d(TAG, "<line>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Line  obj = new SVG.Line();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
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
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <polyline> element


   private void  polyline(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<polyline>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.PolyLine  obj = new SVG.PolyLine();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesPolyLine(obj, attributes);
      currentElement.addChild(obj);     
   }


   /*
    *  Parse the "points" attribute. Used by both <polyline> and <polygon>.
    */
   private void  parseAttributesPolyLine(SVG.PolyLine obj, Attributes attributes) throws SAXException
   {
      for (int i=0; i<attributes.getLength(); i++)
      {
         if (SVGAttr.fromString(attributes.getLocalName(i)) == SVGAttr.points)
         {
            ListTokeniser tok = new ListTokeniser(attributes.getValue(i).trim());
            int n = tok.countTokens();
            if (n % 2 == 1)
               throw new SAXException("Invalid <polyline> points attribute. There should be an even number of coordinates.");
            obj.points = new float[n]; 
            for (int j=0; j<n; j++) {
               obj.points[j] = parseFloat(tok.nextToken());
            }
         }
      }
   }


   //=========================================================================
   // <polygon> element


   private void  polygon(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<polygon>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Polygon  obj = new SVG.Polygon();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesPolyLine(obj, attributes); // reuse of polyline "points" parser
      currentElement.addChild(obj);     
   }


   //=========================================================================
   // <text> element


   private void  text(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<text>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      SVG.Text  obj = new SVG.Text();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesTransform(obj, attributes);
      parseAttributesText(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
   }


   private void  parseAttributesText(SVG.TextContainer obj, Attributes attributes) throws SAXException
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
            default:
               break;
         }
      }
   }


   //=========================================================================
   // <tspan> element


   private void  tspan(Attributes attributes) throws SAXException
   {
/**/Log.d(TAG, "<tspan>");
      if (currentElement == null)
         throw new SAXException("Invalid document. Root element must be <svg>");
      if (!(currentElement instanceof SVG.TextContainer))
         throw new SAXException("Invalid document. <tspan> elements are only valid inside <text> or <other <tspan>s.");
      SVG.TSpan  obj = new SVG.TSpan();
      obj.document = svgDocument;
      obj.parent = currentElement;
      parseAttributesCore(obj, attributes);
      parseAttributesStyle(obj, attributes);
      parseAttributesText(obj, attributes);
      currentElement.addChild(obj);
      currentElement = obj;
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
//Log.d(TAG, "<svg> id="+obj.id);
               break;
         }
      }
   }


   private void  parseAttributesStyle(SVG.SvgElement obj, Attributes attributes) throws SAXException
   {
//Log.d(TAG, "parseAttributesStyle");
      for (int i=0; i<attributes.getLength(); i++)
      {
         String val = attributes.getValue(i).trim();
         switch (SVGAttr.fromString(attributes.getLocalName(i)))
         {
            case fill:
               obj.style.specifiedFlags |= SVG.SPECIFIED_FILL;
               if (val.equals("none")) {
                  obj.style.fill = null;
               } else  if (val.startsWith("url")) {
                  //gradient
               } else {
                  obj.style.fill = parseColour(val);
               }
//Log.d(TAG, "<?> style.fill="+obj.style.fill);
               break;

            case fill_rule:
               obj.style.fillRule = parseFillRule(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_FILL_RULE;
               break;

            case fill_opacity:
               obj.style.fillOpacity = parseFloat(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_FILL_OPACITY;
               break;

            case stroke:
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE;
               if (val.equals("none")) {
               } else  if (val.startsWith("url")) {
                  //gradient
               } else {
                  obj.style.stroke = parseColour(val);
               }
//Log.d(TAG, "<?> style.stroke="+obj.style.stroke);
               break;

            case stroke_opacity:
               obj.style.strokeOpacity = parseFloat(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE_OPACITY;
               break;

            case stroke_width:
               obj.style.strokeWidth = parseLength(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE_WIDTH;
               break;

            case stroke_linecap:
               obj.style.strokeLineCap = parseStrokeLineCap(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_STROKE_LINECAP;
               break;

            case opacity:
               obj.style.opacity = parseFloat(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_OPACITY;
               break;

            case font_family:
               obj.style.fontFamily = val;
               obj.style.specifiedFlags |= SVG.SPECIFIED_FONT_FAMILY;
               break;

            case font_size:
               obj.style.fontSize = parseFontSize(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_FONT_SIZE;
               break;

            case font_weight:
               obj.style.fontWeight = val.toLowerCase();
               obj.style.specifiedFlags |= SVG.SPECIFIED_FONT_WEIGHT;
               break;

            case font_style:
               obj.style.fontStyle = parseFontStyle(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_FONT_STYLE;
               break;

            case text_decoration:
               obj.style.textDecoration = parseTextDecoration(val);
               obj.style.specifiedFlags |= SVG.SPECIFIED_TEXT_DECORATION;
               break;

            default:
               break;
         }
      }
   }


   private void  parseAttributesTransform(SVG.HasTransform obj, Attributes attributes) throws SAXException
   {
//Log.d(TAG, "parseAttributesTransform");
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
//Log.d(TAG, ">>>"+fn+"("+pars+")");
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
//Log.d(TAG, "skewX "+matrix);
                     continue;
                  }
               } else if (fn.equals("skewY")) {
                  if (parstok.countTokens() == 1) {
                     float ang = Float.parseFloat(parstok.nextToken());
                     matrix.preSkew(0f, (float) Math.tan(Math.toRadians(ang)));
//Log.d(TAG, "skewY "+matrix);
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
      Unit  unit = Unit.px;
      char  lastChar = val.charAt(end-1);

      if (lastChar == '%') {
         end -= 1;
         unit = Unit.percent;
      } else if (end > 2 && Character.isLetter(lastChar) && Character.isLetter(val.charAt(end-2))) {
         String unitStr = val.substring(end-2);
         if ("px|em|ex|in|cm|mm|pt|pc".indexOf(unitStr) >= 0) {
           end -= 2;
           unit = Unit.valueOf(unitStr);
         } else {
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
   private List<Length>  parseLengthList(String val) throws SAXException
   {
      if (val.length() == 0)
         throw new SAXException("Invalid length list (empty string)");

      List<Length>  coords = new ArrayList<Length>(1);
      ListTokeniser tok = new ListTokeniser(val);

      while (tok.hasMoreTokens())
      {
         coords.add(parseLength(tok.nextToken()));
      }
      return coords;
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
         float minX = Float.parseFloat(tok.nextToken());
         float minY = Float.parseFloat(tok.nextToken());
         float width = Float.parseFloat(tok.nextToken());
         float height = Float.parseFloat(tok.nextToken());
         if (width < 0)
            throw new SAXException("Invalid viewBox. width cannot be negative");
         if (height < 0)
            throw new SAXException("Invalid viewBox. height cannot be negative");
         return new SVG.Box(minX, minY, width, height);
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
         throw new SAXException("Bad colour value \""+val+"\"");
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
      val = val.toLowerCase();
      if (val.startsWith("rgb("))
      {
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
            throw new SAXException("Bad rgb() colour definition - should have three numbers");
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
      Integer  col = colourKeywords.get(name.toLowerCase());
      if (col == null)
         throw new SAXException("Invalid colour keyword: "+name);
      return new Colour(col.intValue());
   }


   // Parse a font size keyword or numerical value
   private Length  parseFontSize(String val) throws SAXException
   {
/**/Log.d(TAG, "parseFontSize: "+val);

      Length  size = fontSizeKeywords.get(val.toLowerCase());
      if (size == null) {
         size = parseLength(val);
      }
      return size;
   }


   // Parse a font weight keyword or numerical value
   private String  parseFontWeight(String val) throws SAXException
   {
/**/Log.d(TAG, "parseFontWeight: "+val);

      String  wt = fontWeightKeywords.get(val.toLowerCase());
      if (wt == null) {
         throw new SAXException("Invalid font-weight property: "+val);
      }
      return wt;
   }


   // Parse a font style keyword
   private String  parseFontStyle(String val) throws SAXException
   {
/**/Log.d(TAG, "parseFontStyle: "+val);

      val = val.toLowerCase();
      if ("normal".equals(val))
         return val;
      if ("italic".equals(val) || "oblique".equals(val))
         return "italic";
      throw new SAXException("Invalid font-style property: "+val);
   }


   // Parse a text decoration keyword
   private String  parseTextDecoration(String val) throws SAXException
   {
/**/Log.d(TAG, "parseTextDecoration: "+val);

      val = val.toLowerCase();
      if ("none".equals(val) || "overline".equals(val) || "blink".equals(val))
         return "none";
      if ("underline".equals(val) || "line-through".equals(val))
         return val;
      throw new SAXException("Invalid text-decoration property: "+val);
   }


   // Parse fill rule
   private Style.FillRule  parseFillRule(String val) throws SAXException
   {
/**/Log.d(TAG, "parseFillRule: "+val);

      val = val.toLowerCase();
      if ("nonzero".equals(val))
         return Style.FillRule.NonZero;
      if ("evenodd".equals(val))
         return Style.FillRule.EvenOdd;
      throw new SAXException("Invalid fill-rule property: "+val);
   }


   // Parse stroke-linecap
   private Style.LineCaps  parseStrokeLineCap(String val) throws SAXException
   {
/**/Log.d(TAG, "parseStrokeLineCap: "+val);

      val = val.toLowerCase();
      if ("butt".equals(val))
         return Style.LineCaps.Butt;
      if ("round".equals(val))
         return Style.LineCaps.Round;
      if ("square".equals(val))
         return Style.LineCaps.Square;
      throw new SAXException("Invalid stroke-linecap property: "+val);
   }


   //=========================================================================


   // Parse the string that defines a path.
   private Path  parsePath(String val) throws SAXException
   {
/**/Log.d(TAG, "parsePath: "+val);
      PathTokeniser tok = new PathTokeniser(val);

      char    pathCommand = '?';
      float   currentX = 0f, currentY = 0f;    // The last point visited in the subpath
      float   lastMoveX = 0f, lastMoveY = 0f;  // The initial point of current subpath
      float   lastControlX = 0f, lastControlY = 0f;  // Last control point of the just completed bezier curve.
      float   x,y, x1,y1, x2,y2;
      float   rx,ry, xAxisRotation;
      boolean largeArcFlag, sweepFlag;
      boolean startOfPath = true;              // Are we at the start of the whole path?
      Path    path = new Path();

      try
      {
         while (tok.hasMoreTokens())
         {
            if (isCommandLetter(tok.peekToken()))
               pathCommand = tok.nextToken().charAt(0);

            if (startOfPath && pathCommand != 'M' && pathCommand != 'm')
               return path;  // Invalid path - doesn't start with a move

            switch (pathCommand)
            {
                  // Move
               case 'M':
               case 'm':
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
                  // Relative moveto at the start of a path is treated as an absolute moveto.
                  if (pathCommand=='m' && !startOfPath) {
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
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
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
                  x1 = parseFloat(tok.nextToken());
                  y1 = parseFloat(tok.nextToken());
                  x2 = parseFloat(tok.nextToken());
                  y2 = parseFloat(tok.nextToken());
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
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
                  x2 = parseFloat(tok.nextToken());
                  y2 = parseFloat(tok.nextToken());
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
                  if (pathCommand=='c') {
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
                  x = parseFloat(tok.nextToken());
                  if (pathCommand=='h') {
                     x += currentX;
                  }
                  path.lineTo(x, currentY);
                  currentX = lastControlX = x;
                  break;

                  // Vertical line
               case 'V':
               case 'v':
                  y = parseFloat(tok.nextToken());
                  if (pathCommand=='v') {
                     y += currentY;
                  }
                  path.lineTo(currentX, y);
                  currentY = lastControlY = y;
                  break;

                  // Quadratic bezier
               case 'Q':
               case 'q':
                  x1 = parseFloat(tok.nextToken());
                  y1 = parseFloat(tok.nextToken());
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
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
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
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
                  rx = parseFloat(tok.nextToken());
                  ry = parseFloat(tok.nextToken());
                  xAxisRotation = parseFloat(tok.nextToken());
                  largeArcFlag = ("0".equals(tok.nextToken())) ? false : true;
                  sweepFlag = ("0".equals(tok.nextToken())) ? false : true;
                  x = parseFloat(tok.nextToken());
                  y = parseFloat(tok.nextToken());
                  if (pathCommand=='a') {
                     x += currentX;
                     y += currentY;
                  }
                  arcTo(path, currentX, currentY, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y);
                  currentX = lastControlX = x;
                  currentY = lastControlY = y;
                  break;

               default:
                  return path;
            }
            startOfPath = false;
         }
         return path;
      }
      catch (NoSuchElementException e)
      {
         throw new SAXException("Invalid <path> data");
      }
   }


   private boolean  isCommandLetter(String s)
   {
      return (s.length() == 1 && Character.isLetter(s.charAt(0)));
   }


   /*
    * Tokenises SVG path data.
    */
   private class PathTokeniser
   {
      private int nextToken = 0;
      private ArrayList<String>  list = new ArrayList<String>();
      
      public PathTokeniser(String src)
      {
         int start = 0;
         int pos = 0;
         boolean skipWs = false;
         while (pos < src.length())
         {
            char c = src.charAt(pos);
            if (Character.isLetter(c)) {
               if (!skipWs && pos>start) {
//Log.d(TAG, "pt0: "+src.substring(start, pos));
                  list.add(src.substring(start, pos));
               }
//Log.d(TAG, "pt1: "+c);
               list.add(String.valueOf(c));
               skipWs = true;
            } else if (c == ',' || Character.isWhitespace(c)) {
               if (!skipWs) {
//Log.d(TAG, "pt2: "+src.substring(start, pos));
                  list.add(src.substring(start, pos));
                  skipWs = true;
               }
               skipWs = true;
            } else if (skipWs) {
               skipWs = false;
               start = pos;
            }
            pos++;
         }
         if (!skipWs) {
//Log.d(TAG, "pt3: "+src.substring(start, pos));
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
         return list.get(nextToken++);
      }

      public String peekToken()
      {
         if (nextToken == list.size())
            throw new NoSuchElementException();
         return list.get(nextToken);
      }

      //public int countTokens()
      //{
      //   return list.size() - nextToken;
      //}
   }


   //=========================================================================
   // Handling of Arcs

   /*
    * SVG arc representation uses "endpoint parameterisation" where we specify the endpoint of the arc.
    * This is to be consistent with the other path commands.  However we need to convert this to "centre point
    * parameterisation" in order to calculate the arc. Handily, the SVG spec provides all the required maths
    * in section "F.6 Elliptical arc implementation notes".
    * 
    * Some of this code has been borrowed from the Batik library (Apache-2 license).
    */

   private static void arcTo(Path path, float lastX, float lastY, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag, float x, float y)
   {
//Log.d(TAG,  "arcto: "+lastX+" "+lastY+" "+rx+" "+ry+" "+angle+" "+largeArcFlag+" "+sweepFlag+" "+x+" "+y);

      if (lastX == x && lastY == y) {
         // If the endpoints (x, y) and (x0, y0) are identical, then this
         // is equivalent to omitting the elliptical arc segment entirely.
         // (behaviour specified by the spec)
         return;
      }

      // Handle degenerate case (behaviour specified by the spec)
      if (rx == 0 || ry == 0) {
         path.lineTo(x, y);
         return;
      }

      // Sign of the radii is ignored (behaviour specified by the spec)
      rx = Math.abs(rx);
      ry = Math.abs(ry);

      // Convert angle from degrees to radians
      float  angleRad = (float) Math.toRadians(angle % 360.0);
      double cosAngle = Math.cos(angleRad);
      double sinAngle = Math.sin(angleRad);
      
      // We simplify the calculations by transforming the arc so that the origin is at the
      // midpoint calculated above followed by a rotation to line up the coordinate axes
      // with the axes of the ellipse.

      // Compute the midpoint of the line between the current and the end point
      double dx2 = (lastX - x) / 2.0;
      double dy2 = (lastY - y) / 2.0;

      // Step 1 : Compute (x1', y1') - the transformed start point
      double x1 = (cosAngle * dx2 + sinAngle * dy2);
      double y1 = (-sinAngle * dx2 + cosAngle * dy2);

      double rx_sq = rx * rx;
      double ry_sq = ry * ry;
      double x1_sq = x1 * x1;
      double y1_sq = y1 * y1;

      // Check that radii are large enough.
      // If they are not, the spec says to scale them up so they are.
      // This is to compensate for potential rounding errors/differences between SVG implementations.
      double radiiCheck = x1_sq / rx_sq + y1_sq / ry_sq;
      if (radiiCheck > 1) {
         rx = (float) Math.sqrt(radiiCheck) * rx;
         ry = (float) Math.sqrt(radiiCheck) * ry;
         rx_sq = rx * rx;
         ry_sq = ry * ry;
      }

      // Step 2 : Compute (cx1, cy1) - the transformed centre point
      double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
      double sq = ((rx_sq * ry_sq) - (rx_sq * y1_sq) - (ry_sq * x1_sq)) / ((rx_sq * y1_sq) + (ry_sq * x1_sq));
      sq = (sq < 0) ? 0 : sq;
      double coef = (sign * Math.sqrt(sq));
      double cx1 = coef * ((rx * y1) / ry);
      double cy1 = coef * -((ry * x1) / rx);

      // Step 3 : Compute (cx, cy) from (cx1, cy1)
      double sx2 = (lastX + x) / 2.0;
      double sy2 = (lastY + y) / 2.0;
      double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
      double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

      // Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
      double ux = (x1 - cx1) / rx;
      double uy = (y1 - cy1) / ry;
      double vx = (-x1 - cx1) / rx;
      double vy = (-y1 - cy1) / ry;
      double p, n;

      // Compute the angle start
      n = Math.sqrt((ux * ux) + (uy * uy));
      p = ux; // (1 * ux) + (0 * uy)
      sign = (uy < 0) ? -1.0 : 1.0;
      double angleStart = Math.toDegrees(sign * Math.acos(p / n));

      // Compute the angle extent
      n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
      p = ux * vx + uy * vy;
      sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
      double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
      if (!sweepFlag && angleExtent > 0) {
         angleExtent -= 360f;
      } else if (sweepFlag && angleExtent < 0) {
         angleExtent += 360f;
      }
      angleExtent %= 360f;
      angleStart %= 360f;

      // Many elliptical arc implementations including the Java2D and Android ones, only
      // support arcs that are axis aligned.  Therefore we need to substitute the arc
      // with bezier curves.  The following method call will generate the beziers for
      // a unit circle that covers the arc angles we want.
      float[]  bezierPoints = arcToBeziers(angleStart, angleExtent);

      // Calculate a transformation matrix that will move and scale these bezier points to the correct location.
      Matrix m = new Matrix();
      m.postScale(rx, ry);
      m.postRotate(angle);
      m.postTranslate((float) cx, (float) cy);
      m.mapPoints(bezierPoints);

      // The last point in the bezier set should match exactly the last coord pair in the arc (ie: x,y). But
      // considering all the mathematical manipulation we have been doing, it is bound to be off by a tiny
      // fraction. Experiments show that it can be up to around 0.00002.  So why don't we just set it to
      // exactly what it ought to be.
      bezierPoints[bezierPoints.length-2] = x;
      bezierPoints[bezierPoints.length-1] = y;

      // Final step is to add the bezier curves to the path
      for (int i=0; i<bezierPoints.length; i+=6)
      {
         path.cubicTo(bezierPoints[i], bezierPoints[i+1], bezierPoints[i+2], bezierPoints[i+3], bezierPoints[i+4], bezierPoints[i+5]);
      }
   }


   /*
    * Generate the control points and endpoints for a set of bezier curves that match
    * a circular arc starting from angle 'angleStart' and sweep the angle 'angleExtent'.
    * The circle the arc follows will be centred on (0,0) and have a radius of 1.0.
    * 
    * Each bezier can cover no more than 90 degrees, so the arc will be divided evenly
    * into a maximum of four curves.
    * 
    * The resulting control points will later be scaled and rotated to match the final
    * arc required.
    * 
    * The returned array has the format [x0,y0, x1,y1,...] and excludes the start point
    * of the arc.
    */
   private static float[]  arcToBeziers(double angleStart, double angleExtent)
   {
//Log.d(TAG, "arcToBeziers: "+angleStart+" "+angleExtent);
      int    numSegments = (int) Math.ceil(Math.abs(angleExtent) / 90.0);
      
      angleStart = Math.toRadians(angleStart);
      angleExtent = Math.toRadians(angleExtent);
      float  angleIncrement = (float) (angleExtent / numSegments);
      
      // The length of each control point vector is given by the following formula.
      double  controlLength = 4.0 / 3.0 * Math.sin(angleIncrement / 2.0) / (1.0 + Math.cos(angleIncrement / 2.0));
      
      float[] coords = new float[numSegments * 6];
      int     pos = 0;

//Log.d(TAG, "arcToBeziers: num="+numSegments+" contLen="+controlLength);
      for (int i=0; i<numSegments; i++)
      {
         double  angle = angleStart + i * angleIncrement;
         // Calculate the control vector at this angle
         double  dx = Math.cos(angle);
         double  dy = Math.sin(angle);
         // First control point
         coords[pos++]   = (float) (dx - controlLength * dy);
         coords[pos++] = (float) (dy + controlLength * dx);
//Log.d(TAG, "arcToBeziers: x1,y1 = "+coords[pos-2]+","+coords[pos-1]);
         // Second control point
         angle += angleIncrement;
         dx = Math.cos(angle);
         dy = Math.sin(angle);
         coords[pos++] = (float) (dx + controlLength * dy);
         coords[pos++] = (float) (dy - controlLength * dx);
//Log.d(TAG, "arcToBeziers: x2,y2 = "+coords[pos-2]+","+coords[pos-1]);
         // Endpoint of bezier
         coords[pos++] = (float) dx;
         coords[pos++] = (float) dy;
//Log.d(TAG, "arcToBeziers: x,y = "+coords[pos-2]+","+coords[pos-1]);
      }
      return coords;
   }









}
