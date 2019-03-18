# New Entry Point
[CLion](https://www.jetbrains.com/clion/) plugin to execute single file .c/.cpp file quickly.

## Description
If you are from Java world, you might be used to simply creating a new single <code>public static void main</code>
in order to create a new entry point for testing purpose.

With CMake things are more complicating since all files needed to be linked together and duplicated method names
will not be tolerated, therefore<code>CMakeLists.txt</code> needs to be configured
to declare <code>add_executable(Example example.c)</code> to run a new entry point.
It is troublesome when you want to run many of the files independently within the same project.<br>

This plugin supports to insert <code>add_executable()</code> statement into <code>CMakeLists.txt</code>.
It will try to find the nearest <code>CMakeLists.txt</code> on the same folder level with the targeted
source code. If it doesn't exist, the root <code>CMakeLists.txt</code> will be token.

## Usage
Select a C/C++ source and hot key Shift + Alt + E will do the reset for you.

## TODO
if the same executable already exists, create a new executable with a random name so user doesn't 
have to create the executable manually.