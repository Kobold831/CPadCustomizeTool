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

import androidx.annotation.Nullable;

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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FileDownloadTask {

	DownloadEventListenerList downloadEventListenerList;
	int totalByte = 0, currentByte = 0;

	public void execute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode) {
		onPreExecute(downloadEventListener);
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(() -> {
			Handler handler = new Handler(Looper.getMainLooper());
			new Thread(() -> {
				Boolean result = doInBackground(downloadUrl, outputFile);
				handler.post(() -> onPostExecute(result, reqCode));
			}).start();
		});
	}

	private void onPreExecute(DownloadEventListener downloadEventListener) {
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener(downloadEventListener);
	}

	private void onPostExecute(Boolean result, int reqCode) {
		if (result != null) {
			if (result) {
				totalByte = -1;
				downloadEventListenerList.downloadCompleteNotify(reqCode);
			} else {
				totalByte = -1;
				downloadEventListenerList.downloadErrorNotify(reqCode);
			}
		} else {
			totalByte = -1;
			downloadEventListenerList.connectionErrorNotify(reqCode);
		}
	}

	@Nullable
	private Boolean doInBackground(String downloadUrl, File outputFile) {
		BufferedInputStream bufferedInputStream;
		FileOutputStream fileOutputStream;
		byte[] buffer = new byte[1024];

		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
						public void checkClientTrusted(X509Certificate[] certs, String authType) {}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {}
					}
			};
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

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
		} catch (NoSuchAlgorithmException | KeyManagementException ignored) {
			return false;
		}

		try {
			int len;

			while ((len = bufferedInputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, len);
				currentByte += len;
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

	public boolean isFinish() {
		return totalByte == -1;
	}
}
