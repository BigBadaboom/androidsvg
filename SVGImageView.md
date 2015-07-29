# How to use SVGImageView #

AndroidSVG releases from version 1.2.0 onwards include a custom View class that allows you to easily include SVG images in your layouts.

See the [javadoc for SVGImageView](https://androidsvg.googlecode.com/hg/doc/reference/com/caverock/androidsvg/SVGImageView.html).

There are two ways to use SVGImageView in your application.

  1. [Manually add it to your layout in code](#Option_1:_Using_SVGImageView_from_your_code.md)
  1. [Use the view in an XML layout file](#Option_2:_Using_SVGImageView_in_your_XML_layouts.md).


---


## Option 1: Using SVGImageView from your code ##

First, make sure you have included AndroidSVG in your project.  For instance, if you are using Eclipse, copy the `androidsvg.jar` file to your `libs` folder.  The Android plugin will do the rest.

If you want to manually add the SVGImageView to your layout, you will need to do something like the following.
```
public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
 
        LinearLayout layout = new LinearLayout(this);
        SVGImageView svgImageView = new SVGImageView(this);
        svgImageView.setImageAsset("my_svg_file.svg");
        layout.addView(svgImageView,
                       new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setContentView(layout);
    }

}
```

This example uses the `setImageAsset()` method to tell SVGImageView to load the SVG from the `assets` folder.  There are also calls to set the SVG from a resource (`setImageResource()`) and a resource URI (`setImageUri()`);


---


## Option 2: Using SVGImageView in your XML layouts ##

Unfortunately (as of ADT 22) you can't just use the standard jar file (as found on the downloads page) if you want to use SVGImageView in your layout files.  The jars don't include the resources that the layout manager requires.  Instead you need to include AndroidSVG in the form of an Android Library Project.  Instructions for this are included below.

For this approach, you will need.

1. A recent version of the ADT plugin installed in Eclipse.  Must be at least version 17.

2. A copy of the AndroidSVG source.  For this you can [clone the repository](http://code.google.com/p/androidsvg/source/checkout) using Mercurial, or you can download a Zip file containing the source on the [Browse Source page](http://code.google.com/p/androidsvg/source/browse/).

### Creating an AndroidSVG library project ###

Assuming you have downloaded the source into a directory called `androidsvg-src`, create a new Android project in Eclipse using the `androidsvg-src` folder as the existing source.  You can do this by going `File->New->Project...` then selecting `Android Project from Existing Code`.

Then, in the project properties for your app, go to the 'Android' section and then under 'Library', add the new AndroidSVG project that you just created.

![http://i.imgur.com/4g8u249.png](http://i.imgur.com/4g8u249.png)

### Adding the custom view to your layout file ###

You will need to add the SVGImageView widget to your layout file.  If you know what you are doing, you can add it manually, but perhaps the simplest way is to start with an ImageView and modify it.

In the Graphical Layout tab, go to the "Images & Media" section and drag an ImageView component to your layout screen.  Position and resize it how you like, then switch to the XML editing tab.

The ImageView you just added, probably looks something like the following:

```
    <ImageView
        android:id="@+id/imageView1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:src="@drawable/ic_launcher" />
```

The next step is to let your layout know about the custom view.  Add a namespace reference for `svgimageview` to the root element of your layout file.  The root element will commonly be a `LinearLayout`, but may be something else.

```
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:svgimageview="http://schemas.android.com/apk/res-auto"
    ... >
```

The schema is necessary because SVGImageView has a custom attribute for specifying the SVG file (see below).

<blockquote>
<b>Aside:</b>
Note the special schema URL in the namespace.  If you were using a custom View from your own project you would use a URL based on your package name.  For example <code>http://schemas.android.com/apk/com.example.customview</code>.  But since the custom View is in an included library you should use the <code>res-auto</code> short-cut. The real schema URL will be automatically inserted when your app is built.<br>
</blockquote>

The final step is to alter the `ImageView` entry you inserted so that it is a valid SVGImageView entry.

  1. Change `ImageView` to `com.caverock.androidsvg.SVGImageView`.
  1. Replace `android:src` with `svgimageview:svg`

Your entry should now look like this.

```
    <com.caverock.androidsvg.SVGImageView
        android:id="@+id/imageView1"
        android:layout_width="100dp"
        android:layout_height="100dp"
        svgimageview:svg="my_svg_file.svg" />
```

The `svgimageview:svg` attribute can be either:

  * a filename in your assets folder, as in the above example
  * a resource reference such as `@drawable/my_svg_file`
  * a resource URI such as `android.resource://com.example.myapplication/raw/my_svg_file`

Now build and run your application to test your changes.  The custom view won't render correctly in the graphical layout editor. All you will see is a grey rectangle with the SVGImageView class name. You need to deploy your app to a device or the emulator to test if it is working properly.