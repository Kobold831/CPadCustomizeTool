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

import java.util.EventListener;

public interface DownloadEventListener extends EventListener {
	void onDownloadComplete(int reqCode);

	void onDownloadError(int reqCode);

	void onConnectionError(int reqCode);

	void onProgressUpdate(int progress, int currentByte, int totalByte);
}
