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

import java.util.ArrayList;
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
      this.operations.add(String.format(Locale.US, "new Canvas(%s);", bitmap));
      this.operations.add("new Canvas(" + bitmap +");");
   }

   @Implementation
   public boolean  clipRect(int left, int top, int right, int bottom)
   {
      this.clipRect.set(left, top, right, bottom);
      this.operations.add(String.format(Locale.US, "clipRect(%d, %d, %d, %d);", left, top, right, bottom));
      return right > left && bottom > top;
   }

   @Implementation
   public void  concat(Matrix matrix)
   {
      this.matrix.postConcat(matrix);
      this.operations.add(String.format(Locale.US, "concat(%s);", matrix));
   }

   @Implementation
   public boolean  clipPath(Path path)
   {
      //this.clipPath.op(path, Path.Op.INTERSECT);
      this.clipPath = path;   // Good enough for our testing purposes
      this.operations.add(String.format(Locale.US, "clipPath(%s);", path));
      return this.clipPath.isEmpty();
   }

   @Implementation
   public void  drawBitmap(Bitmap bm, float left, float top, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawBitmap(%s, %f, %f, %s);", bm, left, top, paint));
   }

   @Implementation
   public void  drawColor(int color)
   {
      this.operations.add(String.format(Locale.US, "drawColor(#%06x);", color));
   }

   @Implementation
   public void  drawPath(Path path, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawPath(%s, %s);", path, paint));
   }

   @Implementation
   public void  drawText(String text, float x, float y, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawText(\"%s\", %f, %f, %s);", text, x, y, paint));
   }

   @Implementation
   public void  drawTextOnPath(String text, Path path, float hOffset, float vOffset, Paint paint)
   {
      this.operations.add(String.format(Locale.US, "drawTextOnPath(\"%s\", %s, %f, %f, %s);", text, path, hOffset, vOffset, paint));
   }

   @Implementation
   public int  getHeight()
   {
      this.operations.add("getHeight();");
      return this.bitmap.getHeight();
   }

   @Implementation
   public final Matrix  getMatrix()
   {
      this.operations.add("getMatrix();");
      return new Matrix(this.matrix);
   }

   @Implementation
   public int  getWidth()
   {
      this.operations.add("getWidth();");
      return this.bitmap.getWidth();
   }

   @Implementation
   int getSaveCount()
   {
      //this.operations.add("getSaveCount();");
      return this.matrixStack.size();
   }

   @Implementation
   public void  restore()
   {
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
      this.operations.add("save();");
      return internalSave(ALL_SAVE_FLAG);
   }

   @Implementation
   public int  save(int saveFlags)
   {
      this.operations.add(String.format(Locale.US, "save(%x);", saveFlags));
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
      this.operations.add(String.format(Locale.US, "saveLayerAlpha(%s, %d, %x);", bounds, alpha, saveFlags));
      return internalSave(saveFlags);  // Not accurate, but enough for testing for now.
   }

   @Implementation
   public void  scale(float sx, float sy)
   {
      this.matrix.postScale(sx, sy);
      this.operations.add(String.format(Locale.US, "scale(%f, %f);", sx, sy));
   }

   @Implementation
   public void  setMatrix(Matrix matrix)
   {
      this.matrix = matrix;
      this.operations.add(String.format(Locale.US, "setMatrix(%s);", matrix));
   }

   @Implementation
   public void  translate(float dx, float dy)
   {
      this.matrix.postTranslate(dx, dy);
      this.operations.add(String.format(Locale.US, "translate(%f, %f);", dx, dy));
   }
}

