package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.CodeFragment;

@FunctionalInterface
public interface CodeFilter {
    CodeFragment.Data filter(CodeFragment.Data paramCodeFragment);
}
