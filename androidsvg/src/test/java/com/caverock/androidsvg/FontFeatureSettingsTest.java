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
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=Build.VERSION_CODES.O, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
public class FontFeatureSettingsTest
{
   @Test
   public void fontFeatures() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "  <text style=\"font-feature-settings: 'liga' 0, 'clig', 'pnum' on, 'swsh' 42\">Test</text>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bm1);
      svg.renderToCanvas(canvas);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));

      //List<String>  ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("'onum' 0,'subs' 0,'unic' 0,'calt' 1,'dlig' 0,'c2pc' 0,'mkmk' 1,'swsh' 42,'zero' 0,'hlig' 0,'c2sc' 0,'sups' 0,'pcap' 0,'jp78' 0,'pwid' 0,'trad' 0,'ordn' 0,'titl' 0,'fwid' 0,'frac' 0,'locl' 1,'pnum' 1,'smpl' 0,'kern' 1,'tnum' 0,'liga' 0,'lnum' 0,'clig' 1,'jp90' 0,'rlig' 1,'ccmp' 1,'ruby' 0,'jp83' 0,'smcp' 0,'afrc' 0,'jp04' 0,'mark' 1",
                    mock.paintProp(3, "ff"));
   }

   //-----------------------------------------------------------------------------------------------

   @Test
   public void fontStretch() throws SVGParseException
   {
      String  test = "<svg>\n" +
                     "  <text style=\"font-stretch: ultra-condensed\">Test\n" +
                     "  <tspan style=\"font-stretch: normal\">Test</tspan></text>\n" +
                     "  <text style=\"font-stretch: ultra-expanded\">Test</text>\n" +
                     "  <text style=\"font-stretch: 80%\">Test</text>\n" +
                     "  <text style=\"font-stretch: 66\">Test</text>\n" +
                     "  <text style=\"font-stretch: -10%\">Test</text>\n" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap bm1 = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bm1);
      svg.renderToCanvas(canvas);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));

      //List<String>  ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals("'wdth' 50,'wght' 400", mock.paintProp(5, "fv"));
      assertEquals("'wdth' 100,'wght' 400", mock.paintProp(7, "fv"));
      assertEquals("'wdth' 200,'wght' 400", mock.paintProp(11, "fv"));
      assertEquals("'wdth' 80,'wght' 400", mock.paintProp(14, "fv"));
      assertEquals("'wdth' 100,'wght' 400", mock.paintProp(17, "fv"));
      assertEquals("'wdth' 100,'wght' 400", mock.paintProp(20, "fv"));
   }

   //-----------------------------------------------------------------------------------------------


   private static String  sortVariations(String var)
   {
      String[] parts = var.split(",");
      return List.of(parts).stream().sorted().collect(Collectors.joining(","));
   }

}
