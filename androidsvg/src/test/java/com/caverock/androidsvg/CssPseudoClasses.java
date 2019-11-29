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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
public class CssPseudoClasses
{

   @Test
   public void firstChild() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <style>" +
                     "    rect:first-child { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));

      ops.clear();;

      // ":first-child" by itself should match everything (matches <svg>, which affects all children)
      renderOptions.css(":first-child { fill: #00f; }");
      svg.renderToCanvas(canvas, renderOptions);
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));  // Still green because it is more specific
      assertEquals("#ff0000ff", mock.paintProp(6, "color"));  // Should now be blue
   }


   @Test
   public void lastChild() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <style>" +
                     "    rect:last-child { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
   }


   @Test
   public void root() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <style>" +
                     "    :root rect:last-child { fill: #0f0; }" +
                     "    rect:root { fill: #f00; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
   }


   @Test
   public void firstOfType() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <style>" +
                     "    rect:first-of-type { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));

      ops.clear();;

      test = "<svg width=\"100\" height=\"100\">" +
             "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
             "  <rect width=\"10\" height=\"10\"/>" +
             "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
             "  <style>" +
             "    :first-of-type { fill: #0f0; }" +
             "  </style>" +
             "</svg>";
        svg = SVG.getFromString(test);

      svg.renderToCanvas(canvas, renderOptions);

      mock = ((MockCanvas) Shadow.extract(canvas));
      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      // All the elements will be green because :first-of-type matches the <svg> and all the child elements inherit that green
      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
   }


   @Test
   public void lastOfType() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <style>" +
                     "    rect:last-of-type { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));

      // Test tagless version

      ops.clear();;

      test = "<svg width=\"100\" height=\"100\">" +
             "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
             "  <rect width=\"10\" height=\"10\"/>" +
             "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
             "  <style>" +
             "    svg :last-of-type { fill: #0f0; }" +
             "  </style>" +
             "</svg>";
        svg = SVG.getFromString(test);

      svg.renderToCanvas(canvas, renderOptions);

      mock = ((MockCanvas) Shadow.extract(canvas));
      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      // All the elements will be green because :first-of-type matches the <svg> and all the child elements inherit that green
      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
   }


   @Test
   public void onlyChild() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <g>" +
                     "    <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  </g>" +
                     "  <style>" +
                     "    rect:only-child { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(7, "color"));
   }


   @Test
   public void onlyOfType() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <style>" +
                     "    svg :only-of-type { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
   }


   @Test
   public void empty() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\">" +
                     "    <title>Hello</title>" +
                     "  </rect>" +
                     "  <style>" +
                     "    :empty { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      //assertEquals("#ff000000", mock.paintProp(6, "color"));   TODO uncomment when we support children of graphics elements (eg when we have a proper DOM)
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));   // TODO temporary: remove when above fix happens
   }


   @Test
   public void nthChildOdd() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:nth-child(odd) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void nthChildOddAlt() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:nth-child(2n+1) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void nthChildEven() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:nth-child(even) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void nthChildEvenAlt() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:nth-child(2n) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void nthChild4th() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"50\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"60\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"70\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"80\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"90\"/>" +
                     "  <style>" +
                     "    rect:nth-child(5n-1) { fill: #0f0; }" +   // 4th, 9th, 14th etc
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
      assertEquals("#ff000000", mock.paintProp(18, "color"));
      assertEquals("#ff000000", mock.paintProp(21, "color"));
      assertEquals("#ff000000", mock.paintProp(24, "color"));
      assertEquals("#ff00ff00", mock.paintProp(27, "color"));
      assertEquals("#ff000000", mock.paintProp(30, "color"));
   }


   @Test
   public void nthChild4thAlt() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"50\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"60\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"70\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"80\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"90\"/>" +
                     "  <style>" +
                     "    rect:nth-child(5n+4) { fill: #0f0; }" +   // 4th, 9th, 14th etc
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
      assertEquals("#ff000000", mock.paintProp(18, "color"));
      assertEquals("#ff000000", mock.paintProp(21, "color"));
      assertEquals("#ff000000", mock.paintProp(24, "color"));
      assertEquals("#ff00ff00", mock.paintProp(27, "color"));
      assertEquals("#ff000000", mock.paintProp(30, "color"));
   }


   @Test
   public void nthChildFirst3() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child(-1n+3) { fill: #0f0; }" +   // 1st, 2nd, 3rd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
   }


   @Test
   public void nthChild2nd() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child(2) { fill: #0f0; }" +   // 2nd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
   }


   @Test
   public void nthChild2ndAlt() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child( -2n + 2 ) { fill: #0f0; }" +   // 2nd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
   }


   @Test
   public void unsupported() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:lang(en) { fill: #0f0; }" +
                     "    rect:lang(en, fr) { fill: #0f0; }" +
                     "    rect:hover { fill: #0f0; }" +
                     "    rect:focus { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
   }


   @Test
   public void nthChildMinu4Plus10() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"50\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"60\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"70\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"80\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"90\"/>" +
                     "  <style>" +
                     "    rect:nth-child(-4n+10) { fill: #0f0; }" +   // 2nd, 6th, 10th
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
      assertEquals("#ff000000", mock.paintProp(15, "color"));
      assertEquals("#ff00ff00", mock.paintProp(18, "color"));
      assertEquals("#ff000000", mock.paintProp(21, "color"));
      assertEquals("#ff000000", mock.paintProp(24, "color"));
      assertEquals("#ff000000", mock.paintProp(27, "color"));
      assertEquals("#ff00ff00", mock.paintProp(30, "color"));
   }


   @Test
   public void nthChildAll() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child(1n+0) { fill: #0f0; }" +   // 2nd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
      assertEquals("#ff00ff00", mock.paintProp(15, "color"));
   }


   @Test
   public void nthChildAllAlt1() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child(n+0) { fill: #0f0; }" +   // 2nd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
      assertEquals("#ff00ff00", mock.paintProp(15, "color"));
   }


   @Test
   public void nthChildAllAlt2() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"40\"/>" +
                     "  <style>" +
                     "    rect:nth-child(n) { fill: #0f0; }" +   // 2nd
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
      assertEquals("#ff00ff00", mock.paintProp(15, "color"));
   }


   @Test
   public void nthOfTypeOdd() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    svg :nth-of-type(odd) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void nthOfTypeEven() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    svg :nth-of-type(even) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void nthLastChildOdd() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <circle cx=\"30\" cy=\"10\" r=\"10\"/>" +
                     "  <style>" +
                     "    svg :nth-last-child(odd) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void nthLastChildEven() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <circle cx=\"30\" cy=\"10\" r=\"10\"/>" +
                     "  <style>" +
                     "    svg :nth-last-child(even) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void nthLastOfTypeOdd() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <circle cx=\"30\" cy=\"10\" r=\"10\"/>" +
                     "  <style>" +
                     "    svg :nth-last-of-type(odd) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void nthLastOfTypeEven() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <circle cx=\"30\" cy=\"10\" r=\"10\"/>" +
                     "  <style>" +
                     "    svg :nth-last-of-type(even) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void not() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\" class=\"skip\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:not(.skip) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void not2() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\" class=\"skip\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:not(.skip, :last-of-type) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void not3() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\" class=\"skip\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:not(.skip):not(:last-of-type) { fill: #0f0; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void notNotInNot() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"20\" class=\"skip\"/>" +
                     "  <rect width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect:last-of-type { fill: #0f0; }" +
                     "    rect:not(:not(:last-of-type)) { fill: #f00; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff00ff00", mock.paintProp(12, "color"));
   }


   @Test
   public void idSelect() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect id=\"one\" width=\"10\" height=\"10\"/>" +
                     "  <rect id=\"two\" width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect id=\"three\" width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect id=\"four\" width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    #one { fill: #f00; }" +
                     "    #two { fill: #ff0; }" +
                     "    #three { fill: #0f0; }" +
                     "    #four { fill: #00f; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("svg :not(#three) { display: none; }");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals(12, ops.size());
      assertEquals("#ff00ff00", mock.paintProp(7, "color"));
   }



   @Test
   public void target() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect id=\"one\" width=\"10\" height=\"10\"/>" +
                     "  <rect id=\"two\" width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect id=\"three\" width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect id=\"four\" width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    :target { fill: #0f0; }" +
                     "    circle:target { fill: #f00; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().target("two");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));
      assertEquals("#ff00ff00", mock.paintProp(6, "color"));
      assertEquals("#ff000000", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }



   @Test
   public void idTargetSelect() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect id=\"one\" width=\"10\" height=\"10\"/>" +
                     "  <rect id=\"two\" width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect id=\"three\" width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect id=\"four\" width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    #one { fill: #f00; }" +
                     "    #two { fill: #ff0; }" +
                     "    #three { fill: #0f0; }" +
                     "    #four { fill: #00f; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("svg :not(:target) { display: none; }");

      renderOptions.target("two");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals(12, ops.size());
      assertEquals("#ffffff00", mock.paintProp(5, "color"));

      ops.clear();

      renderOptions.target("four");
      svg.renderToCanvas(canvas, renderOptions);

      mock = ((MockCanvas) Shadow.extract(canvas));
      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals(12, ops.size());
      assertEquals("#ff0000ff", mock.paintProp(9, "color"));
   }



}
