/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
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

import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListenerList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileDownloadTask {

	DownloadEventListenerList downloadEventListenerList;
	String downloadUrl;
	int reqCode = 0, totalByte = 0, currentByte = 0;
	File outputFile;

	private class AsyncRunnable implements Runnable {

		Handler handler = new Handler(Looper.getMainLooper());

		@Override
		public void run() {
			Boolean result = doInBackground();
			handler.post(() -> onPostExecute(result));
		}
	}

	public void execute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode) {
		onPreExecute(downloadEventListener, downloadUrl, outputFile, reqCode);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(new AsyncRunnable());
	}

	void onPreExecute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode) {
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener(downloadEventListener);
		this.downloadUrl = downloadUrl;
		this.outputFile = outputFile;
		this.reqCode = reqCode;
	}

	void onPostExecute(Boolean result) {
		if (result != null) {
			if (result) downloadEventListenerList.downloadCompleteNotify(reqCode);
			else downloadEventListenerList.downloadErrorNotify(reqCode);
		} else downloadEventListenerList.connectionErrorNotify(reqCode);
	}

	protected Boolean doInBackground() {
		BufferedInputStream bufferedInputStream;
		FileOutputStream fileOutputStream;
		byte[] buffer = new byte[1024];

		try {
			HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(downloadUrl).openConnection();
			httpURLConnection.setReadTimeout(20000);
			httpURLConnection.setConnectTimeout(20000);
			bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), 1024);
			fileOutputStream = new FileOutputStream(outputFile);
			totalByte = httpURLConnection.getContentLength();
		} catch (SocketTimeoutException | MalformedURLException ignored) {
			return false;
		} catch (IOException ignored) {
			return null;
		}

		try {
			while (bufferedInputStream.read(buffer) != -1) {
				fileOutputStream.write(buffer, 0, bufferedInputStream.read(buffer));
				currentByte += bufferedInputStream.read(buffer);
			}
		} catch (IOException ignored) {
			return false;
		}

		try {
			fileOutputStream.flush();
			fileOutputStream.close();
			bufferedInputStream.close();
		} catch (IOException ignored) {
		}

		return true;
	}

	public int getLoadedBytePercent() {
		if (totalByte <= 0) return 0;
		return (int) Math.floor(((double) getLoadedCurrentByte() / getLoadedTotalByte()) * 100);
	}

	public int getLoadedCurrentByte() {
		if (totalByte <= 0) return 0;
		return currentByte / (1024 * 1024);
	}

	public int getLoadedTotalByte() {
		return totalByte / (1024 * 1024);
	}

	public void onProgressUpdate(int progress) {
		downloadEventListenerList.progressUpdate(progress, getLoadedCurrentByte(), getLoadedTotalByte());
	}
}
