gcc -fPIC -I /home/java/jdk1.8.0_291/include -I /home/java/jdk1.8.0_291/include/linux -c com_arise_linux_NativeSupport.c
gcc -shared com_arise_linux_NativeSupport.o -o com_arise_linux_NativeSupport.so


javah -classpath . -jni sched.NativeSupport