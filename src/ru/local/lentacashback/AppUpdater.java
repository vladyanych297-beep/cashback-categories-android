package ru.local.lentacashback;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class AppUpdater {
    static final String ACTION_INSTALL_STATUS = "ru.local.lentacashback.INSTALL_STATUS";
    private static final String RELEASE_API = "https://api.github.com/repos/vladyanych297-beep/cashback-categories-android/releases/latest";
    private static final String UPDATE_FILE = "cashback-update.apk";

    private final MainActivity activity;
    private File pendingApk;
    private boolean waitingForPermission;

    AppUpdater(MainActivity activity) { this.activity = activity; }

    void checkAtLaunch() {
        activity.showUpdateStatus("Проверяю обновления…");
        new Thread(() -> {
            try {
                JSONObject release = new JSONObject(readText(RELEASE_API));
                String tag = release.optString("tag_name").replaceFirst("^[vV]", "");
                String installed = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                if (compareVersions(tag, installed) <= 0) {
                    activity.runOnUiThread(() -> activity.showUpdateStatus("Установлена актуальная версия " + installed));
                    return;
                }
                String downloadUrl = findApk(release.optJSONArray("assets"), tag);
                if (downloadUrl.isEmpty()) throw new IllegalStateException("APK asset not found");
                activity.runOnUiThread(() -> activity.showUpdateStatus("Скачиваю обновление " + tag + "…"));
                File target = new File(activity.getCacheDir(), UPDATE_FILE);
                download(downloadUrl, target);
                pendingApk = target;
                activity.runOnUiThread(() -> requestInstallPermissionOrInstall(tag));
            } catch (Exception error) {
                activity.runOnUiThread(() -> activity.showUpdateStatus("Не удалось проверить обновления"));
            }
        }, "cashback-update-check").start();
    }

    void onResume() {
        if (!waitingForPermission || pendingApk == null || !pendingApk.isFile()) return;
        if (activity.getPackageManager().canRequestPackageInstalls()) {
            waitingForPermission = false;
            install(pendingApk);
        }
    }

    void handleInstallStatus(Intent intent) {
        if (intent == null || !ACTION_INSTALL_STATUS.equals(intent.getAction())) return;
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) activity.startActivity(confirmation);
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            activity.showUpdateStatus("Обновление установлено");
        } else {
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            activity.showUpdateStatus("Не удалось установить обновление" + (message == null ? "" : ": " + message));
        }
    }

    private void requestInstallPermissionOrInstall(String version) {
        activity.showUpdateStatus("Обновление " + version + " скачано");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            waitingForPermission = true;
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settings);
            return;
        }
        install(pendingApk);
    }

    private void install(File apk) {
        new Thread(() -> {
            PackageInstaller.Session session = null;
            try {
                PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(activity.getPackageName());
                int sessionId = installer.createSession(params);
                session = installer.openSession(sessionId);
                try (InputStream input = new FileInputStream(apk);
                     OutputStream output = session.openWrite("app.apk", 0, apk.length())) {
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                    session.fsync(output);
                }
                Intent callback = new Intent(activity, MainActivity.class).setAction(ACTION_INSTALL_STATUS);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
                PendingIntent pending = PendingIntent.getActivity(activity, sessionId, callback, flags);
                session.commit(pending.getIntentSender());
                activity.runOnUiThread(() -> activity.showUpdateStatus("Откройте системное окно установки"));
            } catch (Exception error) {
                if (session != null) session.abandon();
                activity.runOnUiThread(() -> activity.showUpdateStatus("Не удалось запустить установку"));
            } finally {
                if (session != null) session.close();
            }
        }, "cashback-update-install").start();
    }

    private static String findApk(JSONArray assets, String version) {
        if (assets == null) return "";
        String expected = "lenta-cashback-" + version + ".apk";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset != null && expected.equals(asset.optString("name"))) return asset.optString("browser_download_url");
        }
        return "";
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int av = i < a.length ? number(a[i]) : 0;
            int bv = i < b.length ? number(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int number(String value) {
        try { return Integer.parseInt(value.replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }

    private static String readText(String url) throws Exception {
        try (InputStream input = connect(url).getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copy(input, output);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void download(String url, File target) throws Exception {
        File part = new File(target.getParentFile(), target.getName() + ".part");
        try (InputStream input = connect(url).getInputStream(); OutputStream output = new java.io.FileOutputStream(part)) {
            copy(input, output);
        }
        if (target.exists() && !target.delete()) throw new IllegalStateException("Cannot replace update");
        if (!part.renameTo(target)) throw new IllegalStateException("Cannot finish update");
    }

    private static HttpURLConnection connect(String value) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "ESI-Cashback-Android");
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return connection;
    }

    private static void copy(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[32 * 1024];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
    }
}
