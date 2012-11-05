
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

   String[] fileList = {"sample_6.4.svg",
                        "sample_9.2_rect01.svg",
                        "sample_9.2_rect02.svg",
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
            if (velocityX > 1000f && whichFile > 0) {
               svgView.setSVGAsset(fileList[--whichFile]);
               Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT).show();
               return true;
            } else if (velocityX < -1000f && whichFile < (fileList.length-1)) {
               svgView.setSVGAsset(fileList[++whichFile]);
               Toast.makeText(getApplicationContext(), fileList[whichFile], Toast.LENGTH_SHORT).show();
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
