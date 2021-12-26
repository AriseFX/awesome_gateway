package com.ewell.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The type Extension loader.
 * This is done by loading the properties file.
 *
 * @see <a href="https://github.com/apache/dubbo/blob/master/dubbo-common/src/main/java/org/apache/dubbo/common/extension/ExtensionLoader.java">ExtensionLoader</a>
 */
@SuppressWarnings("all")
public final class ExtensionLoader<T> {

    private static final String SPI_DIRECTORY = "META-INF/spi/";

    private static final Map<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();

    private final Class<T> clazz;

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    private final Map<Class<?>, Object> joinInstances = new ConcurrentHashMap<>();

    private String cachedDefaultName;

    /**
     * Instantiates a new Extension loader.
     *
     * @param clazz the clazz.
     */
    private ExtensionLoader(final Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Gets extension loader.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the extension loader.
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(final Class<T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("extension clazz is null");
        }
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("extension clazz (" + clazz + ") is not interface!");
        }
        if (!clazz.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("extension clazz (" + clazz + ") without @" + SPI.class + " Annotation");
        }
        ExtensionLoader<T> extensionLoader = (ExtensionLoader<T>) LOADERS.get(clazz);
        if (extensionLoader != null) {
            return extensionLoader;
        }
        LOADERS.putIfAbsent(clazz, new ExtensionLoader<>(clazz));
        return (ExtensionLoader<T>) LOADERS.get(clazz);
    }

    /**
     * Gets default join.
     *
     * @return the default join.
     */
    public T getDefaultJoin() {
        getExtensionClasses();
        return getJoin(cachedDefaultName);
    }

    /**
     * Gets join.
     *
     * @param name the name
     * @return the join.
     */
    public T getJoin(final String name) {
        Holder<Object> objectHolder = cachedInstances.get(name);
        if (objectHolder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            objectHolder = cachedInstances.get(name);
        }
        Object value = objectHolder.getValue();
        if (value == null) {
            synchronized (cachedInstances) {
                value = objectHolder.getValue();
                if (value == null) {
                    value = createExtension(name);
                    objectHolder.setValue(value);
                }
            }
        }
        return (T) value;
    }

    /**
     * Get all join.
     *
     * @return all join.
     */
    public List<T> getAllJoin() {
        return getExtensionClasses()
                .keySet()
                .stream()
                .map(e -> getJoin(e)).collect(Collectors.toList());
    }

    /**
     * Get all join.
     *
     * @return all join.
     */
    public Set<String> getAllJoinSpi() {
        return this.getExtensionClasses().keySet();
    }

    @SuppressWarnings("unchecked")
    private T createExtension(final String name) {
        Class<?> aClass = getExtensionClasses().get(name);
        if (aClass == null) {
            throw new IllegalArgumentException("name is error");
        }
        Object o = joinInstances.get(aClass);
        if (o == null) {
            try {
                joinInstances.putIfAbsent(aClass, aClass.newInstance());
                o = joinInstances.get(aClass);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Extension instance(name: " + name + ", class: "
                        + aClass + ")  could not be instantiated: " + e.getMessage(), e);

            }
        }
        return (T) o;
    }

    /**
     * Gets extension classes.
     *
     * @return the extension classes
     */
    public Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.getValue();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.getValue();
                if (classes == null) {
                    classes = loadExtensionClass();
                    cachedClasses.setValue(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClass() {
        SPI annotation = clazz.getAnnotation(SPI.class);
        if (annotation != null) {
            String value = annotation.value();
            cachedDefaultName = value;
        }
        Map<String, Class<?>> classes = new HashMap<>(16);
        loadDirectory(classes);
        return classes;
    }

    /**
     * Load files under SHENYU_DIRECTORY.
     */
    private void loadDirectory(final Map<String, Class<?>> classes) {
        String fileName = SPI_DIRECTORY + clazz.getName();
        try {
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            Enumeration<URL> urls = classLoader != null ? classLoader.getResources(fileName)
                    : ClassLoader.getSystemResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    loadResources(classes, url);
                }
            }
        } catch (IOException t) {
            t.printStackTrace();
        }
    }

    private void loadResources(final Map<String, Class<?>> classes, final URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.forEach((k, v) -> {
                String name = (String) k;
                String classPath = (String) v;
                try {
                    loadClass(classes, name, classPath);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("load extension resources error", e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("load extension resources error", e);
        }
    }

    private void loadClass(final Map<String, Class<?>> classes,
                           final String name, final String classPath) throws ClassNotFoundException {
        Class<?> subClass = Class.forName(classPath);
        if (!clazz.isAssignableFrom(subClass)) {
            throw new IllegalStateException("load extension resources error," + subClass + " subtype is not of " + clazz);
        }
        Join annotation = subClass.getAnnotation(Join.class);
        if (annotation == null) {
            throw new IllegalStateException("load extension resources error," + subClass + " with Join annotation");
        }
        Class<?> oldClass = classes.get(name);
        if (oldClass == null) {
            classes.put(name, subClass);
        } else if (oldClass != subClass) {
            throw new IllegalStateException("load extension resources error,Duplicate class " + clazz.getName() + " name " + name + " on " + oldClass.getName() + " or " + subClass.getName());
        }
    }

    /**
     * The type Holder.
     *
     * @param <T> the type parameter.
     */
    public static class Holder<T> {

        private volatile T value;

        /**
         * Gets value.
         *
         * @return the value
         */
        public T getValue() {
            return value;
        }

        /**
         * Sets value.
         *
         * @param value the value
         */
        public void setValue(final T value) {
            this.value = value;
        }
    }
}
