ssepush-sample-java: Java SSE Push 受信サンプル
===============================================

Java SDK SSE Push機能の、インスタレーション登録と SSE Push 受信を
確認するサンプルアプリです。

本サンプルプログラムは Java コマンドラインアプリケーションです。

実行方法
---------

Config.java.sample を Config.java にリネームして下記項目を設定してください。

* TENANT_ID
* APP_ID
* APP_KEY
* ENDPOINT_URI

Proxy が必要な場合は PROXY_HOST, PROXY_PORT も設定してください。

プログラムの起動は以下のようにして実施してください。

    $ ./gradlew run

Push を受信するたびに、結果が標準出力に出力されます。

Push 送信はデベロッパーコンソールから行ってください。

* 本プログラムは "channel1" チャネルを subscribe していますので、チャネル指定はこの値を指定してください。
* 「SSE固有」の「イベントタイプ」には "message" を入力してください。
