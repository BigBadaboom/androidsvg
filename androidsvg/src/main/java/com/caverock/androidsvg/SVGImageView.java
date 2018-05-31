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
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public interface SVGTouchListener {
        void onSVGObjectLongPress(Point point);
        void onSVGObjectTouch(List<SVG.SvgObject> objectList, Point touchPoint);
    }

    private class SVGGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            if (svg != null && svgTouchlistener != null) {
                int rawX = (int) e.getX();
                int rawY = (int) e.getY();

                svgTouchlistener.onSVGObjectLongPress(new Point(rawX, rawY));
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(svg != null && svgTouchlistener != null) {
                int rawX = (int) e.getX();
                int rawY = (int) e.getY();

                List<SVG.SvgObject> list = svg.getSVGObjectsByCoordinate(new Point(rawX, rawY));
                svgTouchlistener.onSVGObjectTouch(list, new Point(rawX, rawY));
                return true;
            }
            return false;
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (svg != null) {
            svg.initShapesWithOffset(getWidth());
        }

    }
}
