package com.caverock.androidsvg;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
//@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.KITKAT, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
@Config(manifest=Config.NONE, shadows={MockCanvas.class, MockPath.class, MockPaint.class})
public class ArcToTest
{
   @Test
   public void  testIssue155() throws SVGParseException
   {
      String  test = "<svg>" +
                     "  <path d=\"M 163.637 412.021 a 646225.813 646225.813 0 0 1 -36.313 162\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(newBM);

      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(6, ops.size());
      assertEquals("drawPath('M 163.63701 412.02103 C 151.5 466.03125 139.375 520.0156 127.32401 574.021', Paint(color:#ff000000; f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT; h:OFF; s:FILL; tf:android.graphics.Typeface@0; ts:16))", ops.get(3));
   }


   @Test
   public void  testIssue156() throws SVGParseException
   {
      String  test = "<svg>" +
                     "  <path d=\"M 422.776 332.659 a 539896.23 539896.23 0 0 0-22.855-26.558\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(newBM);

      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(6, ops.size());
      assertEquals("drawPath('M 422.77603 332.65903 C 415.15625 323.8125 407.53125 314.96875 399.92102 306.101', Paint(color:#ff000000; f:ANTI_ALIAS|LINEAR_TEXT|SUBPIXEL_TEXT; h:OFF; s:FILL; tf:android.graphics.Typeface@0; ts:16))", ops.get(3));

   }
}
