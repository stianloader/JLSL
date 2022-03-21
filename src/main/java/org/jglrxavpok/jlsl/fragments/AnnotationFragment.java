package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AnnotationFragment(String name, Map<String, Object> values) implements CodeFragment.Data {
    public AnnotationFragment(String name) {
        this(name, new HashMap<>());
    }
}
