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

        // Port fixe : l'adresse (donc le stockage des sauvegardes) reste la même d'un lancement à l'autre.
        try {
            server = new LocalServer(getAssets(), 17846);
            server.start(5000, false);
        } catch (Exception e1) {
            try {
                server = new LocalServer(getAssets(), 0);
                server.start(5000, false);
            } catch (Exception e2) {
                throw new RuntimeException("Impossible de démarrer le serveur local", e2);
            }
        }
        startUrl = "http://127.0.0.1:" + server.getListeningPort() + "/assets/app/index.html";

        geckoView = new GeckoView(this);
        setContentView(geckoView);

        if (sRuntime == null) {
            sRuntime = GeckoRuntime.create(this);
        }
        // Autoriser la lecture automatique du son (selon la version de GeckoView ; sinon ignoré).
        setGeckoPref("media.autoplay.default", Integer.valueOf(0));
        setGeckoPref("media.autoplay.blocking_policy", Integer.valueOf(0));
        setGeckoPref("media.autoplay.block-webaudio", Boolean.FALSE);
        session = new GeckoSession();
        // Le simple fait d'avoir un délégué permet à la page de demander le plein écran
        // (l'application est déjà en plein écran immersif).
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onFullScreen(GeckoSession s, boolean fullScreen) { }
        });
        session.open(sRuntime);
        geckoView.setSession(session);
        session.loadUri(startUrl);
        geckoView.requestFocus();
    }

    /** Règle une préférence du moteur par réflexion, pour compiler avec n'importe quelle version. */
    private static void setGeckoPref(String name, Object value) {
        try {
            Class<?> c = Class.forName("org.mozilla.geckoview.GeckoPreferenceController");
            Class<?> type = (value instanceof Boolean) ? Boolean.class : Integer.class;
            java.lang.reflect.Method m = c.getMethod("setGeckoPref", String.class, type, int.class);
            m.invoke(null, name, value, 0); // 0 = PREF_BRANCH_USER
        } catch (Throwable t) {
            // API absente dans cette version : on ignore.
        }
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
