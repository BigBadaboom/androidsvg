package com.caverock.androidsvg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

public class SVG
{
   private static final String  TAG = "AndroidSVG";

   private static final float   DEFAULT_DPI = 96;
   private static final int     DEFAULT_PICTURE_WIDTH = 512;
   private static final int     DEFAULT_PICTURE_HEIGHT = 512;

   private static final double  SQRT2 = 1.414213562373095;

   private static final List<SvgObject>  EMPTY_CHILD_LIST = new ArrayList<SvgObject>(0);


   private Svg     rootElement = null;

   // Metadata
   private String  svgVersion = "";
   private String  title = "";
   private String  desc = "";

   // Resolvers
   SVGExternalFileResolver  fileResolver = null;


   public enum Unit
   {
      px,
      em,
      ex,
      in,
      cm,
      mm,
      pt,
      pc,
      percent
   }

   public enum AspectRatioAlignment
   {
      none,
      xMinYMin,
      xMidYMin,
      xMaxYMin,
      xMinYMid,
      xMidYMid,
      xMaxYMid,
      xMinYMax,
      xMidYMax,
      xMaxYMax
   }


   public enum GradientSpread
   {
      pad,
      reflect,
      repeat
   }


   protected SVG()
   {
   }


   public static SVG  getFromInputStream(InputStream is) throws SVGParseException
   {
      SVGParser  parser = new SVGParser();
      return parser.parse(is);
   }

   
   public static SVG  getFromString(String svg) throws SVGParseException
   {
      SVGParser  parser = new SVGParser();
      return parser.parse(new ByteArrayInputStream(svg.getBytes()));
   }

   
   public static SVG  getFromResource(Context context, int resourceId) throws SVGParseException
   {
      SVGParser  parser = new SVGParser();
      return parser.parse(context.getResources().openRawResource(resourceId));
   }

   
   public static SVG  getFromAsset(AssetManager assetManager, String filename) throws SVGParseException, IOException
   {
      SVGParser  parser = new SVGParser();
      InputStream  is = assetManager.open(filename);
      SVG  svg = parser.parse(is);
      is.close();
      return svg;
   }


   public void  registerExternalFileResolver(SVGExternalFileResolver fileResolver)
   {
      this.fileResolver = fileResolver;
   }


   public SVG.Svg  getRootElement()
   {
      return rootElement;
   }


   public void setRootElement(SVG.Svg rootElement)
   {
      this.rootElement = rootElement;
   }


   //===============================================================================
   // Object sub-types used in the SVG object tree


   public static class  Box implements Cloneable
   {
      public float  minX, minY, width, height;

      public Box(float minX, float minY, float width, float height)
      {
         this.minX = minX;
         this.minY = minY;
         this.width = width;
         this.height = height;
      }

      public static Box  fromLimits(float minX, float minY, float maxX, float maxY)
      {
         return new Box(minX, minY, maxX-minX, maxY-minY);
      }

      public RectF  toRectF()
      {
         return new RectF(minX, minY, maxX(), maxY());
      }

      public float  maxX() { return minX + width; }
      public float  maxY() { return minY + height; }

      public void  union(Box other)
      {
         if (other.minX < minX) minX = other.minX;
         if (other.minY < minY) minY = other.minY;
         if (other.maxX() > maxX()) width = other.maxX() - minX;
         if (other.maxY() > maxY()) height = other.maxY() - minY;
      }

      public String toString() { return "["+minX+" "+minY+" "+width+" "+height+"]"; }
   }


   public static final long SPECIFIED_FILL              = (1<<0);
   public static final long SPECIFIED_FILL_RULE         = (1<<1);
   public static final long SPECIFIED_FILL_OPACITY      = (1<<2);
   public static final long SPECIFIED_STROKE            = (1<<3);
   public static final long SPECIFIED_STROKE_OPACITY    = (1<<4);
   public static final long SPECIFIED_STROKE_WIDTH      = (1<<5);
   public static final long SPECIFIED_STROKE_LINECAP    = (1<<6);
   public static final long SPECIFIED_STROKE_LINEJOIN   = (1<<7);
   public static final long SPECIFIED_STROKE_MITERLIMIT = (1<<8);
   public static final long SPECIFIED_STROKE_DASHARRAY  = (1<<9);
   public static final long SPECIFIED_STROKE_DASHOFFSET = (1<<10);
   public static final long SPECIFIED_OPACITY           = (1<<11);
   public static final long SPECIFIED_COLOR             = (1<<12);
   public static final long SPECIFIED_FONT_FAMILY       = (1<<13);
   public static final long SPECIFIED_FONT_SIZE         = (1<<14);
   public static final long SPECIFIED_FONT_WEIGHT       = (1<<15);
   public static final long SPECIFIED_FONT_STYLE        = (1<<16);
   public static final long SPECIFIED_TEXT_DECORATION   = (1<<17);
   public static final long SPECIFIED_TEXT_ANCHOR       = (1<<18);
   public static final long SPECIFIED_OVERFLOW          = (1<<19);
   public static final long SPECIFIED_CLIP              = (1<<20);
   public static final long SPECIFIED_MARKER_START      = (1<<21);
   public static final long SPECIFIED_MARKER_MID        = (1<<22);
   public static final long SPECIFIED_MARKER_END        = (1<<23);
   public static final long SPECIFIED_DISPLAY           = (1<<24);
   public static final long SPECIFIED_VISIBILITY        = (1<<25);
   public static final long SPECIFIED_STOP_COLOR        = (1<<26);
   public static final long SPECIFIED_STOP_OPACITY      = (1<<27);
   public static final long SPECIFIED_CLIP_PATH         = (1<<28);
   public static final long SPECIFIED_CLIP_RULE         = (1<<29);
   public static final long SPECIFIED_MASK              = (1<<30);

   public static final long SPECIFIED_ALL = 0xffffffff;

   public static final long SPECIFIED_NON_INHERITING = SPECIFIED_DISPLAY | SPECIFIED_OVERFLOW | SPECIFIED_CLIP
                                                       | SPECIFIED_CLIP_PATH | SPECIFIED_OPACITY | SPECIFIED_STOP_COLOR
                                                       | SPECIFIED_STOP_OPACITY | SPECIFIED_MASK;

   public static class  Style implements Cloneable
   {
      // Which properties have been explicitly specified by this element
      public long       specifiedFlags = 0;

      public SvgPaint   fill;
      public FillRule   fillRule;
      public Float      fillOpacity;

      public SvgPaint   stroke;
      public Float      strokeOpacity;
      public Length     strokeWidth;
      public LineCaps   strokeLineCap;
      public LineJoin   strokeLineJoin;
      public Float      strokeMiterLimit;
      public Length[]   strokeDashArray;
      public Length     strokeDashOffset;

      public Float      opacity; // master opacity of both stroke and fill

      public Colour     color;

      public List<String>    fontFamily;
      public Length          fontSize;
      public Integer         fontWeight;
      public FontStyle       fontStyle;
      public TextDecoration  textDecoration;

      public TextAnchor   textAnchor;

      public Boolean      overflow;  // true if overflow visible
      public CSSClipRect  clip;

      public String     markerStart;
      public String     markerMid;
      public String     markerEnd;
      
      public Boolean    display;    // true if we should display
      public Boolean    visibility; // true if visible

      public SvgPaint   stopColor;
      public Float      stopOpacity;

      public String     clipPath;
      public FillRule   clipRule;

      public String     mask;


      public static final int  FONT_WEIGHT_NORMAL = 400;
      public static final int  FONT_WEIGHT_BOLD = 700;
      public static final int  FONT_WEIGHT_LIGHTER = -1;
      public static final int  FONT_WEIGHT_BOLDER = +1;


      public enum FillRule
      {
         NonZero,
         EvenOdd
      }
      
      public enum LineCaps
      {
         Butt,
         Round,
         Square
      }
      
      public enum LineJoin
      {
         Miter,
         Round,
         Bevel
      }

      public enum FontStyle
      {
         Normal,
         Italic,
         Oblique
      }

      public enum TextAnchor
      {
         Start,
         Middle,
         End
      }
      
      public enum TextDecoration
      {
         None,
         Underline,
         Overline,
         LineThrough,
         Blink
      }
      
      public static Style  getDefaultStyle()
      {
         Style  def = new Style();
         def.specifiedFlags = SPECIFIED_ALL;
         //def.inheritFlags = 0;
         def.fill = Colour.BLACK;
         def.fillRule = FillRule.NonZero;
         def.fillOpacity = 1f;
         def.stroke = null;         // none
         def.strokeOpacity = 1f;
         def.strokeWidth = new Length(1f);
         def.strokeLineCap = LineCaps.Butt;
         def.strokeLineJoin = LineJoin.Miter;
         def.strokeMiterLimit = 4f;
         def.strokeDashArray = null;
         def.strokeDashOffset = new Length(0f);
         def.opacity = 1f;
         def.color = Colour.BLACK; // currentColor defaults to black
         def.fontFamily = null;
         def.fontSize = new Length(12, Unit.pt);
         def.fontWeight = FONT_WEIGHT_NORMAL;
         def.fontStyle = FontStyle.Normal;
         def.textDecoration = TextDecoration.None;
         def.textAnchor = TextAnchor.Start;
         def.overflow = true;  // Overflow shown/visible for root, but not for other elements (see section 14.3.3).
         def.clip = null;
         def.markerStart = null;
         def.markerMid = null;
         def.markerEnd = null;
         def.display = Boolean.TRUE;
         def.visibility = Boolean.TRUE;
         def.stopColor = Colour.BLACK;
         def.stopOpacity = 1f;
         def.clipPath = null;
         def.clipRule = FillRule.NonZero;
         def.mask = null;
         return def;
      }


      // Update the Applied to the current object's style just before we update the current style state.
      // These are the properties that don't inherit.
      public void  resetNonInheritingProperties()
      {
         // If the incoming style doesn't specify one of the non-inheriting properties,
         // make sure we supply a default value instead.
         if (this.display == null)
            this.display = Boolean.TRUE;
         if (this.overflow == null)
            this.overflow = false;
         //if (this.clip == null)
         //   this.clip = null;
         //if (this.clipPath == null)
         //   this.clipPath = null;
         if (this.opacity == null)
            this.opacity = 1f;
         if (this.stopColor == null)
            this.stopColor = Colour.BLACK;
         if (this.stopOpacity == null)
            this.stopOpacity = 1f;
         //if (this.mask == null)
         //   this.mask = null;

         // Set the new styles flags to make sure the state (Paints etc) are correctly updated
         this.specifiedFlags |= SVG.SPECIFIED_NON_INHERITING; 
      }


      @Override
      protected Object  clone()
      {
         Style obj;
         try
         {
            obj = (Style) super.clone();
            if (strokeDashArray != null) {
               obj.strokeDashArray = (Length[]) strokeDashArray.clone();
            }
            return obj;
         }
         catch (CloneNotSupportedException e)
         {
            throw new InternalError(e.toString());
         }
      }
   }


   // What fill or stroke is
   protected abstract static class SvgPaint implements Cloneable
   {
   }

   protected static class Colour extends SvgPaint
   {
      public int colour;
      
      public static final Colour BLACK = new Colour(0);  // Black singleton - a common default value.
      
      public Colour(int val)
      {
         this.colour = val;
      }
      
      public String toString()
      {
         return String.format("#%06x", colour);
      }
   }

   // Special version of Colour that indicates use of 'currentColor' keyword
   protected static class CurrentColor extends SvgPaint
   {
      private static CurrentColor  instance = new CurrentColor();
      
      private CurrentColor()
      {
      }
      
      public static CurrentColor  getInstance()
      {
         return instance;
      }
   }


   protected static class PaintReference extends SvgPaint
   {
      public String    href;
      public SvgPaint  fallback;
      
      public PaintReference(String href, SvgPaint fallback)
      {
         this.href = href;
         this.fallback = fallback;
      }
      
      public String toString()
      {
         return href + " " + fallback;
      }
   }


   protected static class Length implements Cloneable
   {
      float  value = 0;
      Unit   unit = Unit.px;

      public Length(float value, Unit unit)
      {
         this.value = value;
         this.unit = unit;
      }

      public Length(float value)
      {
         this.value = value;
         this.unit = Unit.px;
      }

      public float floatValue()
      {
         return value;
      }

      // Convert length to user units for a horizontally-related context.
      public float floatValueX(SVGAndroidRenderer renderer)
      {
         switch (unit)
         {
            case px:
               return value;
            case em:
               return value * renderer.getCurrentFontSize();
            case ex:
               return value * renderer.getCurrentFontXHeight();
            case in:
               return value * renderer.getDPI();
            case cm:
               return value * renderer.getDPI() / 2.54f;
            case mm:
               return value * renderer.getDPI() / 25.4f;
            case pt: // 1 point = 1/72 in
               return value * renderer.getDPI() / 72f;
            case pc: // 1 pica = 1/6 in
               return value * renderer.getDPI() / 6f;
            case percent:
               Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
               if (viewPortUser == null)
                  return value;  // Undefined in this situation - so just return value to avoid an NPE
               return value * viewPortUser.width / 100f;
            default:
               return value;
         }
      }

      // Convert length to user units for a vertically-related context.
      public float floatValueY(SVGAndroidRenderer renderer)
      {
         if (unit == Unit.percent) {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
               return value;  // Undefined in this situation - so just return value to avoid an NPE
            return value * viewPortUser.height / 100f;
         }
         return floatValueX(renderer);
      }

      // Convert length to user units for a context that is not orientation specific.
      // For example, stroke width.
      public float floatValue(SVGAndroidRenderer renderer)
      {
         if (unit == Unit.percent)
         {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
               return value;  // Undefined in this situation - so just return value to avoid an NPE
            float w = viewPortUser.width;
            float h = viewPortUser.height;
            if (w == h)
               return value * w / 100f;
            float n = (float) (Math.sqrt(w*w+h*h) / SQRT2);  // see spec section 7.10
            return value * n / 100f;
         }
         return floatValueX(renderer);
      }

      // Convert length to user units for a context that is not orientation specific.
      // For percentage values, use the given 'max' parameter to represent the 100% value.
      public float floatValue(SVGAndroidRenderer renderer, float max)
      {
         if (unit == Unit.percent)
         {
            return value * max / 100f;
         }
         return floatValueX(renderer);
      }

      // For situations (like calculating the initial viewport) when we can only rely on
      // physical real world units.
      public float floatValue(float dpi)
      {
         switch (unit)
         {
            case px:
               return value;
            case in:
               return value * dpi;
            case cm:
               return value * dpi / 2.54f;
            case mm:
               return value * dpi / 25.4f;
            case pt: // 1 point = 1/72 in
               return value * dpi / 72f;
            case pc: // 1 pica = 1/6 in
               return value * dpi / 6f;
            case em:
            case ex:
            case percent:
            default:
               return value;
         }
      }

      public boolean isZero()
      {
         return value == 0f;
      }

      public boolean isNegative()
      {
         return value < 0f;
      }

      @Override
      public String toString()
      {
         return String.valueOf(value) + unit;
      }
   }


   protected static class CSSClipRect
   {
      public Length  top;
      public Length  right;
      public Length  bottom;
      public Length  left;
      
      public CSSClipRect(Length top, Length right, Length bottom, Length left)
      {
         this.top = top;
         this.right = right;
         this.bottom = bottom;
         this.left = left;
      }
   }


   //===============================================================================
   // The objects in the SVG object tree
   //===============================================================================


   // Any object that can be part of the tree
   protected static class SvgObject
   {
      public SVG           document;
      public SvgContainer  parent;

      public String  toString()
      {
         return this.getClass().getSimpleName();
         //return super.toString();
      }
   }


   // Any object in the tree that corresponds to an SVG element
   protected static class SvgElementBase extends SvgObject
   {
      public String  id = null;
      public Style   style = new Style();
   }


   // Any object in the tree that corresponds to an SVG element
   protected static class SvgElement extends SvgElementBase
   {
      public Box     boundingBox = null;
   }


   // Any element that can appear inside a <switch> element.
   protected interface SvgConditional
   {
      public void         setRequiredFeatures(Set<String> features);
      public Set<String>  getRequiredFeatures();
      public void         setRequiredExtensions(String extensions);
      public String       getRequiredExtensions();
      public void         setSystemLanguage(Set<String> languages);
      public Set<String>  getSystemLanguage();
   }


   // Any element that can appear inside a <switch> element.
   protected static class  SvgConditionalElement extends SvgElement implements SvgConditional
   {
      public Set<String>  requiredFeatures = null;
      public String       requiredExtensions = null;
      public Set<String>  systemLanguage = null;

      @Override
      public void setRequiredFeatures(Set<String> features) { this.requiredFeatures = features; }
      @Override
      public Set<String> getRequiredFeatures() { return this.requiredFeatures; }
      @Override
      public void setRequiredExtensions(String extensions) { this.requiredExtensions = extensions; }
      @Override
      public String getRequiredExtensions() { return this.requiredExtensions; }
      @Override
      public void setSystemLanguage(Set<String> languages) { this.systemLanguage = languages; }
      @Override
      public Set<String> getSystemLanguage() { return this.systemLanguage; }
   }


   protected interface SvgContainer
   {
      public List<SvgObject>  getChildren();
      public void             addChild(SvgObject elem) throws SAXException;
   }


   protected static class SvgConditionalContainer extends SvgElement implements SvgContainer, SvgConditional
   {
      public List<SvgObject> children = new ArrayList<SvgObject>();

      public Set<String>     requiredFeatures = null;
      public String          requiredExtensions = null;
      public Set<String>     systemLanguage = null;

      @Override
      public List<SvgObject>  getChildren() { return children; }
      @Override
      public void addChild(SvgObject elem) throws SAXException  { children.add(elem); }

      @Override
      public void setRequiredFeatures(Set<String> features) { this.requiredFeatures = features; }
      @Override
      public Set<String> getRequiredFeatures() { return this.requiredFeatures; }
      @Override
      public void setRequiredExtensions(String extensions) { this.requiredExtensions = extensions; }
      @Override
      public String getRequiredExtensions() { return this.requiredExtensions; }
      @Override
      public void setSystemLanguage(Set<String> languages) { this.systemLanguage = languages; }
      @Override
      public Set<String> getSystemLanguage() { return null; }
   }


   protected interface HasTransform
   {
      public void setTransform(Matrix matrix);
   }


   protected static class SvgPreserveAspectRatioContainer extends SvgConditionalContainer
   {
      public AspectRatioAlignment preserveAspectRatioAlignment = null;
      public boolean              preserveAspectRatioSlice;
   }


   protected static class SvgViewBoxContainer extends SvgPreserveAspectRatioContainer
   {
      public Box  viewBox;
   }


   protected static class Svg extends SvgViewBoxContainer
   {
      public Length  x;
      public Length  y;
      public Length  width;
      public Length  height;
   }


   // An SVG element that can contain other elements.
   protected static class Group extends SvgConditionalContainer implements HasTransform
   {
      public Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }


   protected interface NotDirectlyRendered
   {
   }


   // A <defs> object contains objects that are not rendered directly, but are instead
   // referenced from other parts of the file.
   protected static class Defs extends Group implements NotDirectlyRendered
   {
   }


   // One of the element types that can cause graphics to be drawn onto the target canvas.
   // Specifically: ‘circle’, ‘ellipse’, ‘image’, ‘line’, ‘path’, ‘polygon’, ‘polyline’, ‘rect’, ‘text’ and ‘use’.
   protected static abstract class GraphicsElement extends SvgConditionalElement implements HasTransform
   {
      public Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }


   protected static class Use extends Group
   {
      public String  href;
      public Length  x;
      public Length  y;
      public Length  width;
      public Length  height;
   }


   protected static class Path extends GraphicsElement
   {
      public PathDefinition  d;
      public Float           pathLength;
   }


   protected static class Rect extends GraphicsElement
   {
      public Length  x;
      public Length  y;
      public Length  width;
      public Length  height;
      public Length  rx;
      public Length  ry;
   }


   protected static class Circle extends GraphicsElement
   {
      public Length  cx;
      public Length  cy;
      public Length  r;
   }


   protected static class Ellipse extends GraphicsElement
   {
      public Length  cx;
      public Length  cy;
      public Length  rx;
      public Length  ry;
   }


   protected static class Line extends GraphicsElement
   {
      public Length  x1;
      public Length  y1;
      public Length  x2;
      public Length  y2;
   }


   protected static class PolyLine extends GraphicsElement
   {
      public float[]  points;
   }


   protected static class Polygon extends PolyLine
   {
   }


   protected interface  TextChild
   {
      public void  setTextRoot(Text obj);
      public Text  getTextRoot();
   }
   

   protected static class  TextContainer extends SvgConditionalContainer
   {
      @Override
      public void  addChild(SvgObject elem) throws SAXException
      {
         if (elem instanceof TextChild)
            children.add(elem);
         else
            throw new SAXException("Text content elements cannot contain "+elem+" elements.");
      }
   }


   protected static class  TextPositionedContainer extends TextContainer
   {
      public List<Length>  x;
      public List<Length>  y;
      public List<Length>  dx;
      public List<Length>  dy;
   }


   protected static class Text extends TextPositionedContainer implements HasTransform
   {
      public Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }


   protected static class TSpan extends TextPositionedContainer implements TextChild
   {
      private Text  textRoot;

      @Override
      public void  setTextRoot(Text obj) { this.textRoot = obj; }
      @Override
      public Text  getTextRoot() { return this.textRoot; }
   }


   protected static class TextSequence extends SvgObject implements TextChild
   {
      public String  text;

      private Text   textRoot;
      
      public TextSequence(String text)
      {
         this.text = text;
      }
      
      public String  toString()
      {
         return this.getClass().getSimpleName() + " '"+text+"'";
      }

      @Override
      public void  setTextRoot(Text obj) { this.textRoot = obj; }
      @Override
      public Text  getTextRoot() { return this.textRoot; }
   }


   protected static class TRef extends TextContainer implements TextChild
   {
      public String  href;

      private Text   textRoot;

      @Override
      public void  setTextRoot(Text obj) { this.textRoot = obj; }
      @Override
      public Text  getTextRoot() { return this.textRoot; }
   }


   protected static class TextPath extends TextContainer implements TextChild
   {
      public String  href;
      public Length  startOffset;

      private Text  textRoot;

      @Override
      public void  setTextRoot(Text obj) { this.textRoot = obj; }
      @Override
      public Text  getTextRoot() { return this.textRoot; }
   }


   // An SVG element that can contain other elements.
   protected static class Switch extends Group
   {
   }


   protected static class Symbol extends SvgViewBoxContainer implements NotDirectlyRendered
   {
   }


   protected static class Marker extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      public boolean  markerUnitsAreUser;
      public Length   refX;
      public Length   refY;
      public Length   markerWidth;
      public Length   markerHeight;
      public Float    orient;
   }


   protected static class GradientElement extends SvgElementBase implements SvgContainer
   {
      public List<SvgObject> children = new ArrayList<SvgObject>();

      public Boolean         gradientUnitsAreUser;
      public Matrix          gradientTransform;
      public GradientSpread  spreadMethod;
      public String          href;

      @Override
      public List<SvgObject> getChildren()
      {
         return children;
      }

      @Override
      public void addChild(SvgObject elem) throws SAXException
      {
         if (elem instanceof Stop)
            children.add(elem);
         else
            throw new SAXException("Gradient elements cannot contain "+elem+" elements.");
      }
   }


   protected static class Stop extends SvgElementBase implements SvgContainer
   {
      public Float  offset;

      // Dummy container methods. Stop is officially a container, but we 
      // are not interested in any of its possible child elements.
      @Override
      public List<SvgObject> getChildren() { return EMPTY_CHILD_LIST; }
      @Override
      public void addChild(SvgObject elem) throws SAXException { /* do nothing */ }
   }


   protected static class SvgLinearGradient extends GradientElement
   {
      public Length  x1;
      public Length  y1;
      public Length  x2;
      public Length  y2;
   }


   protected static class SvgRadialGradient extends GradientElement
   {
      public Length  cx;
      public Length  cy;
      public Length  r;
      public Length  fx;
      public Length  fy;
   }


   protected static class ClipPath extends Group implements NotDirectlyRendered
   {
      public Boolean  clipPathUnitsAreUser;
   }


   protected static class Pattern extends SvgViewBoxContainer implements NotDirectlyRendered
   {
      public Boolean  patternUnitsAreUser;
      public Boolean  patternContentUnitsAreUser;
      public Matrix   patternTransform;
      public Length   x;
      public Length   y;
      public Length   width;
      public Length   height;
      public String   href;
   }


   protected static class Image extends SvgPreserveAspectRatioContainer implements HasTransform
   {
      public String  href;
      public Length  x;
      public Length  y;
      public Length  width;
      public Length  height;
      public Matrix  transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }


   protected static class View extends SvgViewBoxContainer implements NotDirectlyRendered
   {
   }


   protected static class Mask extends SvgConditionalContainer implements NotDirectlyRendered
   {
      public Boolean  maskUnitsAreUser;
      public Boolean  maskContentUnitsAreUser;
      public Length   x;
      public Length   y;
      public Length   width;
      public Length   height;
   }


   //===============================================================================
   // Protected getters for internal use


   protected void setTitle(String title)
   {
      this.title = title;
   }


   protected void setDesc(String desc)
   {
      this.desc = desc;
   }


   protected void setVersion(String version)
   {
      this.svgVersion = version;
   }


   protected SVGExternalFileResolver  getFileResolver()
   {
      return fileResolver;
   }


   //===============================================================================
   // Path definition


   public interface PathInterface
   {
      public void  moveTo(float x, float y);
      public void  lineTo(float x, float y);
      public void  cubicTo(float x1, float y1, float x2, float y2, float x3, float y3);
      public void  quadTo(float x1, float y1, float x2, float y2);
      public void  arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y);
      public void  close();
   }


   public static class PathDefinition implements PathInterface
   {
      private List<Byte>   commands = null;
      private List<Float>  coords = null;

      private static final byte  MOVETO  = 0;
      private static final byte  LINETO  = 1;
      private static final byte  CUBICTO = 2;
      private static final byte  QUADTO  = 3;
      private static final byte  ARCTO   = 4;   // 4-7
      private static final byte  CLOSE   = 8;


      public PathDefinition()
      {
         this.commands = new ArrayList<Byte>();
         this.coords = new ArrayList<Float>();
      }


      public boolean  isEmpty()
      {
         return commands.isEmpty();
      }


      @Override
      public void  moveTo(float x, float y)
      {
         commands.add(MOVETO);
         coords.add(x);
         coords.add(y);
      }


      @Override
      public void  lineTo(float x, float y)
      {
         commands.add(LINETO);
         coords.add(x);
         coords.add(y);
      }


      @Override
      public void  cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
      {
         commands.add(CUBICTO);
         coords.add(x1);
         coords.add(y1);
         coords.add(x2);
         coords.add(y2);
         coords.add(x3);
         coords.add(y3);
      }


      @Override
      public void  quadTo(float x1, float y1, float x2, float y2)
      {
         commands.add(QUADTO);
         coords.add(x1);
         coords.add(y1);
         coords.add(x2);
         coords.add(y2);
      }


      @Override
      public void  arcTo(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y)
      {
         int  arc = ARCTO | (largeArcFlag?2:0) | (sweepFlag?1:0);
         commands.add((byte) arc);
         coords.add(rx);
         coords.add(ry);
         coords.add(xAxisRotation);
         coords.add(x);
         coords.add(y);
      }


      @Override
      public void  close()
      {
         commands.add(CLOSE);
      }


      public void enumeratePath(PathInterface handler)
      {
         Iterator<Float>  coordsIter = coords.iterator();

         for (byte command: commands)
         {
            switch (command)
            {
               case MOVETO:
                  handler.moveTo(coordsIter.next(), coordsIter.next());
                  break;
               case LINETO:
                  handler.lineTo(coordsIter.next(), coordsIter.next());
                  break;
               case CUBICTO:
                  handler.cubicTo(coordsIter.next(), coordsIter.next(), coordsIter.next(), coordsIter.next(),coordsIter.next(), coordsIter.next());
                  break;
               case QUADTO:
                  handler.quadTo(coordsIter.next(), coordsIter.next(), coordsIter.next(), coordsIter.next());
                  break;
               case CLOSE:
                  handler.close();
                  break;
               default:
                  boolean  largeArcFlag = (command & 2) != 0;
                  boolean  sweepFlag = (command & 1) != 0;
                  handler.arcTo(coordsIter.next(), coordsIter.next(), coordsIter.next(), largeArcFlag, sweepFlag, coordsIter.next(), coordsIter.next());
            }
         }
      }

   }


   //===============================================================================
   // SVG document rendering to a Picture object (indirect rendering)


   public Picture  getPicture()
   {
      // Determine the initial viewport. See SVG spec section 7.2.
      Length  width = rootElement.width;
      if (width != null)
      {
         float w = width.floatValue(DEFAULT_DPI);
         float h;
         Box  rootViewBox = rootElement.viewBox;
         
         if (rootViewBox != null) {
            h = w * rootViewBox.height / rootViewBox.width;
         } else {
            Length  height = rootElement.height;
            if (height != null) {
               h = height.floatValue(DEFAULT_DPI);
            } else {
               h = w;
            }
         }
         return getPicture( (int) Math.ceil(w), (int) Math.ceil(h) );
      }
      else
      {
         return getPicture(DEFAULT_PICTURE_WIDTH, DEFAULT_PICTURE_HEIGHT, DEFAULT_DPI, null, true);
      }
   }


   public Picture  getPicture(int widthInPixels, int heightInPixels)
   {
      return getPicture(widthInPixels, heightInPixels, DEFAULT_DPI, null, true);
   }


   public Picture  getPicture(int widthInPixels, int heightInPixels, float defaultDPI, AspectRatioAlignment alignment, boolean fitToCanvas)
   {
      Picture  picture = new Picture();
      Canvas   canvas = picture.beginRecording(widthInPixels, heightInPixels);
      Box      viewPort = new Box(0f, 0f, (float) widthInPixels, (float) heightInPixels);

      if (alignment == null)
         alignment = AspectRatioAlignment.xMidYMid;

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, viewPort, defaultDPI);

      renderer.renderDocument(this, null, alignment, fitToCanvas, false);

      picture.endRecording();
      return picture;
   }


   public Picture  getPictureForView(String viewId, int widthInPixels, int heightInPixels)
   {
      return getPictureForView(viewId, widthInPixels, heightInPixels, DEFAULT_DPI);
   }


   public Picture  getPictureForView(String viewId, int widthInPixels, int heightInPixels, float defaultDPI)
   {
      SvgObject  obj = this.getElementById(viewId);
      if (obj == null)
         return null;
      if (!(obj instanceof SVG.View))
         return null;

      SVG.View  view = (SVG.View) obj;
      
      if (view.viewBox == null) {
         Log.w(TAG, "View element is missing a viewBox attribute.");
         return null;
      }

      Picture  picture = new Picture();
      Canvas   canvas = picture.beginRecording(widthInPixels, heightInPixels);
      Box      viewPort = new Box(0f, 0f, (float) widthInPixels, (float) heightInPixels);

      AspectRatioAlignment  alignment = view.preserveAspectRatioAlignment;
      if (alignment == null)
         alignment = AspectRatioAlignment.xMidYMid;

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, viewPort, defaultDPI);

      renderer.renderDocument(this, view.viewBox, alignment, !view.preserveAspectRatioSlice, false);

      picture.endRecording();
      return picture;
   }


   //===============================================================================
   // SVG document rendering to a canvas object (direct rendering)


   public void  renderToCanvas(Canvas canvas, RectF dst)
   {
      renderToCanvas(canvas, dst, DEFAULT_DPI, null, true);
   }


   public void  renderToCanvas(Canvas canvas, RectF dst, float defaultDPI, AspectRatioAlignment alignment, boolean fitToCanvas)
   {
      Box  viewPort;

      if (dst != null) {
         viewPort = new Box(dst.left, dst.top, (dst.right - dst.left), (dst.bottom - dst.top));
      } else {
         viewPort = new Box(0f, 0f, (float) canvas.getWidth(), (float) canvas.getHeight());
      }

      if (alignment == null)
         alignment = AspectRatioAlignment.xMidYMid;

      SVGAndroidRenderer  renderer = new SVGAndroidRenderer(canvas, viewPort, defaultDPI);

      renderer.renderDocument(this, null, alignment, fitToCanvas, true);
   }


   //===============================================================================
   // Other document utility API functions


   public SvgObject  resolveIRI(String iri)
   {
      if (iri == null)
         return null;

      if (iri.length() > 1 && iri.startsWith("#"))
      {
         return getElementById(iri.substring(1));
      }
      return null;
   }


   /**
    * Return the contents of the <title> element in the SVG document.
    * @return title contents if declared, otherwise an empty string.
    */
   public String getDocumentTitle()
   {
      return title;
   }


   /**
    * Return the contents of the <desc> element in the SVG document.
    * @return desc contents if declared, otherwise an empty string.
    */
   public String getDocumentDescription()
   {
      return desc;
   }


   /**
    * Return the SVG version number as provided in the root <svg> tag of the document.
    * @return version string if declared, otherwise an empty string.
    */
   public String getDocumentSVGVersion()
   {
      return svgVersion;
   }


   public SvgObject  getElementById(String id)
   {
      if (id.equals(rootElement.id))
         return rootElement;

      // Search the object tree for a node with id property that matches 'id'
      return getElementById(rootElement, id);
   }


   private SvgElementBase  getElementById(SvgContainer obj, String id)
   {
      SvgElementBase  elem = (SvgElementBase) obj;
      if (id.equals(elem.id))
         return elem;
      for (SvgObject child: obj.getChildren())
      {
         if (!(child instanceof SvgElementBase))
            continue;
         SvgElementBase  childElem = (SvgElementBase) child;
         if (id.equals(childElem.id))
            return childElem;
         if (child instanceof SvgContainer)
         {
            SvgElementBase  found = getElementById((SvgContainer) child, id);
            if (found != null)
               return found;
         }
      }
      return null;
   }


   /**
    * Ensure that the root <svg> element has a viewBox.
    * If you want your SVG image to scale properly to your canvas, the renderer
    * needs to know how much to scale it.  It works this out from the viewPort
    * you pass to the renderer combined with the viewBox attribute of the root
    * element.  Some files are missing this viewBox.  When that is the case, the
    * renderer will just draw the image at 1:1 scale.
    * 
    * When called, this method will attempt to generate a suitable viewBox if one
    * is missing.  It does this by using the width and height attributes in the
    * root <svg> element.
    */
   public void  ensureRootViewBox()
   {
      if (rootElement == null)
         return;
      if (rootElement.viewBox != null)
         return;
      if (rootElement.width != null && rootElement.height != null)
      {
         float  w = rootElement.width.floatValue(DEFAULT_DPI);
         float  h = rootElement.height.floatValue(DEFAULT_DPI);
         rootElement.viewBox = new SVG.Box(0,0, w,h);
      }
   }


}
