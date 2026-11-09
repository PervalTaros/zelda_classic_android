package fr.zcbs.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Affiche le lecteur web de ZQuest Classic (embarqué dans l'APK) dans une WebView.
 * Le lecteur a besoin de SharedArrayBuffer, donc d'une page "cross-origin isolated" :
 * on ajoute les en-têtes COOP/COEP à chaque réponse servie depuis les ressources de l'APK.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/app/index.html";

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersive();

        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        setContentView(webView);
        WebView.setWebContentsDebuggingEnabled(true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse resp = assetLoader.shouldInterceptRequest(request.getUrl());
                if (resp == null) return null;

                String path = request.getUrl().getPath();
                if (path != null) {
                    if (path.endsWith(".wasm")) resp.setMimeType("application/wasm");
                    else if (path.endsWith(".js") || path.endsWith(".mjs")) resp.setMimeType("text/javascript");
                    else if (path.endsWith(".json")) resp.setMimeType("application/json");
                    else if (path.endsWith(".html")) resp.setMimeType("text/html");
                    else if (path.endsWith(".css")) resp.setMimeType("text/css");
                }

                Map<String, String> headers = new HashMap<>();
                if (resp.getResponseHeaders() != null) headers.putAll(resp.getResponseHeaders());
                headers.put("Cross-Origin-Opener-Policy", "same-origin");
                headers.put("Cross-Origin-Embedder-Policy", "require-corp");
                headers.put("Cross-Origin-Resource-Policy", "same-origin");
                headers.put("Access-Control-Allow-Origin", "*");
                resp.setResponseHeaders(headers);
                return resp;
            }
        });

        webView.loadUrl(START_URL);
    }

    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Retour : revient au menu des quêtes plutôt que de quitter d'un coup.
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null) {
            String url = webView.getUrl();
            if (url != null && !url.contains("/assets/app/")) {
                webView.loadUrl(START_URL);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }
}
