package com.caverock.androidsvg.utils;

import android.graphics.Canvas;

import java.lang.reflect.Method;

/**
 * "Aaand it's gone": Canvas#save(int) has been removed from sdk-28,
 * so this helper classes uses reflection to access the API on older devices.
 */
@SuppressWarnings("JavaReflectionMemberAccess")
public class CanvasLegacy {
    public static final int MATRIX_SAVE_FLAG;

    private static final Method SAVE;

    static {
        try {
            MATRIX_SAVE_FLAG = (int) Canvas.class.getField("MATRIX_SAVE_FLAG").get(null);
            SAVE = Canvas.class.getMethod("save", int.class);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    public static void save(Canvas canvas, int saveFlags) {
        try {
            SAVE.invoke(canvas, saveFlags);
        } catch (Throwable e) {
            throw sneakyThrow(e);
        }
    }

    private static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) throw new NullPointerException("t");
        return CanvasLegacy.sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}
