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

import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SplitApkSelector {

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

    private SplitApkSelector() {
    }

    public static Selection select(List<String> entries, @Nullable String packageName, int densityDpi) {
        List<String> apks = new ArrayList<>();

        for (String entry : entries) {
            if (entry.endsWith(".apk")) {
                apks.add(entry);
            }
        }

        Collections.sort(apks);

        Set<String> abis = new HashSet<>();
        Set<String> densities = new HashSet<>();

        for (String apk : apks) {
            String qualifier = qualifier(baseName(apk));

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
                && !abis.contains(Constants.SPLIT_ABI_ARM64_V8A)
                && !abis.contains(Constants.SPLIT_ABI_ARMEABI_V7A);
        boolean densityMissing = !densities.isEmpty()
                && !densities.contains(densityQualifier(densityDpi));

        Set<String> selected = new LinkedHashSet<>();
        String core = findCore(apks, packageName);

        if (core != null) {
            selected.add(core);
        }

        for (String apk : apks) {
            if (qualifier(baseName(apk)) == null) {
                selected.add(apk);
            }
        }

        String abi = findByQualifier(apks, Constants.SPLIT_ABI_ARM64_V8A);

        if (abi == null) {
            abi = findByQualifier(apks, Constants.SPLIT_ABI_ARMEABI_V7A);
        }

        if (abi != null) {
            selected.add(abi);
        }

        String density = findByQualifier(apks, densityQualifier(densityDpi));

        if (density != null) {
            selected.add(density);
        }

        String language = findByQualifier(apks, Constants.SPLIT_LANGUAGE_JA);

        if (language != null) {
            selected.add(language);
        }

        return new Selection(new ArrayList<>(selected), abiMissing, densityMissing);
    }

    @Nullable
    private static String findCore(List<String> apks, @Nullable String packageName) {
        for (String apk : apks) {
            String name = baseName(apk);
            String withoutExtension = name.endsWith(".apk")
                    ? name.substring(0, name.length() - 4)
                    : name;

            if ("base".equals(withoutExtension) || "base".equals(qualifier(name))) {
                return apk;
            }

            if (packageName != null && packageName.equals(withoutExtension)) {
                return apk;
            }
        }

        for (String apk : apks) {
            if (qualifier(baseName(apk)) == null) {
                return apk;
            }
        }
        return null;
    }

    @Nullable
    private static String findByQualifier(List<String> apks, String qualifier) {
        if (qualifier.isEmpty()) {
            return null;
        }

        for (String apk : apks) {
            if (qualifier.equals(qualifier(baseName(apk)))) {
                return apk;
            }
        }
        return null;
    }

    private static String densityQualifier(int densityDpi) {
        switch (densityDpi) {
            case Constants.DENSITY_LDPI:
                return Constants.SPLIT_DENSITY_LDPI;
            case Constants.DENSITY_MDPI:
                return Constants.SPLIT_DENSITY_MDPI;
            case Constants.DENSITY_TVDPI:
                return Constants.SPLIT_DENSITY_TVDPI;
            case Constants.DENSITY_HDPI:
                return Constants.SPLIT_DENSITY_HDPI;
            case Constants.DENSITY_XHDPI:
                return Constants.SPLIT_DENSITY_XHDPI;
            case Constants.DENSITY_XXHDPI:
                return Constants.SPLIT_DENSITY_XXHDPI;
            case Constants.DENSITY_XXXHDPI:
                return Constants.SPLIT_DENSITY_XXXHDPI;
            default:
                return "";
        }
    }

    private static String baseName(String entry) {
        int index = entry.lastIndexOf('/');
        return index >= 0 ? entry.substring(index + 1) : entry;
    }

    @Nullable
    private static String qualifier(String fileName) {
        String name = fileName.endsWith(".apk")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;

        if (name.startsWith("split_config.")) {
            return name.substring("split_config.".length());
        }

        if (name.startsWith("config.")) {
            return name.substring("config.".length());
        }
        return null;
    }
}
