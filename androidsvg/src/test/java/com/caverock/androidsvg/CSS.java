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
@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class})
public class CSS
{

   @Test
   public void important() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <rect id=\"one\" width=\"10\" height=\"10\"/>" +
                     "  <rect id=\"two\" width=\"10\" height=\"10\" x=\"10\"/>" +
                     "  <rect id=\"three\" width=\"10\" height=\"10\" x=\"20\"/>" +
                     "  <rect id=\"four\" width=\"10\" height=\"10\" x=\"30\"/>" +
                     "  <style>" +
                     "    rect { fill: #0f0 ! important; }" +
                     "    rect { fill: #f00; }" +
                     "    #four { fill: #f00; }" +
                     "  </style>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String> ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(3, "color"));
      assertEquals("#ff000000", mock.paintProp(6, "color"));
      assertEquals("#ff00ff00", mock.paintProp(9, "color"));
      assertEquals("#ff000000", mock.paintProp(12, "color"));
   }


   @Test
   public void use() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"100\" height=\"100\">" +
                     "  <defs>" +
                     "    <rect id=\"r\" width=\"10\" height=\"10\"/>" +
                     "  </defs>" +
                     "  <style>" +
                     "    use { fill: #0f0; }" +
                     "  </style>" +
                     "  <use href=\"#r\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(newBM);

      RenderOptions renderOptions = RenderOptions.create().css("");
      svg.renderToCanvas(canvas, renderOptions);

      MockCanvas    mock = ((MockCanvas) Shadow.extract(canvas));
      List<String> ops = mock.getOperations();
      //System.out.println(String.join(",", ops));

      assertEquals("#ff00ff00", mock.paintProp(5, "color"));
   }

}
