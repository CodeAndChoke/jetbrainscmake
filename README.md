# New Entry Point
[CLion](https://www.jetbrains.com/clion/) plugin to execute single file .c/.cpp file quickly.

## Description
<code>Shift + Alt + E</code> on a source file to create an executable with the corresponding file.

If you are from the Java world, you might be used to be able to create a new single
<code>public static void main(String [] args)</code> in every class in order test some thing
really quick.

With CMake things are more complicated since all files needed to be linked together in an
<code>executable</code> and duplicated method names will not be tolerated.

This plugin will automatically insert a new <code>executable</code> for a single source file in the nearest CMake file.

Simply choose a C/C++ source file you want to execute separately from your main project and press the hot key
<code>Shift + Alt + E</code>