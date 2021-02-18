gcc -fPIC -I /usr/lib/jvm/java-8-openjdk-amd64/include -I /usr/lib/jvm/java-8-openjdk-amd64/include/linux -c com_arise_linux_NativeSupport.c
gcc -shared com_arise_linux_NativeSupport.o -o com_arise_linux_NativeSupport.so


javah -classpath . -jni sched.NativeSupport