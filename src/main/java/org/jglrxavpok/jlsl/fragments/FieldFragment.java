package org.jglrxavpok.jlsl.fragments;

public record FieldFragment(
    String name,
    String type,
    AccessPolicy access,
    AnnotationFragment[] annotations,
    Object initialValue
) implements CodeFragment.Data {
}
