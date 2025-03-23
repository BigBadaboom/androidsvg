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

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=Build.VERSION_CODES.O, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
public class FontVariationSettingsTest
{

   @Test
   public void fontVariation() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "  <text style=\"font-variation-settings: 'wght' 100, 'slnt' -14, 'ital' 1 \">Test</text>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bm1);
      svg.renderToCanvas(canvas);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));

      //List<String>  ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(sortVariations("'ital' 1,'slnt' -14,'wght' 100"),
                   sortVariations(mock.paintProp(3, "fv")));
   }

   //-----------------------------------------------------------------------------------------------

   @Test
   public void fontBoldVsWght() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "  <text style=\"font-weight: bold; font-variation-settings: 'wght' 100 \">Test</text>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bm1);
      svg.renderToCanvas(canvas);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));

      //List<String>  ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(sortVariations("'wght' 100"),
                   sortVariations(mock.paintProp(3, "fv")));
   }

   //-----------------------------------------------------------------------------------------------



   private static String  sortVariations(String var)
   {
      String[] parts = var.split(",");
      return List.of(parts).stream().sorted().collect(Collectors.joining(","));
   }

}
