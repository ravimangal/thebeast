# Frequently Asked Questions #

## When executing the TheBeast java class instead of calling `thebeast` I get a "Exception in thread "main" java.lang.UnsatisfiedLinkError: no lpsolve55j in java.library.path" message ##

thebeast uses the free ILP solver lp\_solve for MAP inference. This is C-code which java calls via a JNI interface. In order to call lp\_solve two things are needed: the actual C library (lpsolve55 -- with some prefix and file extension depending on OS type) and the JNI implementation (lpsolve55j again with different extension and prefix) that calls the C-library and communicates with java. Both files for different platforms are in `thebeast/bin`.

If you want to use thebeast lpsolve55 must be in your OS library path and lpsolve55j must be in your java.library.path. If the beast is stored the directory  `THEBEAST` than you would need to call
```
export LD_LIBRARY_PATH=$THEBEAST/bin
```
before executing the TheBeast java class. This ensures that lpsolve55 is in the system loader path. To make sure the JVM finds the JNI lib you can add `-Djava.libary.path $THEBEAST/bin` as argument to the java interpreter.