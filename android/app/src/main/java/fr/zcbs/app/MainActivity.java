package fr.zcbs.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

/**
 * Affiche le lecteur web de ZQuest Classic (embarqué dans l'APK) avec le moteur de Firefox
 * (GeckoView), qui gère l'isolation inter-origines nécessaire aux threads du moteur.
 * Les ressources sont servies par un petit serveur local avec les en-têtes COOP/COEP.
 */
public class MainActivity extends Activity {
    private static GeckoRuntime sRuntime;

    private GeckoView geckoView;
    private GeckoSession session;
    private LocalServer server;
    private String startUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersive();

        try {
            server = new LocalServer(getAssets());
            server.start(5000, false);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de démarrer le serveur local", e);
        }
        startUrl = "http://127.0.0.1:" + server.getListeningPort() + "/assets/app/index.html";

        geckoView = new GeckoView(this);
        setContentView(geckoView);

        if (sRuntime == null) sRuntime = GeckoRuntime.create(this);
        session = new GeckoSession();
        session.open(sRuntime);
        geckoView.setSession(session);
        session.loadUri(startUrl);
        geckoView.requestFocus();
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
        // Retour : revient au menu des quêtes.
        if (keyCode == KeyEvent.KEYCODE_BACK && session != null) {
            session.loadUri(startUrl);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (session != null) session.close();
        if (server != null) server.stop();
        super.onDestroy();
    }
}
