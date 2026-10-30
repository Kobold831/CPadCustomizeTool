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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {

    private static final int THREAD_COUNT = 3;

    private static final ExecutorService IO =
            Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactory() {
                private final AtomicInteger index = new AtomicInteger();

                @Override
                public Thread newThread(@NonNull Runnable runnable) {
                    Thread thread = new Thread(runnable, "cct-io-" + index.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static void io(@NonNull Runnable runnable) {
        IO.execute(runnable);
    }

    public static void main(@NonNull Runnable runnable) {
        MAIN.post(runnable);
    }
}
