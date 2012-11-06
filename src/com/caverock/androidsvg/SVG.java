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
   
   private SVG.SvgElement  rootElement = null;

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


   public SVG.SvgElement getRootElement()
   {
      return rootElement;
   }


   public void setRootElement(SVG.SvgElement rootElement)
   {
      this.rootElement = rootElement;
   }


   //===============================================================================
   // Object sub-types used in the SVG object tree

   public static class  Box
   {
      public float  minX, minY, width, height;
   }

   public static class  Style
   {
      public boolean  hasFill; 
      public SvgPaint fill;
      public float    fillOpacity;

      public boolean  hasStroke; 
      public SvgPaint stroke;
      public float    strokeOpacity;
      public Length   strokeWidth;

      public float    opacity; // master opacity of both stroke and fill
      
      public Style()
      {
         hasFill = true;
         fill = new Colour(0);  // black
         fillOpacity = 1f;
         hasStroke = false;
         stroke = new Colour(0);  // black
         strokeOpacity = 1f;
         strokeWidth = new Length(1f);
         opacity = 1f;
      }

      public Style(Style inherit)
      {
         hasFill = inherit.hasFill;
         fill = inherit.fill;
         fillOpacity = inherit.fillOpacity;
         hasStroke = inherit.hasStroke;
         stroke = inherit.stroke;
         strokeOpacity = inherit.strokeOpacity;
         strokeWidth = new Length(1f);
         opacity = inherit.opacity;
      }
   }

   // What fill or stroke is
   protected static class SvgPaint
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

   protected static abstract class SvgElement
   {
      public String       id;
      public SvgContainer parent;
      public Style        style;
      
      public String  toString()
      {
         return this.getClass().getSimpleName();
      }
   }

   protected static class SvgContainer extends SvgElement
   {
      public String id;
      public List<SvgElement> children;
      
      public void addChild(SvgElement elem)
      {
         if (children == null)
            children = new ArrayList<SvgElement>();
         children.add(elem);
      }
   }

   protected static class Svg extends SvgContainer
   {
      public Length width;
      public Length height;
      public Box    viewBox;
   }

   // An SVG element that can contain other elements.
   protected static class Group extends SvgContainer
   {
   }

   // One of the element types that can cause graphics to be drawn onto the target canvas.
   // Specifically: ‘circle’, ‘ellipse’, ‘image’, ‘line’, ‘path’, ‘polygon’, ‘polyline’, ‘rect’, ‘text’ and ‘use’.
   private static abstract class GraphicsElement extends SvgElement
   {
      public Matrix transform;
   }

   protected static class Use extends GraphicsElement
   {
      public SvgElement href;
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
