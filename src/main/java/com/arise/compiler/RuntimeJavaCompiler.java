package com.arise.compiler;


import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;


/**
 * @Author: wy
 * @Date: Created in 18:24 2021/1/8
 * @Description: 运行时编译 //TODO 增加@Namespace注解，完整的实现类隔离
 * @Modified: By：
 */
public class RuntimeJavaCompiler {

    static JavaCompiler compiler;

    final static PrintWriter std_err = new PrintWriter(System.err);
    final static PrintWriter std_out = new PrintWriter(System.out);

    static List<String> options = Arrays.asList("-g", "-nowarn");

    static {
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    //使用弱引用缓存时为了GC能够对类卸载
    static Map<ClassLoader, MyJavaFileManager> classLoaderCache = Collections.synchronizedMap(new WeakHashMap<>());

    public static Class<?> loadClassFromJava(ClassLoader classLoader, String javaCode, String className) {
        // reuse the same file manager to allow caching of jar files
        MyJavaFileManager fileManager = classLoaderCache.get(classLoader);
        if (fileManager == null) {
            classLoaderCache.put(classLoader, fileManager =
                    new MyJavaFileManager(RuntimeJavaCompiler.compiler.getStandardFileManager(null, null, null)));
        }
        boolean called = compiler.getTask(std_err, fileManager, diagnostic -> {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        std_err.println(diagnostic);
                    }
                }, options, null,
                Collections.singletonList(new JavaSourceFromString(className, javaCode)))
                .call();
        if (called) {
            byte[] byteCode = fileManager.getByteCode(className);
            //生成类
            return HackClassloader.defineClass(classLoader, className, byteCode);
        }
        throw new RuntimeCompilerException("编译出错");
    }

    static class JavaSourceFromString extends SimpleJavaFileObject {

        private final String code;

        /**
         * Construct a SimpleJavaFileObject of the given kind and with the
         * given URI.
         *
         * @param name 编译单元的名称(这里是类名)
         * @param code 字节码
         */
        protected JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }
}
