package com.inkatoys;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InkaChromeClient extends WebChromeClient {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int BT_PERMISSION_REQ = 9091;
    private final MainActivity activity;
    private final ExecutorService printExecutor = Executors.newSingleThreadExecutor();

    public InkaChromeClient(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<android.net.Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        activity.launchFileChooser(webView, filePathCallback, fileChooserParams);
        return true;
    }

    @Override
    public boolean onJsPrompt(final WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        if (message == null) return false;
        try {
            if ("INKA_NATIVE_BLUETOOTH_SETTINGS".equals(message)) {
                activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                result.confirm("OK");
                return true;
            }
            if ("INKA_BT_LIST".equals(message)) {
                if (!ensureBtPermission()) { result.confirm("PERMISSION_REQUIRED"); return true; }
                result.confirm(listBondedDevices());
                return true;
            }
            if (message.startsWith("INKA_BT_PING:")) {
                if (!ensureBtPermission()) { result.confirm("PERMISSION_REQUIRED"); return true; }
                result.confirm(ping(message.substring("INKA_BT_PING:".length()).trim()));
                return true;
            }
            if (message.startsWith("INKA_BT_PRINT_ASYNC:")) {
                if (!ensureBtPermission()) { result.confirm("PERMISSION_REQUIRED"); return true; }
                String body = message.substring("INKA_BT_PRINT_ASYNC:".length());
                int sep = body.indexOf('|');
                if (sep <= 0) { result.confirm("ERR:PAYLOAD"); return true; }
                final String mac = body.substring(0, sep).trim();
                final byte[] data = android.util.Base64.decode(body.substring(sep + 1).trim(), android.util.Base64.DEFAULT);
                result.confirm("QUEUED");
                printExecutor.execute(new Runnable() {
                    @Override public void run() {
                        final String printResult = printBytes(mac, data);
                        activity.runOnUiThread(new Runnable() {
                            @Override public void run() {
                                try {
                                    String escaped = jsonEscape(printResult);
                                    view.evaluateJavascript("window.INKA_BT_ASYNC_RESULT&&window.INKA_BT_ASYNC_RESULT(\"" + escaped + "\")", null);
                                } catch (Throwable ignored) {}
                            }
                        });
                    }
                });
                return true;
            }
            if (message.startsWith("INKA_BT_PRINT:")) {
                if (!ensureBtPermission()) { result.confirm("PERMISSION_REQUIRED"); return true; }
                String body = message.substring("INKA_BT_PRINT:".length());
                int sep = body.indexOf('|');
                if (sep <= 0) { result.confirm("ERR:PAYLOAD"); return true; }
                String mac = body.substring(0, sep).trim();
                byte[] data = android.util.Base64.decode(body.substring(sep + 1).trim(), android.util.Base64.DEFAULT);
                result.confirm(printBytes(mac, data));
                return true;
            }
            if ("INKA_NATIVE_PRINT".equals(message)) {
                result.confirm("USE_INTERNAL_BT");
                return true;
            }
            if (message.startsWith("INKA_NATIVE_SHARE:")) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, message.substring("INKA_NATIVE_SHARE:".length()));
                activity.startActivity(Intent.createChooser(send, "Compartir PDF"));
                result.confirm("OK");
                return true;
            }
        } catch (Throwable t) {
            result.confirm("ERR:" + safeMessage(t));
            return true;
        }
        return false;
    }

    private boolean ensureBtPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) return true;
        activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BT_PERMISSION_REQ);
        return false;
    }

    private String listBondedDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return "ERR:NO_BLUETOOTH";
        if (!adapter.isEnabled()) return "ERR:BLUETOOTH_OFF";
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (BluetoothDevice d : devices) {
            if (!first) sb.append(',');
            first = false;
            String name = d.getName();
            if (name == null || name.trim().isEmpty()) name = "Bluetooth";
            sb.append("{\"name\":\"").append(jsonEscape(name)).append("\",\"mac\":\"")
              .append(jsonEscape(d.getAddress())).append("\"}");
        }
        return sb.append(']').toString();
    }

    private String ping(String mac) {
        BluetoothSocket socket = null;
        try { socket = connect(mac); return "OK"; }
        catch (Throwable t) { return "ERR:" + safeMessage(t); }
        finally { closeQuietly(socket); }
    }

    private String printBytes(String mac, byte[] bytes) {
        BluetoothSocket socket = null;
        OutputStream out = null;
        try {
            socket = connect(mac);
            out = socket.getOutputStream();
            out.write(bytes);
            out.flush();
            try { Thread.sleep(220); } catch (InterruptedException ignored) {}
            return "OK";
        } catch (Throwable t) {
            return "ERR:" + safeMessage(t);
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            closeQuietly(socket);
        }
    }

    private BluetoothSocket connect(String mac) throws Exception {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) throw new Exception("NO_BLUETOOTH");
        if (!adapter.isEnabled()) throw new Exception("BLUETOOTH_OFF");
        if (Build.VERSION.SDK_INT < 31) {
            try { adapter.cancelDiscovery(); } catch (Throwable ignored) {}
        }
        BluetoothDevice device = adapter.getRemoteDevice(mac);
        BluetoothSocket secure = null;
        try {
            secure = device.createRfcommSocketToServiceRecord(SPP_UUID);
            secure.connect();
            return secure;
        } catch (Throwable first) {
            closeQuietly(secure);
            BluetoothSocket insecure = null;
            try {
                insecure = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                insecure.connect();
                return insecure;
            } catch (Throwable second) {
                closeQuietly(insecure);
                throw new Exception("RFCOMM:" + safeMessage(second));
            }
        }
    }

    private static void closeQuietly(BluetoothSocket s) {
        try { if (s != null) s.close(); } catch (Throwable ignored) {}
    }

    private static String safeMessage(Throwable t) {
        String m = t == null ? "UNKNOWN" : t.getMessage();
        if (m == null || m.trim().isEmpty()) m = t == null ? "UNKNOWN" : t.getClass().getSimpleName();
        return m.replace('\n', ' ').replace('\r', ' ');
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
