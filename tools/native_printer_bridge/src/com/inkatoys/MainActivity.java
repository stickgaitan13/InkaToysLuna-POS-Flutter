package com.inkatoys;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQ = 5173;
    private static final int STORAGE_TREE_REQ = 6612;
    private WebView webView;
    private StorageBridge storageBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        storageBridge = new StorageBridge(this);
        webView.addJavascriptInterface(storageBridge, "InkaStorage");
        webView.setWebChromeClient(new InkaChromeClient(this));
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        webView.postDelayed(new Runnable() {
            @Override public void run() {
                if (storageBridge != null) storageBridge.requestInitialPermission();
            }
        }, 1200);
    }

    public WebView getWebView() { return webView; }

    public void launchStorageTree() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(i, STORAGE_TREE_REQ);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STORAGE_TREE_REQ) {
            Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
            if (storageBridge != null) storageBridge.onTreeSelected(uri);
            return;
        }
        if (requestCode != FILE_CHOOSER_REQ || webView == null) return;
        Object tag = webView.getTag();
        if (!(tag instanceof ValueCallback)) return;
        @SuppressWarnings("unchecked")
        ValueCallback<Uri[]> callback = (ValueCallback<Uri[]>) tag;
        callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        webView.setTag(null);
    }

    public void launchFileChooser(WebView view, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        view.setTag(callback);
        startActivityForResult(params.createIntent(), FILE_CHOOSER_REQ);
    }
}
