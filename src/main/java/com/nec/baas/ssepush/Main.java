package com.nec.baas.ssepush;

import com.nec.baas.core.NbErrorInfo;
import com.nec.baas.core.NbService;
import com.nec.baas.core.NbServiceBuilder;
import com.nec.baas.generic.NbGenericServiceBuilder;
import com.nec.baas.json.NbJSONObject;
import com.nec.baas.push.NbSsePushInstallation;
import com.nec.baas.push.NbSsePushInstallationCallback;
import com.nec.baas.push.NbSsePushReceiveCallback;
import com.nec.baas.push.NbSsePushReceiveClient;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Main
 */
public class Main {
    // SDK ハンドル
    private NbService fService;

    private final Logger logger = Logger.getLogger("Main");

    public static void main(String[] args) throws Exception {
        new Main().start();
    }

    private Main() {
    }

    private void start() throws Exception {
        NbService.enableMultiTenant(false);

        // モバイルバックエンド基盤 SDK の初期化
        NbServiceBuilder builder = new NbGenericServiceBuilder()
                .tenantId(Config.TENANT_ID)
                .appId(Config.APP_ID)
                .appKey(Config.APP_KEY)
                .endPointUri(Config.ENDPOINT_URI);

        if (Config.PROXY_HOST != null) {
            NbService.setProxy(Config.PROXY_HOST, Config.PROXY_PORT);

            // 以下は SSE Push Client 用の設定
            System.setProperty("http.proxyHost", Config.PROXY_HOST);
            System.setProperty("http.proxyPort", Integer.toString(Config.PROXY_PORT));
            System.setProperty("https.proxyHost", Config.PROXY_HOST);
            System.setProperty("https.proxyPort", Integer.toString(Config.PROXY_PORT));
        }
        fService = builder.build();

        // インスタレーションの登録
        registerInstallation();

        // wait
        while (true) {
            try {
                Thread.sleep(1000000L);
            } catch (InterruptedException e) {
                // just ignore
            }
        }
    }

    /**
     * インスタレーションを登録する
     */
    private void registerInstallation() throws Exception {
        logger.info("Start registration");

        // インスタレーションの更新ロックを取得
        NbSsePushReceiveClient.acquireLock();

        // インスタレーションを取得する
        NbSsePushInstallation installation = NbSsePushInstallation.getCurrentInstallation();

        // チャネル設定
        Set<String> channels = new HashSet<>();
        channels.add("channel1");
        installation.setChannels(channels);

        // 許可送信者設定
        Set<String> allowedSenders = new HashSet<>();
        allowedSenders.add("g:anonymous");
        installation.setAllowedSenders(allowedSenders);

        // インスタレーションをサーバに登録する
        installation.save(new NbSsePushInstallationCallback() {
            @Override
            public void onSuccess(NbSsePushInstallation installation) {
                // インスタレーションの更新ロックを解放
                NbSsePushReceiveClient.releaseLock();

                // プッシュの待受を開始する
                startPolling(); // この処理の実装については、後述する。
            }

            @Override
            public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                // インスタレーションの更新ロックを解放
                NbSsePushReceiveClient.releaseLock();
                logger.severe(String.format("Registration failed: status=%d error=%s", statusCode, errorInfo));
            }
        });
    }

    /**
     * 受信処理
     */
    private void startPolling() {
        logger.info("Start waiting push message...");

        NbSsePushReceiveClient client = new NbSsePushReceiveClient();

        // ハートビート間隔の設定
        client.setHeartbeatInterval(Config.HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        // イベントタイプ設定
        Set<String> events = new HashSet<>();
        events.add("message");

        // 接続開始
        client.connect(events, new NbSsePushReceiveCallback() {
            @Override
            public void onConnect() {
                logger.info("onConnect");
            }

            @Override
            public void onDisconnect() {
                logger.info("onDisconnect");
            }

            @Override
            public void onMessage(NbJSONObject message) {
                // プッシュされたメッセージを処理する
                System.out.println(message.toString());
            }

            @Override
            public void onError(int statusCode, NbErrorInfo errorInfo) {
                // エラー時の処理
                logger.warning(String.format("onError: status=%d, error=%s", statusCode, errorInfo));
            }

            @Override
            public void onHeartbeatLost() {
                // ハートビート喪失検出時の処理
                client.disconnect();
                startPolling();
            }
        });
    }
}
