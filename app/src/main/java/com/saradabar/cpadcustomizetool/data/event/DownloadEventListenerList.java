package com.saradabar.cpadcustomizetool.data.event;

import java.util.HashSet;
import java.util.Set;

public class DownloadEventListenerList {

	private final Set<DownloadEventListener> listeners = new HashSet<>();

	public void addEventListener(DownloadEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify(int reqCode) {
		for (DownloadEventListener listener : listeners) listener.onDownloadComplete(reqCode);
	}

	public void downloadErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onDownloadError();
	}

	public void connectionErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onConnectionError();
	}
}