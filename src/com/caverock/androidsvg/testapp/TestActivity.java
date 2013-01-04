
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

   String[] fileList = {"sample_13.3_pattern01.svg",
                        "test_pattern01.svg",
                        "test_pattern02.svg",
                        "test_pattern03.svg",
                        "test_pattern04.svg",
                        "test_pattern_carto0.svg",
                        "test_pattern_carto1.svg",
                        "test_pattern_carto2.svg",
                        "sample_5.6_Use01.svg",
                        "sample_5.6_Use02.svg",
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
                        "sample_10.5_tspan02.svg",
                        "sample_10.6_tref01.svg",
                        "sample_10.12_textdecoration01.svg",
                        "sample_10.13_toap01.svg",
                        "sample_10.13_toap02.svg",
                        "sample_10.13_toap03.svg",
                        "sample_11.3_fillrule-nonzero.svg",
                        "sample_11.3_fillrule-evenodd.svg",
                        "sample_11.4_linecapb.svg",
                        "sample_11.4_linejoinb.svg",
                        "sample_11.6_marker.svg",
                        "sample_13.2_lingrad01.svg",
                        "sample_13.2_radgrad01.svg",
                        "sample_13.3_pattern01.svg",
                        "sample_14.5_opacity01.svg",
                        "sample_17.2_link01.svg",
                        "test_textanchor.svg",
                        "test_strokedasharray.svg",
                        "test_strokedashoffset.svg",
                        "test_overflow.svg",
                        "test_inherit.svg",
                        "test_requiredFeatures.svg",
                        "test_requiredExtensions.svg",
                        "test_systemLanguage.svg",
                        "test_currentColor.svg",
                        "test_use_svg.svg",
                        "test_miterlimit.svg",
                        "test_style.svg",
                        "test_markers01.svg",
                        "test_markers02.svg",
                        "test_markers03.svg",
                        "test_markers04.svg",
                        "test_markers05.svg",
                        "test_markers06.svg",
                        "test_displaynone.svg",
                        "test_visibilityhidden.svg",
                        "test_gradient01.svg",
                        "test_gradient02.svg",
                        "test_gradient03.svg",
                        "test_gradient04.svg",
                        "test_gradient05.svg",
                        "test_gradient06_badhref.svg",
                        "test_gradient07_href.svg",
                        "test_gradient08_href2.svg",
                        "test_gradient09_misorderedoffsets.svg",
                        "test_gradient10_zerovector.svg",
                        "test_gradient11_onestop.svg",
                        "test_gradient12_zerostops.svg",
                        "test_gradient13_defaults.svg",
                        "test_clip.svg",
                        "test_paths1.svg",
                        "test_paths2.svg",
                        "test_paths3.svg",
                        "test_clipPath01.svg",
                        "test_clipPath02.svg",
                        "!test_clipPath03.svg",
                        "!test_clipPath04.svg",
                        "!test_clipPath05.svg",
                        "!test_clipPath06.svg",
                        "test_clipPath07.svg",
                        "test_clipPath08.svg",
                        "test_paint_fallback.svg",
                        "test_fontfamily.svg",
                        "test_toap_mix.svg",
                        "test_toap_mix_grad.svg",
                        "test_toap_neg_offset.svg",
                        "test_pattern01.svg",
                        "test_pattern02.svg",
                        "test_pattern03.svg",
                        "test_pattern04.svg",
                        "test_pattern_carto0.svg",
                        "test_pattern_carto1.svg",
                        "test_pattern_carto2.svg",
                        "inkscape.svg",
                        "xara.svg",
                        "xara_bluecar.svg",
                        //"Octopus.svg",       // slow!
                        "Octopus_Simple.svg",
                        "butterfly.svg",
                        "lion.svg",
                        "tiger.svg",
                        "eleven_below_single.svg",
                        "Firefox-logo.svg",
                        "chrome-logo.svg",
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
