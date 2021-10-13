package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.ObfuscationFilter.NonObfuscable;
import org.jglrxavpok.jlsl.glsl.GLSL.Extensions;
import org.jglrxavpok.jlsl.glsl.GLSL.Uniform;

@Extensions(
        {
                "GL_ARB_explicit_uniform_location", "GL_ARB_arrays_of_arrays"
        })
public class TestShader extends FragmentShaderEnvironment {

    public final double PI = 3.141592653589793;
    private final Vertex vertex = new Vertex();
    private final Vertex vertex1 = new Vertex();
    private final sampler2D texture = new sampler2D(9);
    @Uniform
    private final vec2[] list = new vec2[70];

    @Uniform
    private final Object[][][] list2 = new Object[70][4][5];
    @Uniform
    private vec2 screenSize;

    @Override
    @NonObfuscable
    public void main() {
        vec4 v = new vec4(gl_FragCoord.x / screenSize.x, gl_FragCoord.y / screenSize.y, vertex.test(1), vertex1.test(1));
        v = normalizer(v, v.length());
        final boolean b1 = true;
        final boolean c1 = false;
        if (!b1) {
            v.z = 1.0;
        }
        final mat2 testMatrix = new mat2(new vec2(((int) v.x << 2), v.y), new vec2(PI, 1));
        vec2 test = (vec2) list2[0][1][2];
        test = test.normalize();
        gl_FragColor = null;
        // new Vertex(); //TODO: NEW

        final String testTxt = "Hello";
        final boolean a = false;
        final boolean c = true;
        final boolean b = false;
        vignette();
        main();
    }

    private void vignette() {
        gl_FragColor = new vec4(gl_FragCoord.x / screenSize.x, gl_FragCoord.y / screenSize.y, 0, 1);
        gl_FragColor.z = 1;

        final double distance = gl_FragCoord.sub(new vec4(screenSize.div(2), gl_FragCoord.z, gl_FragCoord.w)).length();
        gl_FragColor = texture(texture, new vec2(0.5, 0.5));
        gl_FragColor = new vec4(1, 1, 1, 1).mul(distance);

        final boolean b = false;
        gl_FragColor.y = 1;
        gl_FragColor.x = 2;
    }

    private vec4 normalizer(final vec4 v, final double l) {
        final double x1 = v.x / l;
        final double y1 = v.y / l;
        final double z1 = v.z / l;
        final double w1 = v.w / l;
        return new vec4(x1, y1, z1, w1);
    }

}
