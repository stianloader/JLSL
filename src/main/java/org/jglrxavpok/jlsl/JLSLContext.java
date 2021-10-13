package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.CodeFragment;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class JLSLContext {

    public static JLSLContext currentInstance;
    private final CodeDecoder decoder;
    private final CodeEncoder encoder;
    private final ArrayList<CodeFilter> filters;
    private Object object;

    public JLSLContext(final CodeDecoder decoder, final CodeEncoder encoder) {
        JLSLContext.currentInstance = this;
        this.filters = new ArrayList<>();
        this.decoder = decoder;
        this.decoder.context = this;
        this.encoder = encoder;
        this.encoder.context = this;
    }

    public void addFilters(final CodeFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
    }

    public void requestAnalysisForEncoder(final Object data) {
        this.object = data;
        final ArrayList<CodeFragment> fragments = new ArrayList<>();
        decoder.handleClass(data, fragments);
        final ArrayList<CodeFragment> finalFragments = new ArrayList<>();
        for (final CodeFragment frag : fragments) {
			if (frag != null) {
				finalFragments.add(filter(frag));
			}
        }
        encoder.onRequestResult(finalFragments);
    }

    private CodeFragment filter(CodeFragment fragment) {
        for (final CodeFilter filter : filters) {
            fragment = filter.filter(fragment);
        }
        return fragment;
    }

    public void execute(final Object data, final PrintWriter out) {
        this.object = data;
        final ArrayList<CodeFragment> fragments = new ArrayList<>();
        decoder.handleClass(data, fragments);
        final ArrayList<CodeFragment> finalFragments = new ArrayList<>();
        for (final CodeFragment frag : fragments) {
			if (frag != null) {
				finalFragments.add(filter(frag));
			}
        }
        encoder.createSourceCode(finalFragments, out);
    }

    public Object getCurrentObject() {
        return object;
    }
}
