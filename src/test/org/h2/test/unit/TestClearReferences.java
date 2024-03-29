/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.unit;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import org.h2.test.TestBase;
import org.h2.util.MathUtils;
import org.h2.util.New;
import org.h2.value.ValueInt;

/**
 * Tests if Tomcat would clear static fields when re-loading a web application.
 * See also
 * http://svn.apache.org/repos/asf/tomcat/trunk/java/org/apache/catalina
 * /loader/WebappClassLoader.java
 */
public class TestClearReferences extends TestBase {

    private static final String[] KNOWN_REFRESHED = {
        "org.h2.compress.CompressLZF.cachedHashTable",
        "org.h2.constant.DbSettings.defaultSettings",
        "org.h2.engine.SessionRemote.sessionFactory",
        "org.h2.jdbcx.JdbcDataSourceFactory.cachedTraceSystem",
        "org.h2.store.RecoverTester.instance",
        "org.h2.store.fs.FilePath.defaultProvider",
        "org.h2.store.fs.FilePath.providers",
        "org.h2.store.fs.FilePath.tempRandom",
        "org.h2.store.fs.FilePathRec.recorder",
        "org.h2.tools.CompressTool.cachedBuffer",
        "org.h2.util.CloseWatcher.queue",
        "org.h2.util.CloseWatcher.refs",
        "org.h2.util.DateTimeUtils.cachedCalendar",
        "org.h2.util.MathUtils.cachedSecureRandom",
        "org.h2.util.NetUtils.cachedLocalAddress",
        "org.h2.util.StringUtils.softCache",
        "org.h2.util.Utils.allowedClassNames",
        "org.h2.util.Utils.allowedClassNamePrefixes",
        "org.h2.value.CompareMode.lastUsed",
        "org.h2.value.Value.softCache",
    };

    private boolean hasError;

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    private void clear() throws Exception {
        ArrayList<Class <?>> classes = New.arrayList();
        check(classes, new File("bin/org/h2"));
        check(classes, new File("temp/org/h2"));
        for (Class<?> clazz : classes) {
            clearClass(clazz);
        }
    }

    @Override
    public void test() throws Exception {
        // initialize the known classes
        MathUtils.secureRandomLong();
        ValueInt.get(1);
        Class.forName("org.h2.store.fs.FileMemData");

        clear();

        if (hasError) {
            fail("Tomcat may clear the field above when reloading the web app");
        }
        for (String s : KNOWN_REFRESHED) {
            String className = s.substring(0, s.lastIndexOf('.'));
            String fieldName = s.substring(s.lastIndexOf('.') + 1);
            Class<?> clazz = Class.forName(className);
            try {
                clazz.getDeclaredField(fieldName);
            } catch (Exception e) {
                fail(s);
            }
        }
    }

    private void check(ArrayList<Class <?>> classes, File file) {
        String name = file.getName();
        if (file.isDirectory()) {
            if (name.equals("CVS") || name.equals(".svn")) {
                return;
            }
            for (File f : file.listFiles()) {
                check(classes, f);
            }
        } else {
            if (!name.endsWith(".class")) {
                return;
            }
            String className = file.getAbsolutePath().replace('\\', '/');
            className = className.substring(className.lastIndexOf("org/h2"));
            String packageName = className.substring(0, className.lastIndexOf('/'));
            if (!new File("src/main/" + packageName).exists()) {
                return;
            }
            className = className.replace('/', '.');
            className = className.substring(0, className.length() - ".class".length());
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.out.println("Could not load " + className + ": " + e.toString());
            }
            if (clazz != null) {
                classes.add(clazz);
            }
        }
    }

    /**
     * This is how Tomcat resets the fields as of 2009-01-30.
     *
     * @param clazz the class to clear
     */
    private void clearClass(Class<?> clazz) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().isPrimitive() || field.getName().indexOf("$") != -1) {
                continue;
            }
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers)) {
                continue;
            }
            field.setAccessible(true);
            Object o = field.get(null);
            if (o == null) {
                continue;
            }
            if (Modifier.isFinal(modifiers)) {
                if (field.getType().getName().startsWith("java.")) {
                    continue;
                }
                if (field.getType().getName().startsWith("javax.")) {
                    continue;
                }
                clearInstance(o);
            } else {
                clearField(clazz.getName() + "." + field.getName() + " = " + o);
            }
        }
    }

    private void clearInstance(Object instance) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive() || (field.getName().indexOf("$") != -1)) {
                continue;
            }
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                continue;
            }
            field.setAccessible(true);
            Object o = field.get(instance);
            if (o == null) {
                continue;
            }
            // loadedByThisOrChild
            if (o.getClass().getName().startsWith("java.lang.")) {
                continue;
            }
            if (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()) {
                continue;
            }
            clearField(instance.getClass().getName() + "." + field.getName() + " = " + o);
        }
    }

    private void clearField(String s) {
        for (String k : KNOWN_REFRESHED) {
            if (s.startsWith(k)) {
                return;
            }
        }
        hasError = true;
        System.out.println(s);
    }

}
