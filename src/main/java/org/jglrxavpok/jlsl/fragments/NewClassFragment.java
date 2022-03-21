package org.jglrxavpok.jlsl.fragments;

public record NewClassFragment(
    AccessPolicy access,
    String className,
    String superclass,
    FieldFragment[] fields,
    String[] interfaces,
    AnnotationFragment[] annotations,
    String sourceFile,
    int classVersion
) implements CodeFragment.Data {

}
