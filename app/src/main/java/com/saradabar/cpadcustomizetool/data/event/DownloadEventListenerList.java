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

	public void progressUpdate(int progress, int currentByte, int totalByte) {
		for (DownloadEventListener listener : listeners) listener.onProgressUpdate(progress, currentByte, totalByte);
	}
}