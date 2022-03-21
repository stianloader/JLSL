package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.ObfuscationFilter.NonObfuscable;
import static org.jglrxavpok.jlsl.glsl.GLSL.*;


@Extensions({"GL_ARB_explicit_uniform_location", "GL_ARB_arrays_of_arrays"})
public class TestShader
        extends FragmentShaderEnvironment {
    public final double PI = Math.PI;
    private final Vertex vertex = new Vertex();
    private final Vertex vertex1 = new Vertex();
    private final Sampler2D texture = new Sampler2D(9);
    @Uniform
    private final Vec2[] list = new Vec2[70];

    @Uniform
    private final Object[][][] list2 = new Object[70][4][5];

    @Uniform
    private Vec2 screenSize;


    @NonObfuscable
    public void main() {
        Vec4 v = new Vec4(this.gl_FragCoord.x / this.screenSize.x, this.gl_FragCoord.y / this.screenSize.y, this.vertex.test(1.0D), this.vertex1.test(1.0D));
        v = normalizer(v, v.length());
        boolean b1 = true;
        boolean c1 = false;


        Mat2 testMatrix = new Mat2(new Vec2(((int) v.x << 2), v.y), new Vec2(Math.PI, 1.0D));
        Vec2 test = (Vec2) this.list2[0][1][2];
        test = test.normalize();
        this.gl_FragColor = null;


        String testTxt = "Hello";
        boolean a = false;
        boolean c = true;
        boolean b = false;
        vignette();
        main();
    }

    private void vignette() {
        this.gl_FragColor = new Vec4(this.gl_FragCoord.x / this.screenSize.x, this.gl_FragCoord.y / this.screenSize.y, 0.0D, 1.0D);
        this.gl_FragColor.z = 1.0D;

        double distance = this.gl_FragCoord.sub(new Vec4(this.screenSize.div(2.0D), this.gl_FragCoord.z, this.gl_FragCoord.w)).length();
        this.gl_FragColor = texture(this.texture, new Vec2(0.5D, 0.5D));
        this.gl_FragColor = (new Vec4(1.0D, 1.0D, 1.0D, 1.0D)).mul(distance);

        boolean b = false;
        this.gl_FragColor.y = 1.0D;
        this.gl_FragColor.x = 2.0D;
    }

    private Vec4 normalizer(Vec4 v, double l) {
        double x1 = v.x / l;
        double y1 = v.y / l;
        double z1 = v.z / l;
        double w1 = v.w / l;
        return new Vec4(x1, y1, z1, w1);
    }
}
