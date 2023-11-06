package com.saradabar.cpadcustomizetool.data.event;

import java.util.EventListener;

public interface DownloadEventListener extends EventListener {

	void onDownloadComplete();
	void onUpdateAvailable(String str);
	void onUpdateUnavailable();
	void onSupportAvailable();
	void onSupportUnavailable();
	void onUpdateAvailable1(String str);
	void onUpdateUnavailable1();
	void onDownloadError();
	void onConnectionError();
}