package com.caverock.androidsvg;

import android.graphics.Matrix;
import android.graphics.Path;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Paul on 10/07/2017.
 */

@Implements(Path.class)
public class MockPath
{
   private ArrayList<String>  path = new ArrayList<>();
   private ArrayList<Matrix>  transforms = null;


   @Implementation
   public void __constructor__()
   {
      path.clear();
   }

   @Implementation
   public void moveTo(float x, float y)
   {
      path.add(String.format(Locale.US, "M %s %s", num(x), num(y)));
   }

   @Implementation
   public void lineTo(float x, float y)
   {
      path.add(String.format(Locale.US, "L %s %s", num(x), num(y)));
   }

   @Implementation
   public void quadTo(float x1, float y1, float x2, float y2)
   {
      path.add(String.format(Locale.US, "Q %s %s %s %s", num(x1), num(y1), num(x2), num(y2)));
   }

   @Implementation
   public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3)
   {
      path.add(String.format(Locale.US, "C %s %s %s %s %s %s", num(x1), num(y1), num(x2), num(y2), num(x3), num(y3)));
   }

   @Implementation
   public void close()
   {
      path.add("Z");
   }


   @Implementation
   public boolean  op(Path otherPath, Path.Op op)
   {
      MockPath  mockOtherPath = ((MockPath) Shadow.extract(otherPath));
      if (path.isEmpty()) {

         path = new ArrayList(mockOtherPath.path);
         return true;
      }

      // Update the path to represent the Op() operation
      path.add(0, "(");
      switch (op) {
         case UNION:     path.add("\u222a"); break;
         case INTERSECT: path.add("\u2229"); break;
      }
      path.addAll(mockOtherPath.path);
      path.add(")");
      return true;
   }


   @Implementation
   public void  transform(Matrix matrix)
   {
      if (this.transforms == null)
         this.transforms = new ArrayList<Matrix>();
      this.transforms.add(matrix);
   }



   String  getPathDescription()
   {
      StringBuilder  sb = new StringBuilder();
      for (String pathSeg: path) {
         if (sb.length() > 0)
            sb.append(' ');
         sb.append(pathSeg);
      }
      if (transforms != null && !transforms.isEmpty()) {
         for (Matrix matrix: transforms) {
            if (matrix.isIdentity())
               continue;
            sb.append(" \u00d7 [");
            formatMatrix(sb, matrix);
            sb.append(']');
         }
      }
      return sb.toString();
   }

   private void  formatMatrix(StringBuilder sb, Matrix matrix)
   {
      float[]  values = new float[9];
      matrix.getValues(values);
      sb.append(num(values[0]));
      sb.append(", ");
      sb.append(num(values[3]));
      sb.append(", ");
      sb.append(num(values[1]));
      sb.append(", ");
      sb.append(num(values[4]));
      sb.append(", ");
      sb.append(num(values[2]));
      sb.append(", ");
      sb.append(num(values[5]));
   }

   private static String  num(float f)
   {
      if (f == (long) f)
         return String.format("%d", (long) f);
      else
         return String.format("%s", round(f, 5));
   }

   private static float round(float value, int places)
   {
      BigDecimal bd = new BigDecimal(Float.toString(value));
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.floatValue();
   }

}
