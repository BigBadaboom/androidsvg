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
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.JELLY_BEAN, shadows={MockCanvas.class, MockPath.class})
@RunWith(RobolectricTestRunner.class)
public class RenderTest
{

   /*
    * Checks that calling renderToCanvas() does not have any side effects for the Canvas object.
    * See Issue #50. https://github.com/BigBadaboom/androidsvg/issues/50
    */
   @Test
   public void renderToCanvasPreservesState() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 20 20\">" +
                     "  <circle cx=\"10\" cy=\"10\" r=\"10\" transform=\"scale(2)\"/>" +
                     "  <g transform=\"rotate(45)\"></g>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      Matrix  beforeMatrix = canvas.getMatrix();
      Rect    beforeClip = canvas.getClipBounds();
      int     beforeSaves = canvas.getSaveCount();

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      Matrix  afterMatrix = canvas.getMatrix();
      assertEquals(beforeMatrix, afterMatrix);
      assertEquals(true, beforeMatrix.isIdentity());
      assertEquals(true, afterMatrix.isIdentity());

      Rect    afterClip = canvas.getClipBounds();
      assertEquals(beforeClip, afterClip);

      int     afterSaves = canvas.getSaveCount();
      assertEquals(beforeSaves, afterSaves);
   }
}
