/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License, Version
 * 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html). Initial Developer: H2 Group
 */
package org.h2.test.store;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;
import org.h2.mvstore.type.StringDataType;
import org.h2.store.fs.FileUtils;
import org.h2.test.TestBase;
import org.h2.util.New;

/**
 * Tests the r-tree.
 */
public class TestMVRTree extends TestMVStore {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    @Override
    public void test() {
        FileUtils.deleteRecursive(getBaseDir(), true);
        FileUtils.createDirectories(getBaseDir());

        testExample();
        testMany();
        testSimple();
        testRandom();
    }

    private void testExample() {
        // create an in-memory store
        MVStore s = MVStore.open(null);

        // open an R-tree map
        MVRTreeMap<String> r = s.openMap("data",
                new MVRTreeMap.Builder<String>());

        // add two key-value pairs
        // the first value is the key id (to make the key unique)
        // then the min x, max x, min y, max y
        r.add(new SpatialKey(0, -3f, -2f, 2f, 3f), "left");
        r.add(new SpatialKey(1, 3f, 4f, 4f, 5f), "right");

        // iterate over the intersecting keys
        Iterator<SpatialKey> it = r.findIntersectingKeys(
                new SpatialKey(0, 0f, 9f, 3f, 6f));
        for (SpatialKey k; it.hasNext();) {
            k = it.next();
            // System.out.println(k + ": " + r.get(k));
            assertTrue(k != null);
        }
        s.close();
    }

    private void testMany() {
        String fileName = getBaseDir() + "/testMany.h3";
        FileUtils.delete(fileName);
        MVStore s;
        s = openStore(fileName);
        // s.setMaxPageSize(50);
        MVRTreeMap<String> r = s.openMap("data",
                new MVRTreeMap.Builder<String>().dimensions(2).
                valueType(StringDataType.INSTANCE));
        // r.setQuadraticSplit(true);
        Random rand = new Random(1);
        int len = 1000;
        // long t = System.currentTimeMillis();
        // Profiler prof = new Profiler();
        // prof.startCollecting();
        for (int i = 0; i < len; i++) {
            float x = rand.nextFloat(), y = rand.nextFloat();
            float p = (float) (rand.nextFloat() * 0.000001);
            SpatialKey k = new SpatialKey(i, x - p, x + p, y - p, y + p);
            r.add(k, "" + i);
            if (i > 0 && (i % len / 10) == 0) {
                s.store();
            }
            if (i > 0 && (i % 10000) == 0) {
                render(r, getBaseDir() + "/test.png");
            }
        }
        // System.out.println(prof.getTop(5));
        // System.out.println("add: " + (System.currentTimeMillis() - t));
        s.store();
        s.close();
        s = openStore(fileName);
        r = s.openMap("data",
                new MVRTreeMap.Builder<String>().dimensions(2).
                valueType(StringDataType.INSTANCE));
        // t = System.currentTimeMillis();
        rand = new Random(1);
        for (int i = 0; i < len; i++) {
            float x = rand.nextFloat(), y = rand.nextFloat();
            float p = (float) (rand.nextFloat() * 0.000001);
            SpatialKey k = new SpatialKey(i, x - p, x + p, y - p, y + p);
            assertEquals("" + i, r.get(k));
        }
        // System.out.println("query: " + (System.currentTimeMillis() - t));
        assertEquals(len, r.size());
        int count = 0;
        for (SpatialKey k : r.keySet()) {
            assertTrue(r.get(k) != null);
            count++;
        }
        assertEquals(len, count);
        // t = System.currentTimeMillis();
        // Profiler prof = new Profiler();
        // prof.startCollecting();
        rand = new Random(1);
        for (int i = 0; i < len; i++) {
            float x = rand.nextFloat(), y = rand.nextFloat();
            float p = (float) (rand.nextFloat() * 0.000001);
            SpatialKey k = new SpatialKey(i, x - p, x + p, y - p, y + p);
            r.remove(k);
        }
        assertEquals(0, r.size());
        s.close();
        // System.out.println(prof.getTop(5));
        // System.out.println("remove: " + (System.currentTimeMillis() - t));
    }

    private void testSimple() {
        String fileName = getBaseDir() + "/testTree.h3";
        FileUtils.delete(fileName);
        MVStore s;
        s = openStore(fileName);
        MVRTreeMap<String> r = s.openMap("data",
                new MVRTreeMap.Builder<String>().dimensions(2).
                valueType(StringDataType.INSTANCE));

        add(r, "Bern", key(0, 46.57, 7.27, 124381));
        add(r, "Basel", key(1, 47.34, 7.36, 170903));
        add(r, "Zurich", key(2, 47.22, 8.33, 376008));
        add(r, "Lucerne", key(3, 47.03, 8.18, 77491));
        add(r, "Geneva", key(4, 46.12, 6.09, 191803));
        add(r, "Lausanne", key(5, 46.31, 6.38, 127821));
        add(r, "Winterthur", key(6, 47.30, 8.45, 102966));
        add(r, "St. Gallen", key(7, 47.25, 9.22, 73500));
        add(r, "Biel/Bienne", key(8, 47.08, 7.15, 51203));
        add(r, "Lugano", key(9, 46.00, 8.57, 54667));
        add(r, "Thun", key(10, 46.46, 7.38, 42623));
        add(r, "Bellinzona", key(11, 46.12, 9.01, 17373));
        add(r, "Chur", key(12, 46.51, 9.32, 33756));
        // render(r, getBaseDir() + "/test.png");
        ArrayList<String> list = New.arrayList();
        for (SpatialKey x : r.keySet()) {
            list.add(r.get(x));
        }
        Collections.sort(list);
        assertEquals("[Basel, Bellinzona, Bern, Biel/Bienne, Chur, Geneva, " +
                "Lausanne, Lucerne, Lugano, St. Gallen, Thun, Winterthur, Zurich]",
                list.toString());

        SpatialKey k;

        // intersection
        list.clear();
        k = key(0, 47.34, 7.36, 0);
        for (Iterator<SpatialKey> it = r.findIntersectingKeys(k); it.hasNext();) {
            list.add(r.get(it.next()));
        }
        Collections.sort(list);
        assertEquals("[Basel]", list.toString());

        // contains
        list.clear();
        k = key(0, 47.34, 7.36, 0);
        for (Iterator<SpatialKey> it = r.findContainedKeys(k); it.hasNext();) {
            list.add(r.get(it.next()));
        }
        assertEquals(0, list.size());
        k = key(0, 47.34, 7.36, 171000);
        for (Iterator<SpatialKey> it = r.findContainedKeys(k); it.hasNext();) {
            list.add(r.get(it.next()));
        }
        assertEquals("[Basel]", list.toString());

        s.close();
    }

    private static void add(MVRTreeMap<String> r, String name, SpatialKey k) {
        r.put(k, name);
    }

    private static SpatialKey key(int id, double y, double x, int population) {
        float a = (float) ((int) x + (x - (int) x) * 5 / 3);
        float b = 50 - (float) ((int) y + (y - (int) y) * 5 / 3);
        float s = (float) Math.sqrt(population / 10000000.);
        SpatialKey k = new SpatialKey(id, a - s, a + s, b - s, b + s);
        return k;
    }

    private static void render(MVRTreeMap<String> r, String fileName) {
        int width = 1000, height = 500;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        SpatialKey b = new SpatialKey(0, Float.MAX_VALUE, Float.MIN_VALUE,
                Float.MAX_VALUE, Float.MIN_VALUE);
        for (SpatialKey x : r.keySet()) {
            b.setMin(0, Math.min(b.min(0), x.min(0)));
            b.setMin(1, Math.min(b.min(1), x.min(1)));
            b.setMax(0, Math.max(b.max(0), x.max(0)));
            b.setMax(1, Math.max(b.max(1), x.max(1)));
        }
        // System.out.println(b);
        for (SpatialKey x : r.keySet()) {
            int[] rect = scale(b, x, width, height);
            g2d.drawRect(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
            String s = r.get(x);
            g2d.drawChars(s.toCharArray(), 0, s.length(), rect[0], rect[1] - 4);
        }
        g2d.setColor(Color.red);
        ArrayList<SpatialKey> list = New.arrayList();
        r.addNodeKeys(list,  r.getRoot());
        for (SpatialKey x : list) {
            int[] rect = scale(b, x, width, height);
            g2d.drawRect(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
        }
        ImageWriter out = ImageIO.getImageWritersByFormatName("png").next();
        try {
            out.setOutput(new FileImageOutputStream(new File(fileName)));
            out.write(img);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] scale(SpatialKey b, SpatialKey x, int width, int height) {
        int[] rect = {
                (int) ((x.min(0) - b.min(0)) * (width * 0.9) / (b.max(0) - b.min(0)) + width * 0.05),
                (int) ((x.min(1) - b.min(1)) * (height * 0.9) / (b.max(1) - b.min(1)) + height * 0.05),
                (int) ((x.max(0) - b.min(0)) * (width * 0.9) / (b.max(0) - b.min(0)) + width * 0.05),
                (int) ((x.max(1) - b.min(1)) * (height * 0.9) / (b.max(1) - b.min(1)) + height * 0.05),
                };
        return rect;
    }

    private void testRandom() {
        testRandom(true);
        testRandom(false);
    }

    private void testRandom(boolean quadraticSplit) {
        String fileName = getBaseDir() + "/testRandom.h3";
        FileUtils.delete(fileName);
        MVStore s = openStore(fileName);

        MVRTreeMap<String> m = s.openMap("data",
                new MVRTreeMap.Builder<String>());

        m.setQuadraticSplit(quadraticSplit);
        HashMap<SpatialKey, String> map = new HashMap<SpatialKey, String>();
        Random rand = new Random(1);
        int operationCount = 1000;
        int maxValue = 30;
        for (int i = 0; i < operationCount; i++) {
            int key = rand.nextInt(maxValue);
            Random rk = new Random(key);
            float x = rk.nextFloat(), y = rk.nextFloat();
            float p = (float) (rk.nextFloat() * 0.000001);
            SpatialKey k = new SpatialKey(key, x - p, x + p, y - p, y + p);
            String v = "" + rand.nextInt();
            switch (rand.nextInt(3)) {
            case 0:
                log(i + ": put " + k + " = " + v + " " + m.size());
                m.put(k, v);
                map.put(k, v);
                break;
            case 1:
                log(i + ": remove " + k + " " + m.size());
                m.remove(k);
                map.remove(k);
                break;
            default:
                String a = map.get(k);
                String b = m.get(k);
                if (a == null || b == null) {
                    assertTrue(a == b);
                } else {
                    assertEquals(a, b);
                }
                break;
            }
            assertEquals(map.size(), m.size());
        }
        s.close();
    }

}
