package com.kiranrajagopal.abxtimeout;

import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private WebView printView; // kept alive until printing finishes
    private String pendingSave;
    private static final int REQ_SAVE = 11, REQ_OPEN = 12;

    /** Called from the page: render report HTML and open Android's print / Save as PDF dialog. */
    public class Bridge {
        @JavascriptInterface
        public void setAlarms(final String json) { Alarms.setAll(getApplicationContext(), json); }

        @JavascriptInterface
        public boolean notificationsAllowed() {
            if (Build.VERSION.SDK_INT >= 33)
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            return true;
        }

        /** Safety copy of all data in the app's private storage (also included in Android's own device backup). */
        @JavascriptInterface
        public void saveSnapshot(final String json) {
            try {
                java.io.File tmp = new java.io.File(getFilesDir(), "courses.json.tmp");
                try (OutputStream os = new java.io.FileOutputStream(tmp)) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
                tmp.renameTo(new java.io.File(getFilesDir(), "courses.json"));
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public String loadSnapshot() {
            java.io.File f = new java.io.File(getFilesDir(), "courses.json");
            if (!f.exists()) return "";
            try (InputStream is = new java.io.FileInputStream(f)) {
                byte[] b = new byte[(int) f.length()]; int off = 0, n;
                while (off < b.length && (n = is.read(b, off, b.length - off)) > 0) off += n;
                return new String(b, 0, off, StandardCharsets.UTF_8);
            } catch (Exception e) { return ""; }
        }

        /** Backup: let the user choose where to save (Drive, Downloads, etc.). */
        @JavascriptInterface
        public void saveFile(final String name, final String text) {
            runOnUiThread(() -> {
                pendingSave = text;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, name);
                try { startActivityForResult(i, REQ_SAVE); } catch (Exception e) { js("window.__abxSaved&&__abxSaved(false)"); }
            });
        }

        /** Restore: let the user pick a backup file. */
        @JavascriptInterface
        public void openFile() {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try { startActivityForResult(i, REQ_OPEN); } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void printHtml(final String html, final String name) {
            runOnUiThread(() -> {
                final WebView pv = new WebView(MainActivity.this);
                printView = pv;
                pv.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                        PrintAttributes attrs = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                        pm.print(name, view.createPrintDocumentAdapter(name), attrs);
                    }
                });
                pv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Alarms.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        // Respect large system fonts, but cap them so cards and buttons do not break.
        if (s.getTextZoom() > 120) s.setTextZoom(120);
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
    }

    private void js(String code) { web.post(() -> web.evaluateJavascript(code, null)); }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        Uri uri = (res == RESULT_OK && data != null) ? data.getData() : null;
        if (req == REQ_SAVE) {
            boolean ok = false;
            if (uri != null && pendingSave != null) {
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    os.write(pendingSave.getBytes(StandardCharsets.UTF_8));
                    ok = true;
                } catch (Exception ignored) {}
            }
            pendingSave = null;
            if (uri != null) js("window.__abxSaved&&__abxSaved(" + ok + ")");
        } else if (req == REQ_OPEN && uri != null) {
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[8192]; int n;
                while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
                js("window.__abxOpened&&__abxOpened(" + JSONObject.quote(sb.toString()) + ")");
            } catch (Exception e) {
                js("window.__abxOpened&&__abxOpened('')");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        // Close an open form first; otherwise leave the app.
        web.evaluateJavascript(
            "(function(){var s=[].slice.call(document.querySelectorAll('.scrim')).filter(function(x){return !x.hidden;});" +
            "if(s.length){s.forEach(function(x){x.hidden=true;});return '1';}return '0';})()",
            v -> { if (v == null || !v.contains("1")) MainActivity.super.onBackPressed(); });
    }
}
