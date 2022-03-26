package org.jglrxavpok.jlsl.conversion;

import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;

public class IndentedStringWriter extends StringWriter {

    private int indentation = 0;
    private final String indentStr;
    private boolean writeIndentNext = false;

    public IndentedStringWriter(@NotNull String indentStr) {
        this.indentStr = indentStr;
    }

    protected String indentStr() {
        return indentStr.repeat(Math.max(0, indentation));
    }

    @Override
    public void write(int c) {
        write(String.valueOf((char) c));
    }

    @Override
    public void write(@NotNull String str) {
        if (str.isEmpty()) {
            return;
        }
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\n') {
                super.write('\n');
                writeIndentNext = true;
            } else {
                writeChecked(c);
            }
            i++;
        }
    }

    private void writeChecked(char c) {
        if (writeIndentNext) {
            super.write(indentStr());
            writeIndentNext = false;
        }
        super.write(c);
    }

    @Override
    public void write(char @NotNull [] cbuf) {
        write(new String(cbuf));
    }

    @Override
    public void write(@NotNull String str, int off, int len) {
        write(str.substring(off, off + len));
    }

    @Override
    public void write(char @NotNull [] cbuf, int off, int len) {
        write(new String(cbuf, off, len));
    }

    public void indent(int amount) {
        indentation = Math.max(indentation + amount, 0);
    }

    public void indent() {
        indent(1);
    }

    public void unindent(int amount) {
        indent(-amount);
    }

    public void unindent() {
        unindent(1);
    }

    public void newLine() {
        write("\n");
    }
}
