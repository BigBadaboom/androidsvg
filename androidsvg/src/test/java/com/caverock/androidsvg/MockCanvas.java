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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

/**
 * Mock version of Android Canvas class for testing.
 */

@Implements(Canvas.class)
public class MockCanvas
{
   private Bitmap  bitmap = null;
   private Rect    clipRect = new Rect();
   private Path    clipPath = new Path();
   private Matrix  matrix = new Matrix();

   private Stack<Path>    clipPathStack = new Stack<>();
   private Stack<Matrix>  matrixStack = new Stack<>();

   private ArrayList<String>  operations = new ArrayList<>();


   /**
    * Save flags
    */
   public static final int MATRIX_SAVE_FLAG = 0x01;
   public static final int CLIP_SAVE_FLAG = 0x02;
   public static final int HAS_ALPHA_LAYER_SAVE_FLAG = 0x04;
   public static final int FULL_COLOR_LAYER_SAVE_FLAG = 0x08;
   public static final int CLIP_TO_LAYER_SAVE_FLAG = 0x10;
   public static final int ALL_SAVE_FLAG = 0x1F;


   public void  __constructor__(Bitmap bitmap)
   {
      this.bitmap = bitmap;
      //this.operations.add(String.format(Locale.US, "new Canvas(%s)", bitmap));
   }

   public  List<String>  getOperations()
   {
      return this.operations;
   }
   public  void          clearOperations()
   {
      this.operations.clear();
   }


   @Implementation
   public boolean  clipRect(int left, int top, int right, int bottom)
   {
      this.clipRect.set(left, top, right, bottom);
      this.operations.add(String.format(Locale.US, "clipRect(%d, %d, %d, %d)", left, top, right, bottom));
      return right > left && bottom > top;
   }

   @Implementation
   public void  concat(Matrix matrix)
   {
      this.matrix.postConcat(matrix);
      float[]  m = new float[9];
      matrix.getValues(m);
      this.operations.add(String.format(Locale.US, "concat(Matrix(%s %s %s %s %s %s))", num(m[0]), num(m[3]), num(m[1]), num(m[4]), num(m[2]), num(m[5])));
   }

   @Implementation
   public boolean  clipPath(Path path)
   {
      //this.clipPath.op(path, Path.Op.INTERSECT);
      this.clipPath = path;   // Good enough for our testing purposes
      this.operations.add(String.format(Locale.US, "clipPath(%s)", ((MockPath) Shadow.extract(path)).getPathDescription()));
      return this.clipPath.isEmpty();
   }

   @Implementation
   public void  drawBitmap(Bitmap bm, float left, float top, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawBitmap(%s, %s, %s, %s)", bm, num(left), num(top), paintToStr(paint)));
   }

   @Implementation
   public void  drawColor(int color)
   {
      this.operations.add(String.format(Locale.US, "drawColor(#%06x)", color));
   }

   @Implementation
   public void  drawPath(Path path, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawPath('%s', %s)", ((MockPath) Shadow.extract(path)).getPathDescription(), paintToStr(paint)));
   }

   @Implementation
   public void  drawText(String text, float x, float y, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawText(\"%s\", %s, %s, %s)", text, num(x), num(y), paint));
   }

   @Implementation
   public void  drawTextOnPath(String text, Path path, float hOffset, float vOffset, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawTextOnPath('%s', '%s', %s, %s, %s)", text, ((MockPath) Shadow.extract(path)).getPathDescription(), num(hOffset), num(vOffset), paintToStr(paint)));
   }

   @Implementation
   public int  getHeight()
   {
      //this.operations.add("getHeight()");
      return this.bitmap.getHeight();
   }

   @Implementation
   public final Matrix  getMatrix()
   {
      //this.operations.add("getMatrix()");
      return new Matrix(this.matrix);
   }

   @Implementation
   public int  getWidth()
   {
      //this.operations.add("getWidth()");
      return this.bitmap.getWidth();
   }

   @Implementation
   int getSaveCount()
   {
      //this.operations.add("getSaveCount()");
      return this.matrixStack.size();
   }

   @Implementation
   public void  restore()
   {
      this.operations.add("restore()");
      if (matrixStack.isEmpty())
         throw new IllegalStateException("Stack underflow");
      Matrix  m = this.matrixStack.pop();
      Path    cp = this.clipPathStack.pop();
      if (m != null)
         this.matrix = m;
      if (cp != null)
         this.clipPath = cp;
   }

   @Implementation
   public int  save()
   {
      this.operations.add("save()");
      return internalSave(ALL_SAVE_FLAG);
   }

   @Implementation
   public int  save(int saveFlags)
   {
      this.operations.add(String.format(Locale.US, "save(%x)", saveFlags));
      return internalSave(saveFlags);
   }

   private int  internalSave(int saveFlags)
   {
      int n = this.matrixStack.size();
      this.matrixStack.push(((saveFlags & MATRIX_SAVE_FLAG) != 0) ? new Matrix(this.matrix) : null);
      this.clipPathStack.push(((saveFlags & CLIP_SAVE_FLAG) != 0) ? new Path(this.clipPath) : null);
      return n;
   }

   @Implementation
   public int  saveLayerAlpha(RectF bounds, int alpha, int saveFlags)
   {
      this.operations.add(String.format(Locale.US, "saveLayerAlpha(%s, %d, %x)", bounds, alpha, saveFlags));
      return internalSave(saveFlags);  // Not accurate, but enough for testing for now.
   }

   @Implementation
   public void  scale(float sx, float sy)
   {
      this.matrix.postScale(sx, sy);
      this.operations.add(String.format(Locale.US, "scale(%s, %s)", num(sx), num(sy)));
   }

   @Implementation
   public void  setMatrix(Matrix matrix)
   {
      this.matrix = matrix;
      float[]  m = new float[9];
      matrix.getValues(m);
      this.operations.add(String.format(Locale.US, "setMatrix(Matrix(%s %s %s %s %s %s))", num(m[0]), num(m[3]), num(m[1]), num(m[4]), num(m[2]), num(m[5])));
   }

   @Implementation
   public void  translate(float dx, float dy)
   {
      this.matrix.postTranslate(dx, dy);
      this.operations.add(String.format(Locale.US, "translate(%s, %s)", num(dx), num(dy)));
   }


   public static  String num(float f)
   {
      if (f == (long) f)
         return String.format("%d", (long) f);
      else
         return String.format("%s", f);
   }

   private static String  paintToStr(Paint paint)
   {
      return "Paint()";
   }
}

