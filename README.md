**DO NOT TOUCH THIS CODEBASE UNLESS YOU ABSOLUTELY KNOW WHAT YOU ARE DOING.**

We forked this branch as we assumed that it would be more up-to-date in
a few ways, but we were instead greeted by an incomplete mess that
wouldn't even compile. Okay - one killing spree of random test classes
later it seems to all work it *seemed* to work alright. Well, that is
until one would actually try to translate very basic shaders. Things
like Mat4 would erroneously be translated to Mat2 (why?), very basic
annotations such as Varying or Attribute were suddenly missing and
constructor calls to GLSL types such as Mat2 were left completely ignored.

As the fork we forked seems to have been created for the sole purpose of
rewriting the entire project, I have decided that further investing in
removing all regressions is a bit pointless if a more-or-less pristine
state of the project already exists and work from that pristine
point instead.

This refractor will thus be eternally banned into the shadow branch.
You can still contribute to the branch if you wish to, but only really do
so if you believe that the refractor made here are actually worth
fixing.

---

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
