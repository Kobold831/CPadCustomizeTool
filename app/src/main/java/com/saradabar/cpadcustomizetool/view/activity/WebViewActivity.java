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

package com.saradabar.cpadcustomizetool.view.activity;

import android.app.DownloadManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.DialogUtil;

import java.util.Objects;

/** @noinspection deprecation*/
public class WebViewActivity extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initWebView();
        } catch (Exception ignored) {
            new DialogUtil(this)
                    .setMessage("\"WebView\" の起動に問題が発生しました。\nデバイスの更新、または \"WebView\" の更新をお試しください。")
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
        }
    }

    private void initWebView() {
        setContentView(R.layout.activity_web_view);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            finish();
            overridePendingTransition(0, 0);
        });
        webView = findViewById(R.id.activity_web_view);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("読み込みしています");
                    getSupportActionBar().setSubtitle(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(String.valueOf(view.getTitle()));
                    getSupportActionBar().setSubtitle(url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isUrlDistrusted(url)) {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("このサイトにアクセスできません");
                        getSupportActionBar().setSubtitle("このウェブページは、HTTP 接続のため表示できません");
                    }
                }
                return isUrlDistrusted(url);
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Toast.makeText(this, "ダウンロードを開始します。", Toast.LENGTH_SHORT).show();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                    .allowScanningByMediaScanner();
            DownloadManager downloadManager = (DownloadManager) getSystemService(android.content.Context.DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setAllowFileAccess(false);

        if (getIntent().getStringExtra("URL") != null) {
            webView.loadUrl(Objects.requireNonNull(getIntent().getStringExtra("URL")));
        } else {
            webView.loadUrl("https://www.google.co.jp/intl/ja/");
        }
    }

    @Override
    public void onBackPressed() {
        webView.goBack();
    }

    private boolean isUrlDistrusted(String url) {
        try {
            return !Objects.equals(Uri.parse(url).getScheme(), "https");
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void applyOverrideConfiguration(final Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT < 25) {
            overrideConfiguration.uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
}
