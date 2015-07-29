The simplest way to use AndroidSVG with an ImageView is to take advantage of the `renderToPicture()` method and set the `Picture` as a `Drawable`.

```
public class MainActivity extends Activity
{

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      ImageView  imageView = (ImageView) findViewById(R.id.yourImageView);
      imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      try
      {
         SVG svg = SVG.getFromResource(this, R.raw.my_svg_file);
         Drawable drawable = new PictureDrawable(svg.renderToPicture());
         imageView.setImageDrawable(drawable);
      }
      catch(SVGParseException e)
      {}
   }

}
```

Note that the `setLayerType(View.LAYER_TYPE_SOFTWARE)` call is necessary in order to set the View into software rendering mode.  The reason is that AndroidSVG uses features of the `Canvas` API that are not supported by the hardware renderer.

Alternatively, versions of AndroidSVG from 1.2 on include an [implementation of `ImageView` called `SVGImageView`](SVGImageView.md) which handles parsing and displaying SVG documents for you and can be used directly in your layouts.