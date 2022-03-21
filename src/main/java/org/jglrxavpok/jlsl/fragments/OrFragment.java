package org.jglrxavpok.jlsl.fragments;

public record OrFragment(String type, boolean isDouble) implements CodeFragment.Data {
    public OrFragment(String type) {
        this(type, true);
    }
}
