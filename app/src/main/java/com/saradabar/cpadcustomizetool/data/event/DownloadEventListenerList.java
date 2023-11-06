package com.saradabar.cpadcustomizetool.data.event;

import java.util.HashSet;
import java.util.Set;

public class DownloadEventListenerList {

	private final Set<DownloadEventListener> listeners = new HashSet<>();

	public void addEventListener(DownloadEventListener l) {
		listeners.add(l);
	}

	public void downloadCompleteNotify() {
		for (DownloadEventListener listener : listeners) listener.onDownloadComplete();
	}

	public void updateAvailableNotify(String str) {
		for (DownloadEventListener listener : listeners) listener.onUpdateAvailable(str);
	}

	public void updateUnavailableNotify() {
		for (DownloadEventListener listener : listeners) listener.onUpdateUnavailable();
	}

	public void updateAvailableNotify1(String str) {
		for (DownloadEventListener listener : listeners) listener.onUpdateAvailable1(str);
	}

	public void updateUnavailableNotify1() {
		for (DownloadEventListener listener : listeners) listener.onUpdateUnavailable1();
	}

	public void downloadErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onDownloadError();
	}

	public void supportAvailableNotify() {
		for (DownloadEventListener listener : listeners) listener.onSupportAvailable();
	}

	public void supportUnavailableNotify() {
		for (DownloadEventListener listener : listeners) listener.onSupportUnavailable();
	}

	public void connectionErrorNotify() {
		for (DownloadEventListener listener : listeners) listener.onConnectionError();
	}
}