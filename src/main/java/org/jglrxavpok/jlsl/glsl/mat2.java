package org.jglrxavpok.jlsl.glsl;

public class mat2 {
    private final double[] data;

    public mat2(final vec2 column1, final vec2 column2) {
        data = new double[2 * 2];
        data[0] = column1.x;
        data[1] = column1.y;

        data[2] = column2.x;
        data[3] = column2.y;
    }
}
