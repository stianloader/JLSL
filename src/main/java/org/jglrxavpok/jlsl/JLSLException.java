package org.jglrxavpok.jlsl;


import java.io.Serial;

public class JLSLException
        extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8533858789474579803L;

    public JLSLException(String message) {
        super(message);
    }

    public JLSLException(Exception e) {
        super(e);
    }
}
