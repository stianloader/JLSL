package org.jglrxavpok.jlsl.glsl;

public class Vertex {

    private final vec3 pos = new vec3(1, 1, 1);

    private final vec2 texCoords = new vec2(0, 0);

    public double test(final double v) {
        // return pos.x += 1; TODO: DUP2_X1
        pos.x += v;
        return pos.x;
    }
}
