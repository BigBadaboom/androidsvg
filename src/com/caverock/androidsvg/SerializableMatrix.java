package com.caverock.androidsvg;

import android.graphics.Matrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Created by stulden on 11-03-15.
 */
public class SerializableMatrix extends Matrix implements Serializable {
    private static final long serialVersionUID = 0L;

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        float[] f = new float[9];
        this.getValues(f);
        out.writeObject(f);
    }


    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        float[] f = new float[9];
        f = (float[]) in.readObject();
        this.setValues(f);
    }
}
