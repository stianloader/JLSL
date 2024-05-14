package org.jglrxavpok.jlsl.glsl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class GLSL {

	/**
	 * Defines the input attributes of a vertex.
	 *
	 * @deprecated In GLSL 1.3 (OpenGL 3.0), this type qualifier was deprecated
	 * and removed in GLSL 1.4 (OpenGL 3.1). In vertex shaders, {@link In}
	 * is to be used instead. {@link Attribute} cannot be used in any other shader stage.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Deprecated
	public @interface Attribute {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Extensions {
		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Flat {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface In {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Layout {
		int location();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Out {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Substitute {
		boolean actsAsField() default false;

		boolean ownerBefore() default false;

		int ownerPosition() default 0;

		boolean usesParenthesis() default true;

		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface SwizzlingMethod // TODO
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Uniform {
	}

	/**
	 * Defines that the input/output data is interpolated between the vertex
	 * and fragment shader.
	 *
	 * @deprecated In GLSL 1.3 (OpenGL 3.0), this type qualifier was deprecated
	 * and removed in GLSL 1.4 (OpenGL 3.1). In vertex shaders, {@link Out}
	 * is to be used instead. In fragment shaders, {@link In} is to be used instead.
	 * {@link Varying} cannot be used in any other shader stage.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Deprecated
	public @interface Varying {
	}
}
