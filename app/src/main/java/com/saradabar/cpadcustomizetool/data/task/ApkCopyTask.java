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

package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.installer.ApksSelector;
import com.saradabar.cpadcustomizetool.data.installer.SplitApkSelector;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkCopyTask implements CopyProgressTask {

    private enum Format {
        XAPK, APKM, APKS;

        @Nullable
        static Format fromFileName(@NonNull String fileName) {
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

            for (Format format : values()) {
                if (format.name().equalsIgnoreCase(extension)) {
                    return format;
                }
            }
            return null;
        }
    }

    private final Format format;
    private String zipPath;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile int totalByte = 0, currentByte = 0;
    volatile int currentFileCount = 0, totalFileCount = 0;
    private volatile boolean finished = false;

    private Listener listener;

    public ApkCopyTask(String fileName) {
        Format format = Format.fromFileName(fileName);

        if (format == null) {
            throw new IllegalArgumentException("Unsupported file: " + fileName);
        }
        this.format = format;
        this.zipPath = fileName;
    }

    public static boolean isSupported(String fileName) {
        return Format.fromFileName(fileName) != null;
    }

    public void execute(Context context, Listener listener, ArrayList<String> data) {
        if (data.isEmpty()) {
            listener.onError(context.getString(R.string.installer_status_unknown_error));
            return;
        }
        zipPath = data.get(0);

        AppExecutors.io(() -> {
            try {
                Analysis analysis = analyze();
                AppExecutors.main(() -> {
                    if (analysis.entries.isEmpty()) {
                        listener.onError(context.getString(R.string.installer_status_unknown_error));
                    } else {
                        listener.onAnalyzed(analysis.entries, analysis.defaults, analysis.notOptimized);
                    }
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? context.getString(R.string.dialog_failure) : e.getMessage();
                AppExecutors.main(() -> listener.onError(message));
            }
        });
    }

    public void extract(Context context, Listener listener, ArrayList<String> entries) {
        this.listener = listener;
        totalByte = 0;
        currentByte = 0;
        currentFileCount = 0;
        totalFileCount = entries.size();
        finished = false;
        listener.onShow();

        AppExecutors.io(() -> {
            Object result;

            try {
                result = extractEntries(context, entries);
            } catch (Throwable t) {
                result = t.getMessage() == null ? context.getString(R.string.dialog_failure) : t.getMessage();
            }

            finished = true;
            Object finalResult = result;
            AppExecutors.main(() -> {
                if (finalResult instanceof ArrayList) {
                    listener.onSuccess((ArrayList<String>) finalResult);
                } else if (Boolean.FALSE.equals(finalResult)) {
                    listener.onFailure();
                } else {
                    listener.onError(String.valueOf(finalResult));
                }
            });
        });
    }

    private Analysis analyze() throws IOException {
        ArrayList<String> entries = new ArrayList<>();
        byte[] toc = null;
        String packageName = null;

        try (ZipFile zip = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> enumeration = zip.entries();

            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();

                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();

                if (name.endsWith(".apk")) {
                    entries.add(name);
                } else if (name.equals("toc.pb")) {
                    toc = readEntry(zip, entry);
                } else if (name.equals("manifest.json") && packageName == null) {
                    packageName = readJsonString(zip, entry, "package_name");
                } else if (name.equals("info.json") && packageName == null) {
                    packageName = readJsonString(zip, entry, "pname");
                }
            }
        }

        Collections.sort(entries);

        boolean ctxz = Common.isCTX() || Common.isCTZ();
        int density = ctxz ? Constants.DENSITY_HDPI : Constants.DENSITY_MDPI;

        List<String> defaults;
        boolean notOptimized = false;

        if (format == Format.APKS) {
            ApksSelector.Selection selection = toc == null
                    ? null
                    : ApksSelector.select(toc, Build.VERSION.SDK_INT, density);

            if (selection == null) {
                defaults = new ArrayList<>();
            } else {
                defaults = new ArrayList<>(selection.paths);
                notOptimized = selection.abiMissing || selection.densityMissing;
            }
        } else {
            SplitApkSelector.Selection selection = SplitApkSelector.select(entries, packageName, density);
            defaults = new ArrayList<>(selection.paths);
            notOptimized = selection.abiMissing || selection.densityMissing;
        }

        for (String name : Constants.SPLIT_ALWAYS_SELECT) {
            for (String entry : entries) {
                if (name.equals(new File(entry).getName()) && !defaults.contains(entry)) {
                    defaults.add(entry);
                }
            }
        }

        return new Analysis(entries, new ArrayList<>(defaults), notOptimized);
    }

    private Object extractEntries(Context context, ArrayList<String> entries) {
        File root = new File(Common.getTemporaryPath(context));
        Common.deleteDirectory(root);

        if (!root.isDirectory() && !root.mkdirs()) {
            return false;
        }

        try (ZipFile zip = new ZipFile(zipPath)) {
            long total = 0;

            for (String name : entries) {
                ZipEntry entry = zip.getEntry(name);

                if (entry != null && !entry.isDirectory()) {
                    total += entry.getSize();
                }
            }
            totalByte = (int) Math.min(total, Integer.MAX_VALUE);

            ArrayList<String> paths = new ArrayList<>();
            String rootPath = root.getCanonicalPath();

            for (String name : entries) {
                ZipEntry entry = zip.getEntry(name);

                if (entry == null || entry.isDirectory()) {
                    continue;
                }

                File destination = new File(root, name);

                if (!destination.getCanonicalPath().startsWith(rootPath + File.separator)) {
                    return false;
                }

                if (!extractEntry(zip, entry, destination)) {
                    return false;
                }

                paths.add(destination.getPath());
                currentFileCount++;
                listener.onFileProgress(currentFileCount, entries.size());
            }

            copyObbEntries(zip);
            return paths;
        } catch (Exception e) {
            return e.getMessage() == null ? context.getString(R.string.dialog_failure) : e.getMessage();
        }
    }

    private boolean extractEntry(ZipFile zip, ZipEntry entry, File destination) throws IOException {
        File parent = destination.getParentFile();

        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }

        try (InputStream input = zip.getInputStream(entry);
             OutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
                currentByte += length;
            }
        }
        return true;
    }

    private void copyObbEntries(ZipFile zip) {
        try {
            if (format == Format.XAPK) {
                Enumeration<? extends ZipEntry> enumeration = zip.entries();

                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    String name = entry.getName();

                    if (entry.isDirectory() || !name.startsWith("Android/obb/")) {
                        continue;
                    }

                    extractEntry(zip, entry, new File(Environment.getExternalStorageDirectory(), name));
                }
            } else if (format == Format.APKM) {
                ZipEntry info = zip.getEntry("info.json");
                String packageName = info == null ? null : readJsonString(zip, info, "pname");

                if (packageName == null) {
                    return;
                }

                Enumeration<? extends ZipEntry> enumeration = zip.entries();

                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();
                    String name = entry.getName();

                    if (entry.isDirectory() || !name.endsWith(".obb")) {
                        continue;
                    }

                    File destination = new File(Environment.getExternalStorageDirectory()
                            + "/Android/obb/" + packageName, new File(name).getName());
                    extractEntry(zip, entry, destination);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static byte[] readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream input = zip.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

    @Nullable
    private static String readJsonString(ZipFile zip, ZipEntry entry, String key) {
        try {
            String json = new String(readEntry(zip, entry), StandardCharsets.UTF_8);
            String value = new JSONObject(json).optString(key);
            return value.isEmpty() ? null : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean isFinish() {
        return finished;
    }

    @Override
    public int getLoadedBytePercent(Context context) {
        if (totalByte <= 0) {
            return 0;
        }
        return (int) Math.min(Constants.PERCENT_NOT_FINISHED, (currentByte * 100L) / totalByte);
    }

    @Override
    public int getLoadedTotalByte() {
        return totalByte / Constants.BYTE_TO_MB;
    }

    @Override
    public int getLoadedCurrentByte(Context context) {
        return currentByte / Constants.BYTE_TO_MB;
    }

    @Override
    public int getCurrentFileCount() {
        return currentFileCount;
    }

    @Override
    public int getTotalFileCount() {
        return totalFileCount;
    }

    public interface Listener {
        void onShow();

        void onAnalyzed(ArrayList<String> entries, ArrayList<String> defaults, boolean notOptimized);

        void onSuccess(ArrayList<String> paths);

        void onFailure();

        void onError(String message);

        void onProgressUpdate(String message);

        void onFileProgress(int current, int total);
    }

    private static final class Analysis {
        final ArrayList<String> entries;
        final ArrayList<String> defaults;
        final boolean notOptimized;

        Analysis(ArrayList<String> entries, ArrayList<String> defaults, boolean notOptimized) {
            this.entries = entries;
            this.defaults = defaults;
            this.notOptimized = notOptimized;
        }
    }
}
