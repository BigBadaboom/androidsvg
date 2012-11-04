
package com.caverock.androidsvg.testapp;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TestActivity extends Activity
{
   SVGImageView  svgView = null;


   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      
      svgView = (SVGImageView) findViewById(R.id.svg_view);
   }


   
   @Override
   protected void onResume()
   {
      super.onResume();

      if (svgView == null)
         return;

      svgView.setSVGAsset("sample_6.4.svg");
      //svgView.setSVGAsset("rect01.svg");
      //svgView.setSVGAsset("Android_robot.svg");
   }

}
