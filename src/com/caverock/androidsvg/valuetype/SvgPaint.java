package com.caverock.androidsvg.valuetype;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Created by wonson on 15年10月12日.
 */ // What fill or stroke is
public abstract class SvgPaint implements Cloneable, Serializable {
    private static final long serialVersionUID = 12202L;


    public static class Colour extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;

        public int colour;

        public static final Colour BLACK = new Colour(0);  // Black singleton - a common default value.

        public Colour(int val)
        {
            this.colour = val;
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

        public String toString()
        {
            return String.format("#%06x", colour);
        }
    }

    // Special version of Colour that indicates use of 'currentColor' keyword
    public static class CurrentColor extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        private static CurrentColor  instance = new CurrentColor();

        private CurrentColor()
        {
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

        public static CurrentColor  getInstance()
        {
            return instance;
        }
    }

    public static class PaintReference extends SvgPaint implements Serializable
    {
        private static final long serialVersionUID = 12202L;
        public String    href;
        public SvgPaint  fallback;

        public PaintReference(String href, SvgPaint fallback)
        {
            this.href = href;
            this.fallback = fallback;
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

        public String toString()
        {
            return href + " " + fallback;
        }
    }
}
