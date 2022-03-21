package org.jglrxavpok.jlsl.glsl;

public class Mat2 {

    public Mat2(Vec2 column1, Vec2 column2) {
        double[] data = new double[4];
        data[0] = column1.x;
        data[1] = column1.y;

        data[2] = column2.x;
        data[3] = column2.y;
    }
}
