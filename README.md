# JLSL

Bored of programming your shaders with GLSL, where you're really prompt to make
tons of mistakes? With JLSL, you're now able to program them directly with Java,
and fix those problems!

## Description:

JLSL is a library that will convert Java code into GLSL source code, that way
one can create shaders without even needing to learn the specificities of the
GLSL language!

## Why does it exist?

This library allows a developer who doesn't know (or want) how to program in GLSL
to still be able to create shaders for his application.

The library was later on forked under the stianloader umbrella in order to be
able to publish downgraded (via jvmDowngrader) java 8 artifacts as well as
being able to resolve the one or the other issue the people that came before us
left unresolved.

## How it works:

Once you give a class to convert to JLSL, the bytecode of that class is
extracted. Then, while reading this bytecode, JLSL will reconstruct GLSL source
code from it, kind of like a decompiler would reconstruct a Java source code.
