package com.saradabar.cpadcustomizetool.data.connection;

import android.app.Activity;
import android.os.AsyncTask;

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

public class AsyncFileDownload extends AsyncTask<String, Void, Boolean> {

	DownloadEventListenerList downloadEventListenerList;
	String url;
	int reqCode;
	File outputFile;
	FileOutputStream fileOutputStream;
	BufferedInputStream bufferedInputStream;
	int totalByte = 0, currentByte = 0;

	public AsyncFileDownload(Activity activity, String str, File file, int i) {
		url = str;
		outputFile = file;
		reqCode = i;
		downloadEventListenerList = new DownloadEventListenerList();
		downloadEventListenerList.addEventListener((DownloadEventListener) activity);
	}

	@Override
	protected Boolean doInBackground(String... str) {
		final byte[] buffer = new byte[1024];

		try {
			HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
			httpURLConnection.setReadTimeout(5000);
			httpURLConnection.setConnectTimeout(5000);
			bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(), 1024);
			fileOutputStream = new FileOutputStream(outputFile);
			totalByte = httpURLConnection.getContentLength();
		} catch (SocketTimeoutException | MalformedURLException ignored) {
			return false;
		} catch (IOException ignored) {
			return null;
		}

		if (isCancelled()) {
			return false;
		}

		try {
			int len;

			while ((len = bufferedInputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, len);
				currentByte += len;

				if (isCancelled()) break;
			}
		} catch (IOException ignored) {
			return false;
		}

		try {
			close();
		} catch (IOException ignored) {
		}

		return true;
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result != null) {
			if (result) downloadEventListenerList.downloadCompleteNotify(reqCode);
			else downloadEventListenerList.downloadErrorNotify();
		} else downloadEventListenerList.connectionErrorNotify();
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
	}

	private void close() throws IOException {
		fileOutputStream.flush();
		fileOutputStream.close();
		bufferedInputStream.close();
	}

	public int getLoadedBytePercent() {
		if (totalByte <= 0) return 0;
		return (int) Math.floor(100 * currentByte / totalByte);
	}
}