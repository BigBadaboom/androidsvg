package com.caverock.androidsvg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

public class SVGImageView extends SVGBaseImageView implements View.OnTouchListener {

    private SVG svg;
    private GestureDetector gestureDetector;
    private SVGTouchListener svgTouchlistener;

    private float scaleY;
    private float scaleX;

    public SVGImageView(Context context) {
        super(context);
    }

    public SVGImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SVGImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setSVG(SVG svg) {
        if (svg == null) {
            throw new IllegalArgumentException("SVG can not be null");
        }

        this.svg = svg;

        Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()), (int) Math.ceil(svg.getDocumentHeight()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        svg.renderToCanvas(canvas);
        setImageBitmap(bitmap);
    }

    public void setSVGTouchListener(SVGTouchListener listener) {
        svgTouchlistener = listener;
        gestureDetector = new GestureDetector(getContext(), new SVGGestureListener());
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        initScaleRatios();
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void initScaleRatios() {
        if (svg != null) {
            scaleY = svg.getDocumentHeight() / getHeight();
            scaleX = svg.getDocumentWidth() / getWidth();
        }
    }

    public interface SVGTouchListener {
        void onSVGObjectTouch(List<SVG.SvgObject> objectList);
    }

    private class SVGGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if(svg != null && svgTouchlistener != null) {
                int xPos = (int) (e.getX() * scaleX);
                int yPos = (int) (e.getY() * scaleY);
                List<SVG.SvgObject> list = svg.getSVGObjectsByCoordinate(new Point(xPos, yPos));
                svgTouchlistener.onSVGObjectTouch(list);
                return true;
            }
            return false;
        }
    }
}
