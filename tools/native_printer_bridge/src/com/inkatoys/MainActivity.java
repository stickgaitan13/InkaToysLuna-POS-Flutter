package com.inkatoys;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQ = 5173;
    private WebView webView;

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
        webView.setWebChromeClient(new InkaChromeClient(this));
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
