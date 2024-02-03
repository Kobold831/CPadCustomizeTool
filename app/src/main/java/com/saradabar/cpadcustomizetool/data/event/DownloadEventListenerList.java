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

	public void downloadErrorNotify(int reqCode) {
		for (DownloadEventListener listener : listeners) listener.onDownloadError(reqCode);
	}

	public void connectionErrorNotify(int reqCode) {
		for (DownloadEventListener listener : listeners) listener.onConnectionError(reqCode);
	}
}