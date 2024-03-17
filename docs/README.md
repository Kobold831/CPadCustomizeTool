# CPad Customize Tool

このアプリケーションはチャレンジパッドの設定を変更することができます。  
様々な機能を提供します。

## 権限の付与

ADBの有効状態を保持
```
adb shell pm grant com.saradabar.cpadcustomizetool android.permission.WRITE_SECURE_SETTINGS
```

デバイスオーナー権限の付与
```
adb shell dpm set-device-owner com.saradabar.cpadcustomizetool/.Receiver.AdministratorReceiver
```
