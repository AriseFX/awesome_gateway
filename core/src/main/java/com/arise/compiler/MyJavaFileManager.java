package com.arise.compiler;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;


/**
 * @Author: wy
 * @Date: Created in 10:31 2021/1/11
 * @Description: 包装了一下StandardJavaFileManager，用来获取编译后的字节码
 * @Modified: By：
 */
public class MyJavaFileManager implements JavaFileManager {

    private final StandardJavaFileManager fileManager;

    private final Map<String, ByteArrayOutputStream> byteCodeMap = Collections.synchronizedMap(new LinkedHashMap<>());

    public MyJavaFileManager(StandardJavaFileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return fileManager.getClassLoader(location);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        return fileManager.list(location, packageName, kinds, recurse);
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return fileManager.inferBinaryName(location, file);
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return fileManager.isSameFile(a, b);
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return fileManager.handleOption(current, remaining);
    }

    @Override
    public boolean hasLocation(Location location) {
        return fileManager.hasLocation(location);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
        return fileManager.getJavaFileForInput(location, className, kind);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return new SimpleJavaFileObject(URI.create(className), kind) {
            @Override
            public OutputStream openOutputStream() {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byteCodeMap.put(className, outputStream);
                return outputStream;
            }
        };
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return fileManager.getFileForInput(location, packageName, relativeName);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        return fileManager.getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public void flush() {
        try {
            fileManager.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            fileManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int isSupportedOption(String option) {
        return fileManager.isSupportedOption(option);
    }

    public byte[] getByteCode(String className) {
        ByteArrayOutputStream ops = byteCodeMap.get(className);
        if (ops != null) {
            return ops.toByteArray();
        } else {
            return new byte[0];
        }
    }
}
