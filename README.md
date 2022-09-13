# AndroidSVG

AndroidSVG is a SVG parser and renderer for Android.  It has almost complete support for the static
visual elements of the SVG 1.1 and SVG 1.2 Tiny specifications (except for filters).

*AndroidSVG is licensed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)*.

[More information, including downloads and documentation, is available at the main AndroidSVG site.](http://bigbadaboom.github.io/androidsvg/)


### Find a bug?

Please file a [bug report](https://github.com/BigBadaboom/androidsvg/issues) and include as much detail as you can.
If possible, please include a sample SVG file showing the error.

If you wish to contact the author with feedback on this project, you can email me at
[androidsvgfeedback@gmail.com](mailto:androidsvgfeedback@gmail.com).


#### How to use?

- get in path 

```java
   try {

			File startDir = new File(input);
			FileInputStream fileInputStream = new FileInputStream(startDir);

			SVG svg = SVG.getFromInputStream(fileInputStream);
			Drawable drawable = new PictureDrawable(svg.renderToPicture());
			imageview.setImageDrawable(drawable);

		} catch (IOException d) {
			d.printStackTrace();

		} catch (SVGParseException s) {
			s.printStackTrace();

		} catch (Exception exception) {

			exception.printStackTrace();
		}

```

- get in Asster
 
 ```java 
    
    try {

			SVG ss = SVG.getFromAsset(context.getAssets(), input);
			Drawable d = new PictureDrawable(ss.renderToPicture());
			imageview.setImageDrawable(d);

		} catch (Exception exn) {

			exn.printStackTrace();

		}
 
 ```
- get in drawable
 
 
 ```java 
     
     try{
			
			SVG svg = SVG.getFromResource(context.getResources(), R.drawable.mysvg);
			Drawable dr = new PictureDrawable(svg.renderToPicture());
			imageview.setImageDrawable(dr);
			
		}catch(Exception exception){
			
		}
 ```

### Using AndroidSVG in your app?

If you have found AndroidSVG useful and are using it in your project, please let me know. I'd love to hear about it!
