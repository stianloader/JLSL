package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;
import java.util.List;

public record AndFragment(String type, boolean isDouble) implements CodeFragment.Data {
    public AndFragment(String type) {
        this(type, true);
    }
}
