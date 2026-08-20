package com.inkatoys;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageBridge {
    private static final String PREFS = "inka_storage_v862";
    private static final String KEY_TREE = "tree_uri";
    private static final String APP_FOLDER = "INKA TOYS LUNA";
    private final MainActivity activity;
    private final SharedPreferences prefs;
    private final Map<String, ByteArrayOutputStream> buffers = new HashMap<>();
    private final Map<String, String> paths = new HashMap<>();
    private final Map<String, String> mimes = new HashMap<>();

    public StorageBridge(MainActivity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @JavascriptInterface public boolean hasRoot() { return getTreeUri() != null; }
    @JavascriptInterface public String status() { Uri tree=getTreeUri(); return tree==null?"NEEDS_PERMISSION":"OK|"+tree; }
    @JavascriptInterface public void requestRoot() { activity.runOnUiThread(new Runnable(){ @Override public void run(){ activity.launchStorageTree(); }}); }

    public void requestInitialPermission() {
        if (hasRoot()) { ensureBaseFolders(); notifyReady(true); return; }
        new AlertDialog.Builder(activity)
            .setTitle("Permiso de almacenamiento")
            .setMessage("INKA TOYS LUNA necesita autorización para crear su carpeta principal y guardar Cierres, Reportes, Tickets, Datos y Respaldos en el dispositivo. Pulsa CONTINUAR y selecciona el almacenamiento interno o la carpeta donde deseas guardar INKA TOYS LUNA.")
            .setCancelable(false)
            .setNegativeButton("AHORA NO", null)
            .setPositiveButton("CONTINUAR", (d,w)->activity.launchStorageTree())
            .show();
    }

    public void onTreeSelected(Uri uri) {
        if (uri==null) { notifyReady(false); return; }
        try {
            int flags=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            activity.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch(Throwable ignored) {}
        prefs.edit().putString(KEY_TREE, uri.toString()).apply();
        boolean ok=ensureBaseFolders();
        notifyReady(ok);
    }

    @JavascriptInterface public boolean ensureBaseFolders() {
        try {
            Uri root=appRoot(true); if(root==null) return false;
            ensureDir(root,"Cierres"); ensureDir(root,"Reportes"); ensureDir(root,"Tickets"); ensureDir(root,"Datos"); ensureDir(root,"Respaldos");
            return true;
        } catch(Throwable t){ return false; }
    }

    @JavascriptInterface public String beginWrite(String relativePath,String mime) {
        if(getTreeUri()==null) return "ERR:NO_PERMISSION";
        String token=UUID.randomUUID().toString();
        synchronized(buffers){ buffers.put(token,new ByteArrayOutputStream()); paths.put(token,normalize(relativePath)); mimes.put(token,(mime==null||mime.trim().isEmpty())?"application/octet-stream":mime); }
        return token;
    }

    @JavascriptInterface public String appendWrite(String token,String base64) {
        try { ByteArrayOutputStream out; synchronized(buffers){ out=buffers.get(token); } if(out==null) return "ERR:TOKEN"; out.write(Base64.decode(base64,Base64.DEFAULT)); return "OK"; }
        catch(Throwable t){ return "ERR:"+safe(t); }
    }

    @JavascriptInterface public String finishWrite(String token) {
        ByteArrayOutputStream out; String path,mime;
        synchronized(buffers){ out=buffers.remove(token); path=paths.remove(token); mime=mimes.remove(token); }
        if(out==null||path==null) return "ERR:TOKEN";
        try { Uri uri=createFilePath(path,mime,true); if(uri==null) return "ERR:CREATE"; OutputStream os=activity.getContentResolver().openOutputStream(uri,"w"); if(os==null) return "ERR:OPEN"; os.write(out.toByteArray()); os.flush(); os.close(); return "OK|"+uri; }
        catch(Throwable t){ return "ERR:"+safe(t); }
    }

    @JavascriptInterface public String writeBase64(String relativePath,String mime,String base64) {
        try { Uri uri=createFilePath(normalize(relativePath),mime,true); if(uri==null) return "ERR:CREATE"; OutputStream os=activity.getContentResolver().openOutputStream(uri,"w"); if(os==null) return "ERR:OPEN"; os.write(Base64.decode(base64,Base64.DEFAULT)); os.flush(); os.close(); return "OK|"+uri; }
        catch(Throwable t){ return "ERR:"+safe(t); }
    }

    @JavascriptInterface public String openPath(String relativePath) {
        try {
            final Uri uri=resolvePath(normalize(relativePath)); if(uri==null) return "ERR:NOT_FOUND";
            activity.runOnUiThread(new Runnable(){ @Override public void run(){ Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(uri,"application/pdf"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); try{activity.startActivity(i);}catch(Throwable t){activity.startActivity(Intent.createChooser(i,"Abrir reporte"));} }});
            return "OK";
        } catch(Throwable t){ return "ERR:"+safe(t); }
    }

    @JavascriptInterface public String printPath(String relativePath,String jobName) {
        try {
            final Uri uri=resolvePath(normalize(relativePath)); if(uri==null) return "ERR:NOT_FOUND";
            final String label=(jobName==null||jobName.trim().isEmpty())?"INKA TOYS LUNA":jobName;
            activity.runOnUiThread(new Runnable(){ @Override public void run(){ PrintManager pm=(PrintManager)activity.getSystemService(Context.PRINT_SERVICE); pm.print(label,new UriPrintAdapter(activity,uri,label),new PrintAttributes.Builder().build()); }});
            return "OK";
        } catch(Throwable t){ return "ERR:"+safe(t); }
    }

    @JavascriptInterface public String rootDescription(){ Uri t=getTreeUri(); return t==null?"Sin autorización":"Carpeta autorizada · "+t; }

    private Uri getTreeUri(){ String s=prefs.getString(KEY_TREE,""); if(s==null||s.isEmpty()) return null; try{return Uri.parse(s);}catch(Throwable t){return null;} }

    private Uri rootDocumentUri(Uri tree) throws Exception {
        if(tree==null) return null;
        try {
            String id=DocumentsContract.getTreeDocumentId(tree);
            return DocumentsContract.buildDocumentUriUsingTree(tree,id);
        } catch(Throwable ignored) {
            try { DocumentsContract.getDocumentId(tree); return tree; } catch(Throwable t) { throw new Exception("TREE_URI:"+safe(t)); }
        }
    }

    private String documentId(Uri uri) throws Exception {
        try { return DocumentsContract.getDocumentId(uri); }
        catch(Throwable first) { try { return DocumentsContract.getTreeDocumentId(uri); } catch(Throwable second){ throw new Exception("DOC_ID:"+safe(second)); } }
    }

    private Uri appRoot(boolean create) throws Exception {
        Uri tree=getTreeUri(); if(tree==null) return null;
        Uri rootDoc=rootDocumentUri(tree);
        String selectedName=displayName(rootDoc);
        if(APP_FOLDER.equalsIgnoreCase(selectedName)) return rootDoc;
        Uri found=findChild(rootDoc,APP_FOLDER); if(found!=null) return found;
        if(!create) return null;
        return DocumentsContract.createDocument(activity.getContentResolver(),rootDoc,DocumentsContract.Document.MIME_TYPE_DIR,APP_FOLDER);
    }

    private Uri ensureDir(Uri parent,String name) throws Exception { Uri u=findChild(parent,name); if(u!=null) return u; return DocumentsContract.createDocument(activity.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,cleanName(name)); }

    private Uri findChild(Uri parent,String name) throws Exception {
        ContentResolver cr=activity.getContentResolver();
        String parentId=documentId(parent);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,parentId);
        Cursor c=cr.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
        if(c==null) return null;
        try { while(c.moveToNext()){ if(name.equalsIgnoreCase(c.getString(1))) return DocumentsContract.buildDocumentUriUsingTree(parent,c.getString(0)); } }
        finally { c.close(); }
        return null;
    }

    private Uri createFilePath(String relativePath,String mime,boolean replace) throws Exception {
        Uri root=appRoot(true); if(root==null) return null;
        String[] parts=normalize(relativePath).split("/"); Uri dir=root;
        for(int i=0;i<parts.length-1;i++) if(!parts[i].isEmpty()) dir=ensureDir(dir,parts[i]);
        String fileName=parts.length==0?"documento.pdf":cleanName(parts[parts.length-1]);
        Uri old=findChild(dir,fileName);
        if(old!=null&&replace){ try{DocumentsContract.deleteDocument(activity.getContentResolver(),old);}catch(Throwable ignored){} }
        else if(old!=null) return old;
        return DocumentsContract.createDocument(activity.getContentResolver(),dir,(mime==null||mime.isEmpty())?"application/octet-stream":mime,fileName);
    }

    private Uri resolvePath(String relativePath) throws Exception {
        Uri root=appRoot(false); if(root==null) return null; Uri cur=root;
        for(String p:normalize(relativePath).split("/")){ if(p.isEmpty()) continue; cur=findChild(cur,p); if(cur==null) return null; }
        return cur;
    }

    private String displayName(Uri uri){ Cursor c=null; try{ c=activity.getContentResolver().query(uri,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null); if(c!=null&&c.moveToFirst()) return c.getString(0); }catch(Throwable ignored){} finally{if(c!=null)c.close();} return ""; }
    private static String normalize(String p){ p=p==null?"":p.replace('\\','/').replace("..",""); while(p.startsWith("/"))p=p.substring(1); while(p.contains("//"))p=p.replace("//","/"); return p; }
    private static String cleanName(String n){ if(n==null||n.trim().isEmpty())return"SIN_NOMBRE"; return n.replace('/','_').replace('\\','_').replace(':','-').trim(); }

    private void notifyReady(final boolean ok){ if(activity.getWebView()==null)return; activity.getWebView().post(new Runnable(){ @Override public void run(){ activity.getWebView().evaluateJavascript("window.INKA_NATIVE_STORAGE_READY&&window.INKA_NATIVE_STORAGE_READY("+(ok?"true":"false")+");",null); }}); }
    private static String safe(Throwable t){ String m=t==null?"UNKNOWN":t.getMessage(); if(m==null||m.trim().isEmpty())m=t==null?"UNKNOWN":t.getClass().getSimpleName(); return m.replace('\n',' ').replace('\r',' '); }

    private static class UriPrintAdapter extends PrintDocumentAdapter {
        private final Context context; private final Uri uri; private final String name;
        UriPrintAdapter(Context context,Uri uri,String name){this.context=context;this.uri=uri;this.name=name;}
        @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes newAttributes,CancellationSignal cancellationSignal,LayoutResultCallback callback,Bundle extras){ if(cancellationSignal.isCanceled()){callback.onLayoutCancelled();return;} callback.onLayoutFinished(new PrintDocumentInfo.Builder(name+".pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(),true); }
        @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,CancellationSignal cancellationSignal,WriteResultCallback callback){ InputStream in=null; FileOutputStream out=null; try{ in=context.getContentResolver().openInputStream(uri); out=new FileOutputStream(destination.getFileDescriptor()); byte[] buf=new byte[32768]; int n; while((n=in.read(buf))>0){ if(cancellationSignal.isCanceled()){callback.onWriteCancelled();return;} out.write(buf,0,n);} out.flush(); callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES}); }catch(Throwable t){callback.onWriteFailed(safe(t));}finally{try{if(in!=null)in.close();}catch(Throwable ignored){}try{if(out!=null)out.close();}catch(Throwable ignored){}} }
    }
}
