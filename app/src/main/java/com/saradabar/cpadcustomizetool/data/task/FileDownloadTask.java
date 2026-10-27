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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListenerList;
import com.saradabar.cpadcustomizetool.data.net.Ipv4Proxy;
import com.saradabar.cpadcustomizetool.util.Constants;

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

	private static final String TAG = "FileDownloadTask";

	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Constants.DOWNLOAD_THREAD_COUNT);

	private static final Map<String, String> ETAG_MAP = new ConcurrentHashMap<>();

	private static final Object SSL_LOCK = new Object();

	private static volatile boolean sslInitialized = false;

	private static volatile String lastError = "";

	DownloadEventListenerList downloadEventListenerList;
	volatile int totalByte = 0, currentByte = 0;
	private volatile boolean finished = false;

	public static String getLastError() {
		return lastError;
	}

	public void execute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode) {
		execute(downloadEventListener, downloadUrl, outputFile, reqCode, Constants.CONNECT_TIMEOUT_MS, Constants.READ_TIMEOUT_MS);
	}

	public void execute(DownloadEventListener downloadEventListener, String downloadUrl, File outputFile, int reqCode, int connectTimeoutMs, int readTimeoutMs) {
		onPreExecute(downloadEventListener);
		Handler handler = new Handler(Looper.getMainLooper());
		EXECUTOR.execute(() -> {
			Boolean result = doInBackground(connectTimeoutMs, readTimeoutMs, downloadUrl, outputFile, false);
			handler.post(() -> onPostExecute(result, reqCode));
		});
	}

	private void onPreExecute(DownloadEventListener downloadEventListener) {
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener(downloadEventListener);
		finished = false;
		totalByte = 0;
		currentByte = 0;
	}

	public void removeEventListener(DownloadEventListener downloadEventListener) {
		if (downloadEventListenerList != null) {
			downloadEventListenerList.removeEventListener(downloadEventListener);
		}
	}

	private void onPostExecute(Boolean result, int reqCode) {
		finished = true;
		totalByte = Constants.SIZE_UNKNOWN;

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
	private Boolean doInBackground(int connectTimeoutMs, int readTimeoutMs, String downloadUrl, @NonNull File outputFile, boolean withoutCache) {
		lastError = "";
		HttpURLConnection httpURLConnection = null;
		File tempFile = new File(outputFile.getPath() + "." + Thread.currentThread().getId() + "." + System.nanoTime() + ".tmp");

		try {
			if (!isNetworkAvailable()) {
				lastError = "ネットワークに接続されていません";
				return null;
			}

			initSslIfNeeded();

			httpURLConnection = (HttpURLConnection) new URL(downloadUrl).openConnection(Ipv4Proxy.get());
			httpURLConnection.setConnectTimeout(connectTimeoutMs);
			httpURLConnection.setReadTimeout(readTimeoutMs);
			httpURLConnection.setRequestMethod("GET");
			httpURLConnection.setRequestProperty("Accept-Encoding", "identity");

			if (!withoutCache) {
				String etag = ETAG_MAP.get(downloadUrl);
				if (etag != null) {
					httpURLConnection.setRequestProperty("If-None-Match", etag);
				}
			}

			int responseCode = httpURLConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (outputFile.length() > 0) {
					return true;
				}
				ETAG_MAP.remove(downloadUrl);
				return doInBackground(connectTimeoutMs, readTimeoutMs, downloadUrl, outputFile, true);
			}

			if (responseCode != HttpURLConnection.HTTP_OK) {
				lastError = "HTTP " + responseCode;
				return false;
			}

			String etag = httpURLConnection.getHeaderField("ETag");
			totalByte = httpURLConnection.getContentLength();

			try (InputStream bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), Constants.BUFFER_SIZE);
			     FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
				byte[] buffer = new byte[Constants.BUFFER_SIZE];
				int len;

				while ((len = bufferedInputStream.read(buffer)) != -1) {
					fileOutputStream.write(buffer, 0, len);
					currentByte += len;
				}
				fileOutputStream.flush();
			}

			if (totalByte > 0 && currentByte < totalByte) {
				lastError = "ダウンロードが途中で切断されました";
				return null;
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
		} catch (SocketTimeoutException e) {
			lastError = "接続がタイムアウトしました";
			Log.w(TAG, "GET timeout: " + downloadUrl, e);
			return false;
		} catch (MalformedURLException e) {
			lastError = "URL が不正です";
			Log.w(TAG, "GET malformed: " + downloadUrl, e);
			return false;
		} catch (IOException e) {
			lastError = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
			Log.w(TAG, "GET failed: " + downloadUrl, e);
			return null;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			lastError = "TLS の初期化に失敗しました";
			Log.e(TAG, "TLS init failed", e);
			return false;
		} catch (Throwable t) {
			lastError = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
			Log.e(TAG, "GET crashed: " + downloadUrl, t);
			return false;
		} finally {
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
	}

	private static boolean isNetworkAvailable() {
		Context context = MyApplication.getContext();

		if (context == null) {
			return true;
		}

		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (manager == null) {
			return true;
		}

		NetworkInfo info = manager.getActiveNetworkInfo();
		return info != null && info.isConnected();
	}

	private static void initSslIfNeeded() throws NoSuchAlgorithmException, KeyManagementException {
		if (sslInitialized) {
			return;
		}

		synchronized (SSL_LOCK) {
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
	}

	public int getLoadedBytePercent() {
		if (finished) return Constants.PERCENT_MAX;
		if (totalByte <= 0) return 0;
		return (int) Math.min(Constants.PERCENT_NOT_FINISHED, (currentByte * (long) Constants.PERCENT_MAX) / totalByte);
	}

	public int getLoadedCurrentByte() {
		if (totalByte <= 0) return 0;
		return currentByte / Constants.BYTE_TO_MB;
	}

	public int getLoadedTotalByte() {
		return totalByte / Constants.BYTE_TO_MB;
	}

	public void onProgressUpdate(int progress) {
		downloadEventListenerList.progressUpdate(progress, getLoadedCurrentByte(), getLoadedTotalByte());
	}

	public boolean isFinish() {
		return finished;
	}
}
