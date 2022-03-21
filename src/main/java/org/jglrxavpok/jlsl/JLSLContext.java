package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.CodeFragment;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JLSLContext {
    public static JLSLContext currentInstance;
    private final CodeDecoder decoder;
    private final CodeEncoder encoder;
    private final List<CodeFilter> filters = new ArrayList<>();
    private Object object;

    public JLSLContext(CodeDecoder decoder, CodeEncoder encoder) {
        currentInstance = this;
        this.decoder = decoder;
        this.decoder.context = this;
        this.encoder = encoder;
        this.encoder.context = this;
    }

    public void addFilters(CodeFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
    }

    public void requestAnalysisForEncoder(Object data) {
        this.object = data;
        List<CodeFragment.Data> fragments = new ArrayList<>();
        this.decoder.handleClass(data, fragments);
    }

    private CodeFragment.Data filter(CodeFragment.Data fragment) {
        for (CodeFilter filter : this.filters) {
            fragment = filter.filter(fragment);
        }
        return fragment;
    }

    public void execute(Object data, PrintWriter out) {
        this.object = data;
        List<CodeFragment.Data> fragments = new ArrayList<>();
        this.decoder.handleClass(data, fragments);
        this.encoder.createSourceCode(fragments, out);
    }

    public Object getCurrentObject() {
        return this.object;
    }
}
