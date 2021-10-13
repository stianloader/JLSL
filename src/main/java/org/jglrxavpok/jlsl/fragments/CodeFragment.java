package org.jglrxavpok.jlsl.fragments;

import java.util.ArrayList;

public abstract class CodeFragment {

    private final ArrayList<CodeFragment> children = new ArrayList<>();
    public boolean forbiddenToPrint;

    public void addChild(final CodeFragment fragment) {
        children.add(fragment);
    }

    public ArrayList<CodeFragment> getChildren() {
        return children;
    }
}
