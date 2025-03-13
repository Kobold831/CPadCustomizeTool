package com.saradabar.cpadcustomizetool.view.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.view.views.NoticeListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoticeActivity extends AppCompatActivity implements DownloadEventListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        new FileDownloadTask().execute(this, Constants.URL_NOTICE, new File(getExternalCacheDir(), "ct-notice.json"), Constants.REQUEST_DOWNLOAD_NOTICE);
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_NOTICE) {
            try {
                List<NoticeListView.AppData> appDataList = new ArrayList<>();
                ListView listView = findViewById(R.id.list_notice);

                JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), "ct-notice.json"));
                JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                JSONArray jsonArray = jsonObj2.getJSONArray("noticeList");

                if (jsonArray.length() == 0) {
                    new AlertDialog.Builder(this)
                            .setMessage("アプリのお知らせはありません。")
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
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    try {
                        String url = jsonArray.getJSONObject(position).getString("url");
                        if (!url.isEmpty()) {
                            startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", url).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        }
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }

    /* メニュー選択 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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
