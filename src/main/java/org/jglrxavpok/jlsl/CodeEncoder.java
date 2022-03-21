package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.CodeFragment;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class CodeEncoder {
    public JLSLContext context;

    public abstract void createSourceCode(List<CodeFragment.Data> paramList, PrintWriter paramPrintWriter);

    public void onRequestResult(List<CodeFragment.Data> fragments) {
    }
}
