package com.caverock.androidsvg;

import android.graphics.Canvas;

import org.junit.Test;

public class CanvasLegacyTest {

    @Test
    public void testSave() {
        final Canvas canvas = new Canvas();
        CanvasLegacy.save(canvas, CanvasLegacy.MATRIX_SAVE_FLAG);
        canvas.restore();
    }
}