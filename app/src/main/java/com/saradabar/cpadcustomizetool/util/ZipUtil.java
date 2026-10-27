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

package com.saradabar.cpadcustomizetool.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtil {

    public interface EntryListener {
        void onEntry(int current, int total);
    }

    public static long getUncompressedSize(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            long total = 0;
            boolean known = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                long size = entries.nextElement().getSize();

                if (size > 0) {
                    total += size;
                    known = true;
                }
            }
            return known ? total : Constants.SIZE_UNKNOWN;
        } catch (IOException | RuntimeException ignored) {
            return Constants.SIZE_UNKNOWN;
        }
    }

    public static void unpack(File zipFile, @NonNull File destDir) throws IOException {
        unpack(zipFile, destDir, null);
    }

    public static void unpack(File zipFile, @NonNull File destDir, @Nullable EntryListener listener) throws IOException {
        if (!destDir.isDirectory() && !destDir.mkdirs()) {
            throw new IOException("Could not create directory: " + destDir);
        }

        String destPath = destDir.getCanonicalPath();

        try (ZipFile zip = new ZipFile(zipFile)) {
            List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> enumeration = zip.entries();

            while (enumeration.hasMoreElements()) {
                entries.add(enumeration.nextElement());
            }

            int total = 0;

            for (ZipEntry entry : entries) {
                if (!entry.isDirectory()) {
                    total++;
                }
            }

            int current = 0;
            byte[] buffer = new byte[Constants.BUFFER_SIZE];

            for (ZipEntry entry : entries) {
                File file = new File(destDir, entry.getName());

                if (!file.getCanonicalPath().startsWith(destPath + File.separator)) {
                    throw new IOException("Illegal zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new IOException("Could not create directory: " + file);
                    }
                    continue;
                }

                current++;

                if (listener != null) {
                    listener.onEntry(current, total);
                }

                File parent = file.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Could not create directory: " + parent);
                }

                try (InputStream is = zip.getInputStream(entry);
                     OutputStream os = new FileOutputStream(file)) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                }

            }
        }
    }
}
