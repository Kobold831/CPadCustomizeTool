# CPad Customize Tool

このアプリケーションはチャレンジパッドの設定を変更することができます。  

## 機能

- システムUI・ナビゲーションバーの設定変更
- システム設定の変更
- 解像度の変更
- 内臓ブラウザ
  - WebViewを使用したブラウザを起動します。  
    セキュリティ保護のためJavaScriptによるファイルアクセス機能・非SSL接続は拒否しています。
- 学習両立モード
  - 緊急モード  
    デバイスを瞬時に学習環境に変更します。
  - 通常モード  
    デバイスを瞬時に通常環境に変更します。
- サイレントインストール
  - 対応した拡張子(.apk・.xapk・.apkm)のみ対応しています。  
    DchaServiceまたはデバイスオーナーでインストールします。
- アンインストールブロッカー
  - インストールされているアプリケーションがDchaServiceにアンインストールされないようにします。  
    学習環境と両立している場合は設定をおすすめします。

## 対応機種

- チャレンジパッド ２シリーズ

- チャレンジパッド ３

- チャレンジパッド NEO

- チャレンジパッド NEXT

## 動作要件

このアプリの動作には以下が必要です。

- DchaService
  - DchaServiceを使用しない場合でもこのアプリを使用できますが、一部の機能は制限されます。

## サンプル画像

<a href="#"><img src="images/image-02.png" height="400"></a> <a href="#"><img src="images/image-01.png" height="400"></a>

## 権限付与

一部の機能を使用するには `WRITE_SECURE_SETTINGS` の権限が必要です。

付与されない場合でも他の機能は問題なく機能します。

アプリにセキュア設定の変更を許可する場合は以下のコマンドをADBで実行してください。

```
adb shell pm grant com.saradabar.cpadcustomizetool android.permission.WRITE_SECURE_SETTINGS
```

## デバイスオーナー

アンインストールブロッカーなどの一部の機能を使用するにはデバイスオーナーを設定する必要があります。

デバイスオーナーは[Dhizuku](https://github.com/iamr0s/Dhizuku)に設定することを推奨します。

> [!WARNING]
> デバイスオーナーの設定をするときはデバイスにアカウントが１つも存在しないか確認してください。  
> Google アカウント等が追加されていると設定できません。

Dhizukuに設定する場合は以下のコマンドをADBで実行してください。

```
adb shell dpm set-device-owner com.rosan.dhizuku/.server.DhizukuDAReceiver
```

> [!NOTE]
> 通常、CPadCustomizeTool にデバイスオーナーを設定する必要はありません。

このアプリに設定する場合は以下のコマンドをADBで実行してください。

```
adb shell dpm set-device-owner com.saradabar.cpadcustomizetool/.Receiver.AdministratorReceiver
```

## 問題の報告

新たなバグや修正方法を見つけた場合は、お手数ですが [報告](https://github.com/Kobold831/CPadCustomizeTool/issues/new/choose) をお願いします。

## 外部ライブラリー

このアプリは以下のライブラリーを使用しています。

- [welcome-android](https://github.com/stephentuso/welcome-android)
  
  Copyright © 2015-2017 Stephen Tuso

- [ZeroTurnaround ZIP Library](https://github.com/zeroturnaround/zt-zip)
  
  Copyright © 2012 ZeroTurnaround LLC.

- [Dhizuku-API](https://github.com/iamr0s/Dhizuku-API)
  
  Copyright © 2023 R0S
