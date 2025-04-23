# CPad Customize Tool

このアプリケーションはチャレンジパッドの設定を変更することができます。

学習兼用環境での改造サポートを提供します。

## 初期状態からのインストール

SDカード から簡単に利用できるようになりました。

+ [**SetupLogin**](https://github.com/Kobold831/SetupLogin/blob/master/docs/README.md) を参考にセットアップしてください。
+ アプリ一覧が表示されたら、\[**チャレンジパッド総合カスタマイズツール**\] を選択して続行します。

## 機能

標準機能一覧です。  
その他、別機能を要求するものに関しては、**動作推奨要件** をご覧ください。

- **システムUI / ナビゲーションバー** の設定変更
- システム設定の変更
- 内臓ブラウザ
  - WebView を使用したブラウザを起動します。  
    セキュリティ保護のため、HTTP(非SSL)接続、ファイルアクセス機能は使用できません。
- 学習両立モード  
  デバイスを学習モードまたは通常モードに瞬時に切り替えます

## 対応機種

- チャレンジパッド２
  - TAB-A03-BS
  - TAB-A03-BR
  - TAB-A03-BR2
  - TAB-A03-BR2B
- チャレンジパッド３
  - TAB-A04-BR3
- チャレンジパッドNeo
  - TAB-A05-BD
- チャレンジパッドNext
  - TAB-A05-BA1

## 動作推奨要件

- **DchaService**
  - 単一APKのサイレントインストール
  - 既定のランチャーの切り替え
  - 再起動のショートカット
  - ファームウェアアップデート  
    ファイルは各自で用意する必要あり
- **DchaUtilService**
  - 解像度の変更
- 端末所有者 (デバイスオーナー)
  - アンインストールブロック
  - サイレントインストール
  - アプリの権限自動昇格

## サンプル画像

<a href="#"><img src="images/image-02.png" height="400"></a><a href="#"><img src="images/image-01.png" height="400"></a>

## 権限付与

ADB 及び 開発者向けオプション の状態を保持する機能を使用するには、  
`WRITE_SECURE_SETTINGS` の権限が必要です。

アプリで該当の設定の有効にするには、  
以下のコマンドをADBで実行してください：

```
adb shell pm grant com.saradabar.cpadcustomizetool android.permission.WRITE_SECURE_SETTINGS
```

## デバイスオーナー

アンインストールブロッカーなどの一部の機能を使用するにはデバイスオーナーを設定する必要があります。

デバイスオーナーには [**Dhizuku**](https://github.com/iamr0s/Dhizuku) が推奨されています。

> [!WARNING]
> デバイスオーナーの設定をする際にデバイスにアカウントが１つも存在しないか確認してください。  
> Google アカウント等が追加されていると設定できません。

**Dhizuku** に設定する場合は以下のコマンドをADBで実行してください：

```
adb shell dpm set-device-owner com.rosan.dhizuku/.server.DhizukuDAReceiver
```

> [!NOTE]
> 通常、CPadCustomizeTool にデバイスオーナーを設定する必要はありません。

Dhizuku を何らかの理由で設定できない場合は、  
このアプリに設定する以下のコマンドをADBで実行してください：

```
adb shell dpm set-device-owner com.saradabar.cpadcustomizetool/.data.receiver.DeviceAdminReceiver
```

> [!TIP]
> デバイスオーナーは複数設定することはできません

> [!NOTE]
> アプリ内の機能からデバイスオーナーを解除することができない場合は、以下のいずれかをお試しください。
> 
> ・デバイスの初期化。
> 
> ・以下のコマンドを ADB で**特権実行**。
> 
> ```
> adb shell rm /data/system/device_owner_2.xml
> ```

## 問題の報告

新たなバグや修正方法を見つけた場合は、お手数ですが [報告](https://github.com/Kobold831/CPadCustomizeTool/issues/new/choose) をお願いします。

GitHub アカウントをお持ちでない方、または匿名を希望の場合は[**Google フォームから報告**](https://forms.gle/LnGuEc4GdRmwzf3GA)できます。

## 利用規約

以下のリンクから確認することができます。

[利用規約はこちら](https://drive.google.com/file/d/1yUfxu7CEqYikn0FN5SItc4valZOvR4Uh/view?usp=sharing)

## 外部ライブラリー

このアプリは以下のライブラリーを使用しています。

- [**welcome-android**](https://github.com/stephentuso/welcome-android)
  
  Copyright © 2015-2017 Stephen Tuso

- [**ZeroTurnaround ZIP Library**](https://github.com/zeroturnaround/zt-zip)
  
  Copyright © 2012 ZeroTurnaround LLC.

- [**Dhizuku-API**](https://github.com/iamr0s/Dhizuku-API)
  
  Copyright © 2023 R0S
