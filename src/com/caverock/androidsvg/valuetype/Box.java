package com.caverock.androidsvg.valuetype;

import android.graphics.RectF;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Created by wonson on 15年10月12日.
 */
public class Box implements Cloneable, Serializable {
    private static final long serialVersionUID = 12202L;
    public float minX, minY, width, height;

    public Box(float minX, float minY, float width, float height) {
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    protected Object readResolve() throws ObjectStreamException {
        return this;
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        // write 'this' to 'out'...
        out.defaultWriteObject();

    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // populate the fields of 'this' from the data in 'in'...
        in.defaultReadObject();

    }

    public static Box fromLimits(float minX, float minY, float maxX, float maxY) {
        return new Box(minX, minY, maxX - minX, maxY - minY);
    }

    public RectF toRectF() {
        return new RectF(minX, minY, maxX(), maxY());
    }

    public float maxX() {
        return minX + width;
    }

    public float maxY() {
        return minY + height;
    }

    public void union(Box other) {
        if (other.minX < minX) minX = other.minX;
        if (other.minY < minY) minY = other.minY;
        if (other.maxX() > maxX()) width = other.maxX() - minX;
        if (other.maxY() > maxY()) height = other.maxY() - minY;
    }

    public String toString() {
        return "[" + minX + " " + minY + " " + width + " " + height + "]";
    }
}
