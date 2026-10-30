package com.saradabar.cpadcustomizetool.view.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.view.views.NoticeListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NoticeActivity extends AppCompatActivity implements DownloadEventListener {

    private FileDownloadTask fileDownloadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        fileDownloadTask = new FileDownloadTask();
        fileDownloadTask.execute(this, Constants.URL_CORE, new File(getExternalCacheDir(), Constants.CORE_JSON), Constants.REQUEST_DOWNLOAD_NOTICE, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileDownloadTask != null) {
            fileDownloadTask.removeEventListener(this);
        }
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (reqCode == Constants.REQUEST_DOWNLOAD_NOTICE) {
            try {
                List<NoticeListView.AppData> appDataList = new ArrayList<>();
                ListView listView = findViewById(R.id.list_notice);

                JSONArray jsonArray = Common.getNotices(new File(getExternalCacheDir(), Constants.CORE_JSON));

                if (jsonArray.length() == 0) {
                    new DialogUtil(this)
                            .setMessage(R.string.dialog_no_notice)
                            .setPositiveButton(getString(R.string.dialog_common_ok), (dialog, which) -> finish())
                            .show();
                    return;
                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    NoticeListView.AppData appData = new NoticeListView.AppData();
                    appData.title = jsonArray.getJSONObject(i).getString("title");
                    appData.message = jsonArray.getJSONObject(i).getString("message");
                    appDataList.add(appData);
                }
                NoticeListView.AppListAdapter appListAdapter = new NoticeListView.AppListAdapter(this, appDataList);
                listView.setAdapter(appListAdapter);
                appListAdapter.setOnItemClickListener((view, position) -> {
                    try {
                        String url = jsonArray.getJSONObject(position).getString("url");
                        if (!url.isEmpty()) {
                            startActivity(new Intent(this, WebViewActivity.class).putExtra(Constants.URI_EXTRA_NAME, url).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        }
                    } catch (JSONException ignored) {
                    }
                });
            } catch (JSONException | IOException ignored) {
            }
        }
    }

    /* メニュー選択 */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDownloadError(int reqCode) {
    }

    @Override
    public void onConnectionError(int reqCode) {
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
    }
}
