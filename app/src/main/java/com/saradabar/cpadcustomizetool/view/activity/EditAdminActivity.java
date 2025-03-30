package com.saradabar.cpadcustomizetool.view.activity;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class EditAdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            document.createComment("");
            Element element = document.createElement("root");
            document.appendChild(element);
            Element element1 = document.createElement("device-owner");
            element1.setAttribute("package", "com.rosan.dhizuku");
            element1.setAttribute("name", "");
            element1.setAttribute("component", "com.rosan.dhizuku/com.rosan.dhizuku.server.DhizukuDAReceiver");
            element1.setAttribute("userRestrictionsMigrated", "true");
            element.appendChild(element1);
            Element element2 = document.createElement("device-owner-context");
            element2.setAttribute("userId", "0");
            element.appendChild(element2);
            writeXML(new File("/sdcard/device_owner_02.xml"), document);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("エラーが発生しました")
                    .setMessage(e.getMessage())
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("成功")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void writeXML(File file, Document document) throws Exception {
        Transformer tf;
        tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty("indent", "yes");
        tf.setOutputProperty("encoding", "UTF-8");
        tf.setOutputProperty("standalone", "yes");
        tf.transform(new DOMSource(document), new StreamResult(file));
    }
}
