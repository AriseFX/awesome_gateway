#!/usr/bin/make -f
#
# Makefile for C code
#
# C sources to compile
TARGET_DIR := ../../../target/classes
TARGET := $(TARGET_DIR)/jni/libNativeSupport.so

WORKING_DIR := $(TARGET_DIR)/../jni

JAVA_CLASSES = com.arise.linux.NativeSupport

JNI_STUBS := $(subst .,_,$(JAVA_CLASSES))
JNI_HEADERS := $(patsubst %,$(WORKING_DIR)/%.h,$(JNI_STUBS))
JNI_SOURCES := $(patsubst %,%.c,$(JNI_STUBS))
JNI_JAVA_SOURCES := $(patsubst %,$(TARGET_DIR)/%.class,$(subst .,/,$(JAVA_CLASSES)))

JAVA_BUILD_DIR := $(TARGET_DIR)

JAVA_HOME ?= /home/java/jdk1.8.0_291
JAVA_LIB := $(JAVA_HOME)/jre/lib
JVM_SHARED_LIBS := -L$(JAVA_LIB)/amd64/server -L$(JAVA_LIB)/i386/server  -L$(JAVA_LIB)/amd64/jrockit/ -L$(JAVA_LIB)/i386/jrockit/ -L$(JAVA_LIB)/ppc64le/server -L$(JAVA_LIB)/ppc64le/jrockit/

CXX=gcc
INCLUDES := -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux -I $(WORKING_DIR)

# classpath for javah
ifdef CLASSPATH
JAVAH_CLASSPATH = $(JAVA_BUILD_DIR):$(CLASSPATH)
else
JAVAH_CLASSPATH = $(JAVA_BUILD_DIR)
endif

.PHONY : clean

all: $(TARGET)

$(TARGET): $(JNI_HEADERS) $(JNI_SOURCES)
	$(CXX) -fPIC $(INCLUDES) -g -c $(JNI_SOURCES)
	$(CXX) -shared com_arise_linux_NativeSupport.o -o $(TARGET)
	#$(CXX) -shared $(TARGET_DIR)/libNativeSupport.o -o $(TARGET_DIR)/libNativeSupport.so
	#gcc -fPIC -I /home/java/jdk1.8.0_291/include -I /home/java/jdk1.8.0_291/include/linux -g -c com_arise_linux_NativeSupport.c && gcc -shared com_arise_linux_NativeSupport.o -o com_arise_linux_NativeSupport.so

$(JNI_HEADERS): $(JNI_JAVA_SOURCES)
	mkdir -p $(TARGET_DIR)/jni
	javah -force -classpath $(JAVAH_CLASSPATH) -d $(WORKING_DIR) $(JAVA_CLASSES)

clean:
	rm $(TARGET)
	rm com_arise_linux_NativeSupport.o