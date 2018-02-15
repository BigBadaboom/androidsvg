package com.caverock.androidsvg;

import android.graphics.Path;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Paul on 10/07/2017.
 */

@Implements(Path.class)
class MockPath
{
   private ArrayList<String>  path = new ArrayList<>();

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
      // Does nothing
   }


   String  getPathDescription()
   {
      StringBuilder  sb = new StringBuilder();
      for (String pathSeg: path) {
         if (sb.length() > 0)
            sb.append(' ');
         sb.append(pathSeg);
      }
      return sb.toString();
   }

   private static String  num(float f)
   {
      if (f == (long) f)
         return String.format("%d", (long) f);
      else
         return String.format("%s", f);
   }
}
