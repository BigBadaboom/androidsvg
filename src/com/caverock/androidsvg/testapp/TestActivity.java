
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

   String[] fileList = {//"!acid1_noerrors_nocss.svg",
                        "test_systemLanguage.svg",
                        "sample_5.6_Use01.svg",
                        "sample_5.6_Use03.svg",
                        "sample_6.4.svg",
                        "sample_7.3_InitialCoords.svg",
                        "sample_7.4_NewCoordSys.svg",
                        "sample_7.4_RotateScale.svg",
                        "sample_7.4_Skew.svg",
                        "sample_7.5_Nested.svg",
                        "sample_7.8_PreserveAspectRatio.svg",
                        "!sample_7.10_Units.svg",
                        "sample_8.3_triangle01.svg",
                        "sample_8.3_cubic01b.svg",
                        "sample_8.3_cubic02b.svg",
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
                        "sample_10.4_text01.svg",
                        "sample_10.5_tspan01.svg",
                        "sample_10.12_textdecoration01.svg",
                        "sample_11.3_fillrule-nonzero.svg",
                        "sample_11.3_fillrule-evenodd.svg",
                        "sample_11.4_linecapb.svg",
                        "sample_11.4_linejoinb.svg",
                        "test_textanchor.svg",
                        "test_strokedasharray.svg",
                        "test_strokedashoffset.svg",
                        "test_overflow.svg",
                        "test_inherit.svg",
                        "test_requiredFeatures.svg",
                        "test_requiredExtensions.svg",
                        "test_systemLanguage.svg",
                        "!acid1_noerrors_nocss.svg",
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
               setAsset(fileList[whichFile]);

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
               setAsset(fileList[whichFile]);

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

      setAsset(fileList[whichFile]);
      Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT).show();
   }


   private void setAsset(String filename)
   {
      if (filename.charAt(0) == '!')
      {
         svgView.setRenderDPI(96f);
         svgView.setSVGAsset(filename.substring(1));
      }
      else
      {
         svgView.setRenderDPI(null);
         svgView.setSVGAsset(filename);
      }
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
