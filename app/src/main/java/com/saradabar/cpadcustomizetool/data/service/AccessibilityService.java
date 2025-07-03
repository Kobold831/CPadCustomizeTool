package com.saradabar.cpadcustomizetool.data.service;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity;
import com.saradabar.cpadcustomizetool.view.activity.NormalActivity;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    private static boolean isHomeButtonPressed = false;
    private static boolean isVolumeDownPressed = false;
    private static boolean isVolumeUpPressed = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                isHomeButtonPressed = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                isHomeButtonPressed = false;
            }
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                isVolumeDownPressed = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                isVolumeDownPressed = false;
            }
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                isVolumeUpPressed = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                isVolumeUpPressed = false;
            }
        }

        if (isHomeButtonPressed && isVolumeDownPressed &&
                !Preferences.load(this, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            // ホームボタンが押されたかつボリュームダウンが押されたかつ通常環境モードではない
            startActivity(new Intent(this, NormalActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }

        if (isHomeButtonPressed && isVolumeUpPressed &&
                !Preferences.load(this, Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            // ホームボタンが押されたかつボリュームダウンが押されたかつ通常環境モードではない
            startActivity(new Intent(this, EmergencyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return super.onKeyEvent(event);
    }
}
