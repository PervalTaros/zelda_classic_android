package fr.zcbs.app;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import fi.iki.elonen.NanoHTTPD;

/**
 * Petit serveur HTTP local (127.0.0.1) qui sert les ressources de l'APK avec les
 * en-têtes COOP/COEP nécessaires à SharedArrayBuffer (threads du moteur).
 */
public class LocalServer extends NanoHTTPD {
    private final AssetManager assets;

    public LocalServer(AssetManager assets) {
        super("127.0.0.1", 0);
        this.assets = assets;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null || uri.equals("/")) uri = "/assets/app/index.html";
        if (!uri.startsWith("/assets/")) return notFound();

        String path = uri.substring("/assets/".length());
        if (path.endsWith("/")) path += "index.html";
        if (path.contains("..")) return notFound();

        try {
            InputStream in = assets.open(path);
            Response r = newChunkedResponse(Response.Status.OK, mime(path), in);
            addIsolationHeaders(r);
            return r;
        } catch (IOException e) {
            return notFound();
        }
    }

    private Response notFound() {
        Response r = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404");
        addIsolationHeaders(r);
        return r;
    }

    private void addIsolationHeaders(Response r) {
        r.addHeader("Cross-Origin-Opener-Policy", "same-origin");
        r.addHeader("Cross-Origin-Embedder-Policy", "require-corp");
        r.addHeader("Cross-Origin-Resource-Policy", "same-origin");
        r.addHeader("Cache-Control", "no-cache");
    }

    private static String mime(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html; charset=utf-8";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "text/javascript; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".json") || p.endsWith(".webmanifest") || p.endsWith(".map")) return "application/json";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }
}
