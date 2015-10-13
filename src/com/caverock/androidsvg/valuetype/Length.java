package com.caverock.androidsvg.valuetype;

import com.caverock.androidsvg.SVGAndroidRenderer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * @hide
 */
public class Length implements Cloneable, Serializable
{
    private static final double  SQRT2 = 1.414213562373095;

    private static final long serialVersionUID = 12202L;

    public float  value = 0;
    public Unit   unit = Unit.px;

    public Length(float value, Unit unit)
    {
        this.value = value;
        this.unit = unit;
    }

    public Length(float value)
    {
        this.value = value;
        this.unit = Unit.px;
    }

    protected Object readResolve() throws ObjectStreamException {
        return this;
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        // write 'this' to 'out'...
        out.defaultWriteObject();

    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // populate the fields of 'this' from the data in 'in'...
        in.defaultReadObject();

    }

    public float floatValue()
    {
        return value;
    }

    // Convert length to user units for a horizontally-related context.
    public float floatValueX(SVGAndroidRenderer renderer)
    {
        switch (unit)
        {
            case px:
                return value;
            case em:
                return value * renderer.getCurrentFontSize();
            case ex:
                return value * renderer.getCurrentFontXHeight();
            case in:
                return value * renderer.getDPI();
            case cm:
                return value * renderer.getDPI() / 2.54f;
            case mm:
                return value * renderer.getDPI() / 25.4f;
            case pt: // 1 point = 1/72 in
                return value * renderer.getDPI() / 72f;
            case pc: // 1 pica = 1/6 in
                return value * renderer.getDPI() / 6f;
            case percent:
                Box viewPortUser = renderer.getCurrentViewPortInUserUnits();
                if (viewPortUser == null)
                    return value;  // Undefined in this situation - so just return value to avoid an NPE
                return value * viewPortUser.width / 100f;
            default:
                return value;
        }
    }

    // Convert length to user units for a vertically-related context.
    public float floatValueY(SVGAndroidRenderer renderer)
    {
        if (unit == Unit.percent) {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
                return value;  // Undefined in this situation - so just return value to avoid an NPE
            return value * viewPortUser.height / 100f;
        }
        return floatValueX(renderer);
    }

    // Convert length to user units for a context that is not orientation specific.
    // For example, stroke width.
    public float floatValue(SVGAndroidRenderer renderer)
    {
        if (unit == Unit.percent)
        {
            Box  viewPortUser = renderer.getCurrentViewPortInUserUnits();
            if (viewPortUser == null)
                return value;  // Undefined in this situation - so just return value to avoid an NPE
            float w = viewPortUser.width;
            float h = viewPortUser.height;
            if (w == h)
                return value * w / 100f;
            float n = (float) (Math.sqrt(w*w+h*h) / SQRT2);  // see spec section 7.10
            return value * n / 100f;
        }
        return floatValueX(renderer);
    }

    // Convert length to user units for a context that is not orientation specific.
    // For percentage values, use the given 'max' parameter to represent the 100% value.
    public float floatValue(SVGAndroidRenderer renderer, float max)
    {
        if (unit == Unit.percent)
        {
            return value * max / 100f;
        }
        return floatValueX(renderer);
    }

    // For situations (like calculating the initial viewport) when we can only rely on
    // physical real world units.
    public float floatValue(float dpi)
    {
        switch (unit)
        {
            case px:
                return value;
            case in:
                return value * dpi;
            case cm:
                return value * dpi / 2.54f;
            case mm:
                return value * dpi / 25.4f;
            case pt: // 1 point = 1/72 in
                return value * dpi / 72f;
            case pc: // 1 pica = 1/6 in
                return value * dpi / 6f;
            case em:
            case ex:
            case percent:
            default:
                return value;
        }
    }

    public boolean isZero()
    {
        return value == 0f;
    }

    public boolean isNegative()
    {
        return value < 0f;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value) + unit;
    }
}
