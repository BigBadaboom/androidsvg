/*
 * Copyright 2013-2018 Paul LeBeau, Cave Rock Software Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.caverock.androidsvg;

import android.graphics.Canvas;
import android.graphics.Picture;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPicture;

import java.util.List;

/**
 * Mock version of Android Picture class for testing.
 */

@Implements(Picture.class)
public class MockPicture extends ShadowPicture
{
   private int      width = 0;
   private int      height = 0;
   private Canvas   canvas = null;
   private boolean  recording = false;


   @Implementation
   public void  __constructor__()
   {
   }

   public List<String> getOperations()
   {
      return ((MockCanvas) Shadow.extract(canvas)).getOperations();
   }

   public void  clearOperations()
   {
      ((MockCanvas) Shadow.extract(canvas)).clearOperations();
   }


   @Implementation
   public Canvas  beginRecording(int width, int height)
   {
      this.width = width;
      this.height = height;
      this.canvas = new Canvas();
      this.recording = true;
      return this.canvas;
   }


   @Implementation
   public void  endRecording()
   {
      recording = false;
   }


   public int getWidth()
   {
      return this.width;
   }

   public int getHeight()
   {
      return this.height;
   }


   //public static Picture createFromStream(InputStream stream ) { return null; }
   //public void draw(Canvas canvas) { /* do nothing */ }
   //public boolean requiresHardwareAcceleration() { return true; }
   //public void writeToStream(OutputStream stream ) { /* do nothing */ }

}
