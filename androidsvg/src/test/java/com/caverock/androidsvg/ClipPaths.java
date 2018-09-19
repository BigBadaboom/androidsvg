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
import org.robolectric.shadow.api.Shadow;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk = Build.VERSION_CODES.KITKAT, shadows={MockCanvas.class, MockPath.class})
public class ClipPaths
{

   @Test
   public void emptyClipPath() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\">" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath()", ops.get(3));
   }


   @Test
   public void simpleClipPath() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\">" +
                     "      <rect x=\"10\" y=\"10\" width=\"80\" height=\"80\"/>" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath(M 10 10 L 90 10 L 90 90 L 10 90 L 10 10 Z)", ops.get(3));
   }


   @Test
   public void twoClipPath() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\">" +
                     "      <rect x=\"10\" y=\"10\" width=\"40\" height=\"80\"/>" +
                     "      <polygon points=\"50,50, 90,10, 90,90\"/>" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath(( M 10 10 L 50 10 L 50 90 L 10 90 L 10 10 Z \u222a M 50 50 L 90 10 L 90 90 Z ))", ops.get(3));
   }


   @Test
   public void clipPathWithClipPath() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\" clip-path=\"url(#clipclip)\">" +
                     "      <rect x=\"10\" y=\"10\" width=\"80\" height=\"80\"/>" +
                     "    </clipPath>" +
                     "    <clipPath id=\"clipclip\">" +
                     "      <polygon points=\"20,50, 80,20, 80,80\"/>" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath(( M 10 10 L 90 10 L 90 90 L 10 90 L 10 10 Z \u2229 M 20 50 L 80 20 L 80 80 Z ))", ops.get(3));
   }


   @Test
   public void clipPathIncludesClipPath() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\">" +
                     "      <rect x=\"10\" y=\"10\" width=\"80\" height=\"80\" clip-path=\"url(#clipclip)\"/>" +
                     "    </clipPath>" +
                     "    <clipPath id=\"clipclip\">" +
                     "      <polygon points=\"20,50, 80,20, 80,80\"/>" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath(( M 10 10 L 90 10 L 90 90 L 10 90 L 10 10 Z \u2229 M 20 50 L 80 20 L 80 80 Z ))", ops.get(3));
   }


   @Test
   public void clipPathObjectBoundingBox() throws SVGParseException
   {
      //disableLogging();
      String  test = "<svg width=\"500\" height=\"100\">" +
                     "  <defs>" +
                     "    <clipPath id=\"clip\" clipPathUnits=\"objectBoundingBox\" transform=\"translate(4,3)\">" +
                     "      <rect x=\"0.10\" y=\"0.10\" width=\"0.80\" height=\"0.80\"/>" +
                     "    </clipPath>" +
                     "  </defs>" +
                     "  <rect width=\"100\" height=\"100\" fill=\"green\" clip-path=\"url(#clip)\"/>" +
                     "</svg>";
      SVG  svg = SVG.getFromString(test);

      Bitmap newBM = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()),
                                         (int) Math.ceil(svg.getDocumentHeight()),
                                         Bitmap.Config.ARGB_8888);
      Canvas  canvas = new Canvas(newBM);

      //canvas.save(); canvas.scale(2f, 2f); canvas.restore();
      svg.renderToCanvas(canvas);

      List<String> ops = ((MockCanvas) Shadow.extract(canvas)).getOperations();
      //System.out.println(String.join(",", ops));
      assertEquals(7, ops.size());
      assertEquals("clipPath(M 0.1 0.1 L 0.9 0.1 L 0.9 0.9 L 0.1 0.9 L 0.1 0.1 Z \u00d7 [100, 0, 0, 100, 400, 300])", ops.get(3));
   }



}
