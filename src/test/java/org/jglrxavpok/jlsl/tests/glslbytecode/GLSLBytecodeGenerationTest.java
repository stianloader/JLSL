package org.jglrxavpok.jlsl.tests.glslbytecode;

import org.jglrxavpok.jlsl.conversion.glslbytecode.GLSLBytecode;
import org.jglrxavpok.jlsl.tests.glslbytecode.generation.BytecodeGenerationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GLSLBytecodeGenerationTest {

    @Test
    public void bytecodeTests() {
        for (BytecodeGenerationTest test : BytecodeGenerationTest.ALL) {
            String expected = test.expected();
            GLSLBytecode.Root root = test.generate();
            String source = root.generateSource("    ");
            assertEquals(expected, source);
        }
    }
}