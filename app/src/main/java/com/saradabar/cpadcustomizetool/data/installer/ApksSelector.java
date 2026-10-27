/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.data.installer;

import com.saradabar.cpadcustomizetool.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ApksSelector {

    private static final Set<String> ABIS = new HashSet<>(Arrays.asList(
            Constants.SPLIT_ABI_ARMEABI, Constants.SPLIT_ABI_ARMEABI_V7A, Constants.SPLIT_ABI_ARM64_V8A,
            Constants.SPLIT_ABI_X86, Constants.SPLIT_ABI_X86_64, Constants.SPLIT_ABI_MIPS,
            Constants.SPLIT_ABI_MIPS64, Constants.SPLIT_ABI_RISCV64));
    private static final Set<String> DENSITIES = new HashSet<>(Arrays.asList(
            Constants.SPLIT_DENSITY_LDPI, Constants.SPLIT_DENSITY_MDPI, Constants.SPLIT_DENSITY_TVDPI,
            Constants.SPLIT_DENSITY_HDPI, Constants.SPLIT_DENSITY_XHDPI, Constants.SPLIT_DENSITY_XXHDPI,
            Constants.SPLIT_DENSITY_XXXHDPI));

    public static final class Selection {
        public final List<String> paths;
        public final boolean abiMissing;
        public final boolean densityMissing;

        Selection(List<String> paths, boolean abiMissing, boolean densityMissing) {
            this.paths = paths;
            this.abiMissing = abiMissing;
            this.densityMissing = densityMissing;
        }
    }

    private ApksSelector() {
    }

    public static Selection select(byte[] toc, int sdk, int densityDpi) {
        try {
            List<Variant> variants = parseVariants(toc);
            Variant variant = chooseVariant(variants, sdk);

            if (variant == null) {
                return new Selection(Collections.emptyList(), false, false);
            }

            Set<String> abis = new HashSet<>();
            Set<String> densities = new HashSet<>();

            for (Apk apk : variant.apks) {
                String qualifier = qualifier(apk.path);

                if (qualifier == null) {
                    continue;
                }

                if (ABIS.contains(qualifier)) {
                    abis.add(qualifier);
                } else if (DENSITIES.contains(qualifier)) {
                    densities.add(qualifier);
                }
            }

            boolean abiMissing = !abis.isEmpty()
                    && !abis.contains("arm64_v8a")
                    && !abis.contains("armeabi_v7a");
            boolean densityMissing = !densities.isEmpty()
                    && !densities.contains(densityToken(densityDpi));

            boolean useArm64 = variant.abis.isEmpty() || variant.abis.contains(Constants.ABI_ARM64_V8A);
            int densityFilter = densityDpi;

            List<Apk> apks = new ArrayList<>();

            for (Apk apk : variant.apks) {
                if (matches(apk, sdk, useArm64, densityFilter)) {
                    apks.add(apk);
                }
            }

            Collections.sort(apks, (first, second) -> Boolean.compare(second.master, first.master));

            List<String> paths = new ArrayList<>();

            for (Apk apk : apks) {
                paths.add(apk.path);
            }
            return new Selection(paths, abiMissing, densityMissing);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String densityToken(int densityDpi) {
        switch (densityDpi) {
            case Constants.DENSITY_MDPI:
                return Constants.SPLIT_DENSITY_MDPI;
            case Constants.DENSITY_HDPI:
                return Constants.SPLIT_DENSITY_HDPI;
            case Constants.DENSITY_XXXHDPI:
                return Constants.SPLIT_DENSITY_XXXHDPI;
            default:
                return "";
        }
    }

    private static String qualifier(String path) {
        String name = new File(path).getName();

        if (!name.endsWith(".apk")) {
            return null;
        }

        name = name.substring(0, name.length() - 4);
        int index = name.lastIndexOf('-');
        return index >= 0 ? name.substring(index + 1) : null;
    }

    private static Variant chooseVariant(List<Variant> variants, int sdk) {
        Variant best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Variant variant : variants) {
            if (variant.sdkMin > sdk) {
                continue;
            }

            int abiScore = scoreAbi(variant.abis);

            if (abiScore < 0) {
                continue;
            }

            int score = abiScore * 10000 + variant.sdkMin * 10 + variant.number;

            if (score > bestScore) {
                bestScore = score;
                best = variant;
            }
        }
        return best;
    }

    private static int scoreAbi(Set<Integer> abis) {
        if (abis.isEmpty()) {
            return 1;
        }

        if (abis.contains(Constants.ABI_ARM64_V8A)) {
            return 3;
        }

        if (abis.contains(Constants.ABI_ARMEABI_V7A)) {
            return 2;
        }
        return -1;
    }

    private static boolean matches(Apk apk, int sdk, boolean arm64, int densityDpi) {
        if (apk.sdkMin > sdk) {
            return false;
        }

        if (apk.abi != 0 && apk.abi != (arm64 ? Constants.ABI_ARM64_V8A : Constants.ABI_ARMEABI_V7A)) {
            return false;
        }

        if (!apk.densities.isEmpty() && !apk.densities.contains(densityDpi)) {
            return false;
        }

        if (!apk.languages.isEmpty() && !apk.languages.contains("ja")) {
            return false;
        }
        return true;
    }

    private static List<Variant> parseVariants(byte[] data) {
        List<Variant> variants = new ArrayList<>();
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                variants.add(parseVariant(reader.nextBytes()));
            } else {
                reader.skip(wire(tag));
            }
        }
        return variants;
    }

    private static Variant parseVariant(byte[] data) {
        Variant variant = new Variant();
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                parseVariantTargeting(reader.nextBytes(), variant);
            } else if (field(tag) == 2 && wire(tag) == 2) {
                parseApkSet(reader.nextBytes(), variant);
            } else if (field(tag) == 3 && wire(tag) == 0) {
                variant.number = reader.nextInt();
            } else {
                reader.skip(wire(tag));
            }
        }
        return variant;
    }

    private static void parseVariantTargeting(byte[] data, Variant variant) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                variant.sdkMin = parseSdkTargeting(reader.nextBytes());
            } else if (field(tag) == 2 && wire(tag) == 2) {
                parseAbiTargeting(reader.nextBytes(), variant.abis);
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static int parseSdkTargeting(byte[] data) {
        Reader reader = new Reader(data);
        int min = 0;

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                int value = parseSdkVersion(reader.nextBytes());

                if (min == 0 || (value > 0 && value < min)) {
                    min = value;
                }
            } else {
                reader.skip(wire(tag));
            }
        }
        return min;
    }

    private static int parseSdkVersion(byte[] data) {
        Reader reader = new Reader(data);
        int value = 0;

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                Reader wrapper = new Reader(reader.nextBytes());

                while (wrapper.hasMore()) {
                    int wrapperTag = wrapper.nextTag();

                    if (field(wrapperTag) == 1 && wire(wrapperTag) == 0) {
                        value = wrapper.nextInt();
                    } else {
                        wrapper.skip(wire(wrapperTag));
                    }
                }
            } else {
                reader.skip(wire(tag));
            }
        }
        return value;
    }

    private static void parseAbiTargeting(byte[] data, Set<Integer> abis) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                int abi = parseAbi(reader.nextBytes());

                if (abi > 0) {
                    abis.add(abi);
                }
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static int parseAbi(byte[] data) {
        Reader reader = new Reader(data);
        int abi = 0;

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 0) {
                abi = reader.nextInt();
            } else {
                reader.skip(wire(tag));
            }
        }
        return abi;
    }

    private static void parseApkSet(byte[] data, Variant variant) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 2 && wire(tag) == 2) {
                Apk apk = parseApkDescription(reader.nextBytes());

                if (apk.path != null && !apk.path.isEmpty()) {
                    variant.apks.add(apk);
                }
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static Apk parseApkDescription(byte[] data) {
        Apk apk = new Apk();
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                parseApkTargeting(reader.nextBytes(), apk);
            } else if (field(tag) == 2 && wire(tag) == 2) {
                apk.path = reader.nextString();
            } else if (field(tag) == 3 && wire(tag) == 2) {
                parseSplitMetadata(reader.nextBytes(), apk);
            } else {
                reader.skip(wire(tag));
            }
        }
        return apk;
    }

    private static void parseSplitMetadata(byte[] data, Apk apk) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 2 && wire(tag) == 0) {
                apk.master = reader.nextInt() != 0;
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static void parseApkTargeting(byte[] data, Apk apk) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();
            int field = field(tag);

            if (field == 1 && wire(tag) == 2) {
                Set<Integer> abis = new HashSet<>();
                parseAbiTargeting(reader.nextBytes(), abis);
                apk.abi = abis.isEmpty() ? 0 : abis.iterator().next();
            } else if (field == 3 && wire(tag) == 2) {
                parseLanguageTargeting(reader.nextBytes(), apk.languages);
            } else if (field == 4 && wire(tag) == 2) {
                parseDensityTargeting(reader.nextBytes(), apk.densities);
            } else if (field == 5 && wire(tag) == 2) {
                apk.sdkMin = parseSdkTargeting(reader.nextBytes());
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static void parseLanguageTargeting(byte[] data, Set<String> languages) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                languages.add(reader.nextString());
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static void parseDensityTargeting(byte[] data, Set<Integer> densities) {
        Reader reader = new Reader(data);

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 2) {
                int density = parseScreenDensity(reader.nextBytes());

                if (density > 0) {
                    densities.add(density);
                }
            } else {
                reader.skip(wire(tag));
            }
        }
    }

    private static int parseScreenDensity(byte[] data) {
        Reader reader = new Reader(data);
        int density = 0;

        while (reader.hasMore()) {
            int tag = reader.nextTag();

            if (field(tag) == 1 && wire(tag) == 0) {
                density = densityAliasToDpi(reader.nextInt());
            } else if (field(tag) == 2 && wire(tag) == 0) {
                density = reader.nextInt();
            } else {
                reader.skip(wire(tag));
            }
        }
        return density;
    }

    private static int densityAliasToDpi(int alias) {
        switch (alias) {
            case 2:
                return Constants.DENSITY_LDPI;
            case 3:
                return Constants.DENSITY_MDPI;
            case 4:
                return Constants.DENSITY_TVDPI;
            case 5:
                return Constants.DENSITY_HDPI;
            case 6:
                return Constants.DENSITY_XHDPI;
            case 7:
                return Constants.DENSITY_XXHDPI;
            case 8:
                return Constants.DENSITY_XXXHDPI;
            default:
                return 0;
        }
    }

    private static int field(int tag) {
        return tag >>> 3;
    }

    private static int wire(int tag) {
        return tag & 7;
    }

    private static final class Variant {
        int sdkMin;
        int number;
        final Set<Integer> abis = new HashSet<>();
        final List<Apk> apks = new ArrayList<>();
    }

    private static final class Apk {
        String path = "";
        boolean master;
        int abi;
        int sdkMin;
        final Set<Integer> densities = new HashSet<>();
        final Set<String> languages = new HashSet<>();
    }

    private static final class Reader {
        private final byte[] data;
        private int position;

        Reader(byte[] data) {
            this.data = data;
        }

        boolean hasMore() {
            return position < data.length;
        }

        int nextTag() {
            return (int) readVarint();
        }

        int nextInt() {
            return (int) readVarint();
        }

        byte[] nextBytes() {
            int length = (int) readVarint();
            byte[] value = new byte[length];
            System.arraycopy(data, position, value, 0, length);
            position += length;
            return value;
        }

        String nextString() {
            return new String(nextBytes(), StandardCharsets.UTF_8);
        }

        long readVarint() {
            long value = 0;
            int shift = 0;

            while (position < data.length) {
                int current = data[position++] & 0xFF;
                value |= (long) (current & 0x7F) << shift;

                if ((current & 0x80) == 0) {
                    return value;
                }
                shift += 7;
            }
            throw new IllegalArgumentException("truncated");
        }

        void skip(int wireType) {
            switch (wireType) {
                case 0:
                    readVarint();
                    break;
                case 1:
                    position += 8;
                    break;
                case 2:
                    int length = (int) readVarint();
                    position += length;
                    break;
                case 5:
                    position += 4;
                    break;
                default:
                    throw new IllegalArgumentException("wire " + wireType);
            }
        }
    }
}
