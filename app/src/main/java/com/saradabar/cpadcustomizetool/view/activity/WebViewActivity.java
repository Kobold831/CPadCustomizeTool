package com.saradabar.cpadcustomizetool.view.activity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.saradabar.cpadcustomizetool.R;

import java.util.Objects;

public class WebViewActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        WebView webView = findViewById(R.id.activity_web_view);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("読込中");
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
                        getSupportActionBar().setTitle("接続拒否");
                        getSupportActionBar().setSubtitle("アクセスしようとしたサイトは非SSL接続のため拒否しました");
                    }
                }
                return isUrlDistrusted(url);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setAllowFileAccess(false);

        if (getIntent().getStringExtra("URL") != null) {
            webView.loadUrl(Objects.requireNonNull(getIntent().getStringExtra("URL")));
        } else {
            webView.loadUrl("https://www.google.com");
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.goBack();
            }
        });
    }

    private boolean isUrlDistrusted(String url) {
        try {
            return !Objects.equals(Uri.parse(url).getScheme(), "https");
        } catch (Exception ignored) {
        }
        return true;
    }
}
