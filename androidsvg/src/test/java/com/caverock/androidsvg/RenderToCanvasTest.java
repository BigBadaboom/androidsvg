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
import android.graphics.RectF;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
public class RenderToCanvasTest
{
   @Test
   public void renderToCanvas() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas1 = new Canvas(bm1);
      svg.renderToCanvas(bmcanvas1);

      List<String>  ops = ((MockCanvas) Shadow.extract(bmcanvas1)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(1 0 0 1 0 50))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint(f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT; h:OFF; s:FILL; ts:16; tf:android.graphics.Typeface@0; color:#ff008000))", ops.get(3));
   }


   @Test
   public void renderToCanvasWithViewPort() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas2 = new Canvas(bm2);
      svg.renderToCanvas(bmcanvas2, new RectF(50, 50, 150, 150));

      List<String>  ops = ((MockCanvas) Shadow.extract(bmcanvas2)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(0.5 0 0 0.5 50 75))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint(f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT; h:OFF; s:FILL; ts:16; tf:android.graphics.Typeface@0; color:#ff008000))", ops.get(3));
   }


   //--------------------------------------------------------------------------


   @Test
   public void renderViewToCanvas() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 100 100\">\n" +
                     "  <view id=\"test\" viewBox=\"25 25 50 50\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas1 = new Canvas(bm1);
      svg.renderViewToCanvas("test", bmcanvas1);

      List<String>  ops = ((MockCanvas) Shadow.extract(bmcanvas1)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(4 0 0 4 -100 -100))", ops.get(1));
   }



   @Test
   public void renderViewToCanvasViewPort() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 100 100\">\n" +
                     "  <view id=\"test\" viewBox=\"25 25 50 50\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas1 = new Canvas(bm1);
      svg.renderViewToCanvas("test", bmcanvas1, new RectF(100, 100, 200, 200));

      List<String>  ops = ((MockCanvas) Shadow.extract(bmcanvas1)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 2 50 50))", ops.get(1));
   }





   //--------------------------------------------------------------------------


   @Test
   public void renderToCanvasWithViewPortRO() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm2 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas2 = new Canvas(bm2);

      RenderOptions opts = RenderOptions.create().viewPort(100,100,100,50);
      svg.renderToCanvas(bmcanvas2, opts);

      List<String>  ops = ((MockCanvas) Shadow.extract(bmcanvas2)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(0.5 0 0 0.5 100 100))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint(f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT; h:OFF; s:FILL; ts:16; tf:android.graphics.Typeface@0; color:#ff008000))", ops.get(3));
   }


   @Test
   public void renderToCanvasRO() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm2 = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888);
      Canvas bmcanvas2 = new Canvas(bm2);

      // Step 1

      RenderOptions opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50);

      svg.renderToCanvas(bmcanvas2, opts);

      MockCanvas    mock = Shadow.extract(bmcanvas2);
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 2 0 100))", ops.get(1));

      // Step 2

      mock.clearOperations();

      opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50)
          .preserveAspectRatio(PreserveAspectRatio.of("xMinYMax meet"));

      svg.renderToCanvas(bmcanvas2, opts);

      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 2 0 200))", ops.get(1));

      // Step 3

      mock.clearOperations();

      opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50)
          .preserveAspectRatio(PreserveAspectRatio.of("none"));

      svg.renderToCanvas(bmcanvas2, opts);

      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 6 0 0))", ops.get(1));
   }


}
