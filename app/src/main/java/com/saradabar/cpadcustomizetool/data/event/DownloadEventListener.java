package com.saradabar.cpadcustomizetool.data.event;

import java.util.EventListener;

public interface DownloadEventListener extends EventListener {

	void onDownloadComplete(int reqCode);
	void onDownloadError(int reqCode);
	void onConnectionError(int reqCode);
}