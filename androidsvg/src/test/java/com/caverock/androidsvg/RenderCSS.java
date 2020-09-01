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
public class RenderCSS
{

   /*
    * Checks that calling renderToCanvas() does not have any side effects for the Canvas object.
    * See Issue #50. https://github.com/BigBadaboom/androidsvg/issues/50
    */
   @Test
   public void renderWithCSS() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect width=\"10\" height=\"10\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      //List<String>  ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff000000", mock.paintProp(3, "color"));


      // Step 2
      mock.clearOperations();

      renderOptions = RenderOptions.create().css("rect { fill: red }");
      svg.renderToCanvas(canvas, renderOptions);
      //System.out.println(String.join(",", ops));

      // rect should be red now
      assertEquals("#ffff0000", mock.paintProp(3, "color"));


      // Step 3: Make sure temp CSS hasn't stuck around

      mock.clearOperations();

      svg.renderToCanvas(canvas);
      //System.out.println(String.join(",", ops));

      // rect should be black again
      assertEquals("#ff000000", mock.paintProp(3, "color"));
   }
}
