package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.CodeFragment;

import java.util.List;

public abstract class CodeDecoder {

    public JLSLContext context;

    public abstract void handleClass(Object data, List<CodeFragment> out);
}
