
package com.caverock.androidsvg.testapp;


import android.app.Activity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TestActivity extends Activity
{
   SVGImageView  svgView = null;

   GestureDetector  gesture;
   Toast            currentToast;

   String[] fileList = {"sample_8.3_arcs02.svg",
                        "sample_6.4.svg",
                        "sample_7.3_InitialCoords.svg",
                        "sample_7.4_NewCoordSys.svg",
                        "sample_7.4_RotateScale.svg",
                        "sample_7.4_Skew.svg",
                        "sample_7.5_Nested.svg",
                        "sample_8.3_triangle01.svg",
                        "sample_8.3_cubic01.svg",
                        "sample_8.3_cubic02.svg",
                        "sample_8.3_quad01.svg",
                        "sample_8.3_arcs01.svg",
                        "sample_8.3_arcs02.svg",
                        "sample_9.2_rect01.svg",
                        "sample_9.2_rect02.svg",
                        "sample_9.3_circle01.svg",
                        "sample_9.4_ellipse01.svg",
                        "sample_9.5_line01.svg",
                        "sample_9.6_polyline01.svg",
                        "sample_9.7_polygon01.svg",
                        "Android_robot.svg"};
   int  whichFile = 0;


   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      
      svgView = (SVGImageView) findViewById(R.id.svg_view);

      initSwipes();

      if (savedInstanceState != null)
      {
         whichFile = savedInstanceState.getInt("whichFile");
      }
   }



   private void initSwipes()
   {
      GestureDetector.SimpleOnGestureListener   gListener = new GestureDetector.SimpleOnGestureListener() {
         
         @Override
         public boolean  onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
         {
            if (velocityX > 1000f) {
               // Decrement file index
               whichFile = (--whichFile + fileList.length) % fileList.length;

               // Update the view
               svgView.setSVGAsset(fileList[whichFile]);

               // Show the filename as a Toast
               if (currentToast != null)
                  currentToast.cancel();
               currentToast = Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT);
               currentToast.show();
               return true;
            } else if (velocityX < -1000f) {
               // Increment file index
               whichFile = ++whichFile % fileList.length;
               
               // Update the view
               svgView.setSVGAsset(fileList[whichFile]);

               // Show the filename as a Toast
               if (currentToast != null)
                  currentToast.cancel();
               currentToast = Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT);
               currentToast.show();
               return true;
            }
            return false;
         }
      };
      this.gesture = new GestureDetector(this, gListener);
   }


   
   @Override
   protected void onResume()
   {
      super.onResume();

      if (svgView == null)
         return;

      svgView.setSVGAsset(fileList[whichFile]);
      Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT).show();
   }



   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      return gesture.onTouchEvent(event);
   }


   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);
      outState.putInt("whichFile",  whichFile);
   }



}
