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
import android.graphics.Picture;
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
@Config(manifest=Config.NONE, sdk=Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class, MockPicture.class})
public class RenderToPictureTest
{
   @Test
   public void renderToPicture() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Picture picture = svg.renderToPicture();

      List<String>  ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(512, picture.getWidth());
      assertEquals(512, picture.getHeight());
      assertEquals("concat(Matrix(2.56 0 0 2.56 0 128))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint())", ops.get(3));
   }


   @Test
   public void renderToPictureIntrinsic() throws SVGParseException
   {
      // Calc height of picture given only width
      String  test = "<svg width=\"400\" viewBox=\"0 0 200 100\">\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Picture picture = svg.renderToPicture();

      List<String>  ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(400, picture.getWidth());
      assertEquals(200, picture.getHeight());
      assertEquals("concat(Matrix(2 0 0 2 0 0))", ops.get(1));

      // Calc width of picture given only height
      test = "<svg height=\"400\" viewBox=\"0 0 200 100\">\n" +
             "</svg>";
      svg = SVG.getFromString(test);

      picture = svg.renderToPicture();

      ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(800, picture.getWidth());
      assertEquals(400, picture.getHeight());
      assertEquals("concat(Matrix(4 0 0 4 0 0))", ops.get(1));
   }


   @Test
   public void renderToPictureWithDims() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Picture picture = svg.renderToPicture(400,400);

      List<String>  ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(400, picture.getWidth());
      assertEquals(400, picture.getHeight());
      assertEquals("concat(Matrix(2 0 0 2 0 100))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint())", ops.get(3));
   }


   //--------------------------------------------------------------------------


   @Test
   public void renderViewToPicture() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 100 100\">\n" +
                     "  <view id=\"test\" viewBox=\"25 25 50 50\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Picture picture = svg.renderViewToPicture("test",200,300);

      List<String>  ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(4 0 0 4 -100 -50))", ops.get(1));
   }



   //--------------------------------------------------------------------------

   @Test
   public void renderToPictureWithDimsRO() throws SVGParseException
   {
      String  test = "<svg viewBox=\"0 0 200 100\">\n" +
                     "  <rect width=\"200\" height=\"100\" fill=\"green\"/>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      RenderOptions  opts = RenderOptions.create().viewPort(100,100,200,300);
      Picture  picture = svg.renderToPicture(400,400, opts);

      List<String>  ops = ((MockPicture) Shadow.extract(picture)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(400, picture.getWidth());
      assertEquals(400, picture.getHeight());
      assertEquals("concat(Matrix(1 0 0 1 100 200))", ops.get(1));
      assertEquals("drawPath('M 0 0 L 200 0 L 200 100 L 0 100 L 0 0 Z', Paint())", ops.get(3));
   }



   @Test
   public void renderToPictureRO() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      // Step 1

      RenderOptions opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50);

      Picture  picture = svg.renderToPicture(opts);

      MockPicture  mock = ((MockPicture) Shadow.extract(picture));
      List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 2 0 100))", ops.get(1));

      // Step 2

      opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50)
          .preserveAspectRatio(PreserveAspectRatio.of("xMinYMax meet"));

      picture = svg.renderToPicture(opts);

      mock = ((MockPicture) Shadow.extract(picture));
      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 2 0 200))", ops.get(1));

      // Step 3

      opts = RenderOptions.create();
      opts.viewPort(0,0,200,300)
          .viewBox(0,0,100,50)
          .preserveAspectRatio(PreserveAspectRatio.of("none"));

      picture = svg.renderToPicture(opts);

      mock = ((MockPicture) Shadow.extract(picture));
      ops = mock.getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("concat(Matrix(2 0 0 6 0 0))", ops.get(1));
   }


}
