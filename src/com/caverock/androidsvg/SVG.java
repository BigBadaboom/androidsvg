package com.caverock.androidsvg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;

public class SVG
{
   private static final String  TAG = "AndroidSVG";
   
   private SVG.SvgObject  rootElement = null;

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
      percent,
      none,
      error;

      public static Unit  fromString(String str)
      {
         if (str.equals("%"))
            return percent;
         try
         {
            return valueOf(str);
         } 
         catch (Exception e)
         {
            return error;
         }
      }
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


   public SVG.SvgObject getRootElement()
   {
      return rootElement;
   }


   public void setRootElement(SVG.SvgObject rootElement)
   {
      this.rootElement = rootElement;
   }


   //===============================================================================
   // Object sub-types used in the SVG object tree

   public static class  Box
   {
      public float  minX, minY, width, height;
   }


   public static final long SPECIFIED_FILL            = (1<<0);
   public static final long SPECIFIED_FILL_OPACITY    = (1<<1);
   public static final long SPECIFIED_STROKE          = (1<<2);
   public static final long SPECIFIED_STROKE_OPACITY  = (1<<3);
   public static final long SPECIFIED_STROKE_WIDTH    = (1<<4);
   public static final long SPECIFIED_OPACITY         = (1<<5);
   public static final long SPECIFIED_FONT_FAMILY     = (1<<6);
   public static final long SPECIFIED_FONT_SIZE       = (1<<7);
   public static final long SPECIFIED_FONT_WEIGHT     = (1<<8);
   public static final long SPECIFIED_FONT_STYLE      = (1<<9);
   public static final long SPECIFIED_TEXT_DECORATION = (1<<10);

   public static class  Style
   {
      // Which properties have been explicity specified by this element
      public long     specifiedFlags;

      public boolean  hasFill;
      public SvgPaint fill;
      public Float    fillOpacity;

      public boolean  hasStroke; 
      public SvgPaint stroke;
      public Float    strokeOpacity;
      public Length   strokeWidth;

      public Float    opacity; // master opacity of both stroke and fill

      public String   fontFamily;
      public Length   fontSize;
      public String   fontWeight;
      public String   fontStyle;
      public String   textDecoration;
      
      public Style()
      {
         specifiedFlags = 0;
         hasFill = true;
         fill = new Colour(0);  // black
         fillOpacity = 1f;
         hasStroke = false;
         stroke = null;         // none
         strokeOpacity = 1f;
         strokeWidth = new Length(1f);
         opacity = 1f;
         fontFamily = null;
         fontSize = new Length(12, Unit.pt);
         fontWeight = "normal";
         fontStyle = "normal";
         textDecoration = "none";
      }

      public Style(Style inherit)
      {
         // Not inherited: display, 
         specifiedFlags = 0;
         hasFill = inherit.hasFill;
         fill = inherit.fill;
         fillOpacity = inherit.fillOpacity;
         hasStroke = inherit.hasStroke;
         stroke = inherit.stroke;
         strokeOpacity = inherit.strokeOpacity;
         strokeWidth = inherit.strokeWidth;
         opacity = inherit.opacity;
         fontFamily = inherit.fontFamily;
         fontSize = inherit.fontSize;
         fontWeight = inherit.fontWeight;
         fontStyle = inherit.fontStyle;
         textDecoration = inherit.textDecoration;
      }
   }

   // What fill or stroke is
   protected abstract static class SvgPaint
   {
   }

   protected static class Colour extends SvgPaint
   {
      public int colour;
      
      public Colour(int val)
      {
         this.colour = val;
      }
      
      public String toString()
      {
         return String.format("%02x%02x%02x", (colour>>16)&0xFF, (colour>>8)&0xFF, colour&0xFF);
      }
   }

   protected static class Length
   {
      float  value = 0;;
      Unit   unit = Unit.none;

      public Length(float value, Unit unit)
      {
         this.value = value;
         this.unit = unit;
      }

      public Length(float value)
      {
         this.value = value;
         this.unit = Unit.none;
      }

      public float floatValue()
      {
         return value;
      }

      public float floatValue(int dpi)
      {
         return value;   // FIXME
      }

      public boolean isZero()
      {
         return value == 0f;
      }

      public boolean isNegative()
      {
         return value < 0f;
      }
   }


   //===============================================================================
   // The objects in the SVG object tree

   // Any object that can be part of the tree
   protected static class SvgObject
   {
      public SvgContainer parent;
      
      public String  toString()
      {
         return this.getClass().getSimpleName();
      }
   }

   // Any object in the tree that corresponds to an SVG element
   protected static abstract class SvgElement extends SvgObject
   {
      public String       id;
      public Style        style;
   }

   protected static class SvgContainer extends SvgElement
   {
      public List<SvgObject> children = new ArrayList<SvgObject>();

      public void addChild(SvgObject elem)
      {
         children.add(elem);
      }
   }

   protected static class Svg extends SvgContainer
   {
      public Length width;
      public Length height;
      public Box    viewBox;
   }

   protected interface Transformable
   {
      public void setTransform(Matrix matrix);
   }

   // An SVG element that can contain other elements.
   protected static class Group extends SvgContainer implements Transformable
   {
      public Matrix transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }

   // One of the element types that can cause graphics to be drawn onto the target canvas.
   // Specifically: ‘circle’, ‘ellipse’, ‘image’, ‘line’, ‘path’, ‘polygon’, ‘polyline’, ‘rect’, ‘text’ and ‘use’.
   protected static abstract class GraphicsElement extends SvgElement implements Transformable
   {
      public Matrix transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }

   protected static class Use extends GraphicsElement
   {
      public String href;
   }


   protected static class Path extends GraphicsElement
   {
      public android.graphics.Path  path;
      public Float                  pathLength;
   }


   protected static class Rect extends GraphicsElement
   {
      public Length x;
      public Length y;
      public Length width;
      public Length height;
      public Length rx;
      public Length ry;
   }


   protected static class Circle extends GraphicsElement
   {
      public Length cx;
      public Length cy;
      public Length r;
   }


   protected static class Ellipse extends GraphicsElement
   {
      public Length cx;
      public Length cy;
      public Length rx;
      public Length ry;
   }


   protected static class Line extends GraphicsElement
   {
      public Length x1;
      public Length y1;
      public Length x2;
      public Length y2;
   }


   protected static class PolyLine extends GraphicsElement
   {
      public float[] points;
   }


   protected static class Polygon extends PolyLine
   {
   }


   protected static class TextContainer extends SvgContainer
   {
      public List<Length> x;
      public List<Length> y;
   }


   protected static class Text extends TextContainer implements Transformable
   {
      public Matrix transform;

      @Override
      public void setTransform(Matrix transform) { this.transform = transform; }
   }


   protected static class TSpan extends TextContainer
   {
   }


   protected static class TextSequence extends SvgObject
   {
      String text;
      
      public TextSequence(String text)
      {
         this.text = text;
      }
      
      public String  toString()
      {
         return this.getClass().getSimpleName() + " '"+text.trim()+"'";
      }
   }


   protected static class TRef extends SvgElement
   {
      String href;
   }


   //===============================================================================
   // SVG document rendering

   public Picture  getPicture()
   {
      Picture             picture = new Picture();
      Canvas              canvas = picture.beginRecording(1000, 1000);  // FIXME
      SVGAndroidRenderer  renderer= new SVGAndroidRenderer(canvas);

      renderer.render(rootElement);

      picture.endRecording();
      return picture;
   }



}
