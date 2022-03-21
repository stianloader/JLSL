package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EndOfMethodFragment(
        AccessPolicy access,
        String name,
        String owner,
        AnnotationFragment[] annotations,
        String returnType,
        List<String> argumentsTypes,
        List<String> argumentsNames,
        Map<Integer, String> varNameMap,
        Map<Integer, String> varTypeMap,
        Map<String, String> varName2TypeMap
) implements CodeFragment.Data {
    public EndOfMethodFragment(StartOfMethodFragment start) {
        this(
                start.access(),
                start.name(),
                start.owner(),
                start.annotations(),
                start.returnType(),
                new ArrayList<>(start.argumentsTypes()),
                new ArrayList<>(start.argumentsNames()),
                new HashMap<>(start.varNameMap()),
                new HashMap<>(start.varTypeMap()),
                new HashMap<>(start.varName2TypeMap())
        );
    }
}
