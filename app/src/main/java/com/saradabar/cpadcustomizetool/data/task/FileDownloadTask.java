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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FileDownloadTask {

	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
	private static final int BUFFER_SIZE = 8192;
	private static final int CONNECT_TIMEOUT = 15000;
	private static final int READ_TIMEOUT = 30000;

	private static final Map<String, String> ETAG_MAP = new ConcurrentHashMap<>();

	private static volatile boolean sslInitialized = false;

	DownloadEventListenerList downloadEventListenerList;
	volatile int totalByte = 0, currentByte = 0;
	private volatile boolean finished = false;

	public void execute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode) {
		onPreExecute(downloadEventListener);
		Handler handler = new Handler(Looper.getMainLooper());
		EXECUTOR.execute(() -> {
			Boolean result = doInBackground(downloadUrl, outputFile, false);
			handler.post(() -> onPostExecute(result, reqCode));
		});
	}

	private void onPreExecute(DownloadEventListener downloadEventListener) {
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener(downloadEventListener);
	}

	private void onPostExecute(Boolean result, int reqCode) {
		finished = true;
		totalByte = -1;

		if (result != null) {
			if (result) {
				downloadEventListenerList.downloadCompleteNotify(reqCode);
			} else {
				downloadEventListenerList.downloadErrorNotify(reqCode);
			}
		} else {
			downloadEventListenerList.connectionErrorNotify(reqCode);
		}
	}

	@Nullable
	private Boolean doInBackground(String downloadUrl, File outputFile, boolean withoutCache) {
		HttpURLConnection httpURLConnection = null;
		File tempFile = new File(outputFile.getPath() + ".tmp");

		try {
			initSslIfNeeded();

			httpURLConnection = (HttpURLConnection) new URL(downloadUrl).openConnection();
			httpURLConnection.setReadTimeout(READ_TIMEOUT);
			httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);

			if (!withoutCache) {
				String etag = ETAG_MAP.get(downloadUrl);
				if (etag != null) {
					httpURLConnection.setRequestProperty("If-None-Match", etag);
				}
			}

			int responseCode = httpURLConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (outputFile.exists()) {
					return true;
				}
				ETAG_MAP.remove(downloadUrl);
				return doInBackground(downloadUrl, outputFile, true);
			}

			if (responseCode != HttpURLConnection.HTTP_OK) {
				return false;
			}

			String etag = httpURLConnection.getHeaderField("ETag");
			totalByte = httpURLConnection.getContentLength();

			try (InputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), BUFFER_SIZE);
			     FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
				byte[] buffer = new byte[BUFFER_SIZE];
				int len;

				while ((len = bufferedInputStream.read(buffer)) != -1) {
					fileOutputStream.write(buffer, 0, len);
					currentByte += len;
				}
				fileOutputStream.flush();
			}

			if (outputFile.exists() && !outputFile.delete()) {
				return false;
			}
			if (!tempFile.renameTo(outputFile)) {
				return false;
			}

			if (etag != null) {
				ETAG_MAP.put(downloadUrl, etag);
			} else {
				ETAG_MAP.remove(downloadUrl);
			}
			return true;
		} catch (SocketTimeoutException | MalformedURLException | NoSuchAlgorithmException |
                 KeyManagementException ignored) {
			return false;
		} catch (IOException ignored) {
			return null;
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
	}

	private static synchronized void initSslIfNeeded() throws NoSuchAlgorithmException, KeyManagementException {
		if (sslInitialized) {
			return;
		}

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
		HostnameVerifier hostnameVerifier = (hostname, session) -> true;
		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		sslInitialized = true;
	}

	public int getLoadedBytePercent() {
		if (finished) return 100;
		if (totalByte <= 0) return 0;
		return (int) Math.min(99, (currentByte * 100L) / totalByte);
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
		return finished;
	}
}
