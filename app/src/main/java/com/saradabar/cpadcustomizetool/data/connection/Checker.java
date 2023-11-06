package com.saradabar.cpadcustomizetool.data.connection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Checker {

    int supportCode;
    String url;
    DownloadEventListenerList downloadEventListenerList;

    public Checker(Activity activity, String str) {
        url = str;
        downloadEventListenerList = new DownloadEventListenerList();

        downloadEventListenerList.addEventListener((DownloadEventListener) activity);
    }

    private int supportAvailableCheck() {
        try {
            getSupportInfo();

            if (supportCode == 1) {
                return 1;
            } else {
                if (supportCode == 0) {
                    return 0;
                } else return -1;
            }
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void getSupportInfo() {
        HashMap<String, String> map = parseSupportXml(url);

        if (map != null) {
            supportCode = Integer.parseInt(Objects.requireNonNull(map.get("supportCode")));
        } else {
            supportCode = -99;
        }
    }

    public void supportCheck() {
        new Checker.supportCheckTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class supportCheckTask extends AsyncTask<Object, Object, Integer> {

        @Override
        protected Integer doInBackground(Object... arg0) {
            return supportAvailableCheck();
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case -1:
                    downloadEventListenerList.connectionErrorNotify();
                    break;
                case 0:
                    downloadEventListenerList.supportUnavailableNotify();
                    break;
                case 1:
                    downloadEventListenerList.supportAvailableNotify();
                    break;
            }
        }
    }

    private HashMap<String, String> parseSupportXml(String url) {
        HashMap<String, String> map = new HashMap<>();
        HttpURLConnection httpURLConnection;

        try {
            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setConnectTimeout(5000);
            InputStream is = httpURLConnection.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(bis);
            Element root = document.getDocumentElement();

            if (root.getTagName().equals("support")) {
                NodeList nodelist = root.getChildNodes();

                for (int j = 0; j < nodelist.getLength(); j++) {
                    Node node = nodelist.item(j);

                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String tagName = element.getTagName();
                        String textContent = element.getTextContent().trim();

                        map.put(tagName, textContent);
                    }
                }
            }

            return map;
        } catch (IOException | SAXException | ParserConfigurationException ignored) {
            return null;
        }
    }
}