/*
   Copyright 2017 Paul LeBeau, Cave Rock Software Ltd.

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
import android.graphics.Path;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
@RunWith(RobolectricTestRunner.class)
public class ParseTest
{
   @Test
   public void emptySVG() throws SVGParseException
   {
      // XmlPullParser
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);
      assertNotNull(svg.getRootElement());
   }

   @Test
   public void emptySVGEntitiesEnabled() throws SVGParseException
   {
      // NOTE: Is *really* slow when running under JUnit (15-20secs).
      // However, the speed seems to be okay under normal usage (a real app).
      String test = "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\" [" +
             "  <!ENTITY hello \"Hello World!\">" +
             "]>" +
             "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
             "</svg>";
      SVG svg = SVG.getFromString(test);
      assertNotNull(svg.getRootElement());
   }

   @Test
   public void emptySVGEntitiesDisabled() throws SVGParseException
   {
      String test = "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\" \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\" [" +
             "  <!ENTITY hello \"Hello World!\">" +
             "]>" +
             "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
             "</svg>";
      SVG.setInternalEntitiesEnabled(false);
      SVG svg = SVG.getFromString(test);
      assertNotNull(svg.getRootElement());
   }

   @Test (expected = SVGParseException.class)
   public void unbalancedClose() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "</svg>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);
   }


   @Test
   public void parsePath()
   {
      String  test = "M100,200 C100,100 250,100 250,200 S400,300 400,200";
      Path  path = SVG.parsePath(test);
      assertEquals("M 100 200 C 100 100 250 100 250 200 C 250 300 400 300 400 200", ((MockPath) Shadow.extract(path)).getPathDescription());

      // The arcs in a path get converted to cubic beziers
      test = "M-100 0 A 100 100 0 0 0 0,100";
      path = SVG.parsePath(test);
      assertEquals("M -100 0 C -100 55.22848 -55.22848 100 0 100", ((MockPath) Shadow.extract(path)).getPathDescription());

      // Path with errors
      test = "M 0 0 L 100 100 C 200 200 Z";
      path = SVG.parsePath(test);
      assertEquals("M 0 0 L 100 100", ((MockPath) Shadow.extract(path)).getPathDescription());
   }


/*
   @Test
   public void issue177() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "  <defs></defs>" +
                     "  <g></g>" +
                     "  <a></a>" +
                     "  <use></use>" +
                     "  <image></image>" +
                     "  <text>" +
                     "    <tspan></tspan>" +
                     "    <textPath></textPath>" +
                     "  </text>" +
                     "  <switch></switch>" +
                     "  <symbol></symbol>" +
                     "  <marker></marker>" +
                     "  <linearGradient>" +
                     "    <stop></stop>" +
                     "  </linearGradient>" +
                     "  <radialGradient></radialGradient>" +
                     "  <clipPath></clipPath>" +
                     "  <pattern></pattern>" +
                     "  <view></view>" +
                     "  <mask></mask>" +
                     "  <solidColor></solidColor>" +
                     "  <g>" +
                     "    <path>" +
                     "      <style media=\"print\">" +
                     "      </style>" +
                     "    </path>" +
                     "  </g>" +
                     "</svg>";

      try {
         SVG  svg = SVG.getFromString(test);
         fail("Should have thrown ParseException");
      } catch (SVGParseException e) {
         // passed!
      }
   }
*/


   /*
    * Checks that A elements are parsed and rendered correctly.
    * @throws SVGParseException
    */
   @Test
   public void parseA() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "<a>" +
                     "  <rect width=\"10\" height=\"10\" fill=\"red\"/>" +
                     "</a>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas = new Canvas(bm);

      // Test that A element has been inserted in the DOM tree correctly
      RenderOptions opts = RenderOptions.create();
      opts.css("a rect { fill: green; }");

      svg.renderToCanvas(bmcanvas, opts);

      MockCanvas    mock = Shadow.extract(bmcanvas);
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("drawPath('M 0 0 L 10 0 L 10 10 L 0 10 L 0 0 Z', Paint(f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT h:OFF s:FILL ts:16 tf:android.graphics.Typeface@0 color:#ff008000))", ops.get(4));


      // Test that A elements are being visited properly while rendering
      test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
             "<a fill=\"green\">" +
             "  <rect width=\"10\" height=\"10\"/>" +
             "</a>" +
             "</svg>";
      svg = SVG.getFromString(test);

      mock.clearOperations();
      svg.renderToCanvas(bmcanvas);

      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("drawPath('M 0 0 L 10 0 L 10 10 L 0 10 L 0 0 Z', Paint(f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT h:OFF s:FILL ts:16 tf:android.graphics.Typeface@0 color:#ff008000))", ops.get(4));
   }


   /**
    * Issue 186
    * CSS properties without a value are badly parsed.
    */
   @Test
   public void issue186() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "<text style=\"text-decoration:;fill:green\">Test</text>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas = new Canvas(bm);

      svg.renderToCanvas(bmcanvas);

      MockCanvas    mock = Shadow.extract(bmcanvas);
      //List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("#ff008000", mock.paintProp(3, "color"));
   }


   @Test
   public void parseStyleLeadingColon() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
                     "<text style=\"fill:green;:fill:red\">Test</text>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas = new Canvas(bm);

      svg.renderToCanvas(bmcanvas);

      MockCanvas    mock = Shadow.extract(bmcanvas);
      //List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("#ff008000", mock.paintProp(3, "color"));
   }


   /**
    * Issue 199
    * Semi-thread safe parsing properties (enableInternalEntities and externalFileResolver)
    */
   @Test
   public void issue199() throws SVGParseException
   {
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";

      SVG  svg = SVG.getFromString(test);
      assertTrue(svg.isInternalEntitiesEnabled());
      Assert.assertNull(svg.getExternalFileResolver());

      SVG.setInternalEntitiesEnabled(false);
      SVGExternalFileResolver  resolver = new SimpleAssetResolver(null);
      SVG.registerExternalFileResolver(resolver);

      SVG  svg2 = SVG.getFromString(test);
      Assert.assertFalse(svg2.isInternalEntitiesEnabled());
      Assert.assertEquals(resolver, svg2.getExternalFileResolver());

      // Ensure settings for "svg" haven't changed
      assertTrue(svg.isInternalEntitiesEnabled());
      Assert.assertNull(svg.getExternalFileResolver());
   }

}
