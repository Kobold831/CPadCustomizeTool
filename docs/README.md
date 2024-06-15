# CPad Customize Tool

このアプリケーションはチャレンジパッドの設定を変更することができます。

## 初期状態からのインストール
[SetupLogin](https://kobold831.github.io/SetupLogin/)から簡単に利用できるようになりました。

+ [SetupLogin](https://kobold831.github.io/SetupLogin/)にあるとおりにセットアップをしてください。
+ チャレンジパッド総合カスタマイズツールを選択して続行します。
+ このアプリが起動します。

## 機能

- システムUI・ナビゲーションバーの設定変更
- システム設定の変更
- 解像度の変更（この機能は削除する予定です）
- 内臓ブラウザ（この機能は削除する予定です）
  - WebViewを使用したブラウザを起動します。  
    セキュリティ保護のためJavaScriptによるファイルアクセス機能・非SSL接続は拒否しています。
- 学習両立モード
  - 緊急モード  
    デバイスを瞬時に学習環境に変更します。
  - 通常モード  
    デバイスを瞬時に通常環境に変更します。
- サイレントインストール（この機能は削除する予定です）
  - 対応した拡張子(.apk・.xapk・.apkm)のみ対応しています。  
    DchaServiceまたはデバイスオーナーでインストールします。
- アンインストールブロッカー
  - インストールされているアプリケーションがDchaServiceにアンインストールされないようにします。  
    学習環境と両立している場合は設定をおすすめします。

## 対応機種

- チャレンジパッド２シリーズ
- チャレンジパッド３
- チャレンジパッドNeo
- チャレンジパッドNext

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

## 既知の問題

### Dhizukuで任意のapkをサイレントインストールしようとするとSecurityExceptionがスローされる

- 修正状況
  - このアプリに起因する問題でないため修正はありません。

- 症状
  - 以下のクラッシュ（ **Caller has no access to session** ）が発生する。
    <a href="#"><img src="images/screenshot-01.png" width="640" height="400"></a>
- 原因と対処法
  - Dhizukuに権限が付与されていない可能性があります。
  - Dhizukuに権限（ストレージなど）が付与されているかを確認してください。

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
