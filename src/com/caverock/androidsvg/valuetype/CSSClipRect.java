package com.caverock.androidsvg.valuetype;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Created by wonson on 15年10月12日.
 */
public class CSSClipRect implements Serializable {
    private static final long serialVersionUID = 12202L;
    public Length top;
    public Length right;
    public Length bottom;
    public Length left;

    public CSSClipRect(Length top, Length right, Length bottom, Length left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
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
}
