package PACKAGE_PLACEHOLDER;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wv = new WebView(this);
        wv.setBackgroundColor(Color.BLACK);
        
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setTextZoom(100);
        
        // Bypassing CORS for local files
        ws.setAllowFileAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        
        // Disable user gesture requirement for media playback (allows autoplay with sound)
        ws.setMediaPlaybackRequiresUserGesture(false);
        
        // Override User Agent to look like a standard mobile Chrome (prevents YouTube WebView blocking)
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        
        wv.setWebViewClient(new WebViewClient());
        wv.addJavascriptInterface(new Bridge(this), "NativeBridge");
        
        try {
            InputStream is = getResources().openRawResource(
                getResources().getIdentifier("index", "raw", getPackageName()));
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            
            // Set base URL to http://localhost to bypass HTTPS domain spoofing restrictions
            wv.loadDataWithBaseURL("http://localhost", sb.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            wv.loadData("<h1 style='color:green;background:black'>Load Error: " + e.getMessage() + "</h1>", "text/html", "UTF-8");
        }
        setContentView(wv);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            int kc = event.getKeyCode();
            if (kc == 21) { wv.evaluateJavascript("onKey('left')", null); return true; }
            if (kc == 22) { wv.evaluateJavascript("onKey('right')", null); return true; }
            if (kc == 23) { wv.evaluateJavascript("onKey('center')", null); return true; }
            if (kc == 19) { wv.evaluateJavascript("onKey('up')", null); return true; }
            if (kc == 20) { wv.evaluateJavascript("onKey('down')", null); return true; }
        }
        return super.dispatchKeyEvent(event);
    }

    public static class Bridge {
        private final Activity activity;

        public Bridge(Activity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public void save(String key, String val) {
            activity.getSharedPreferences("app", 0).edit().putString(key, val).apply();
        }

        @JavascriptInterface
        public String load(String key) {
            return activity.getSharedPreferences("app", 0).getString(key, "");
        }

        @JavascriptInterface
        public String loadConfig() {
            try {
                File file = new File(activity.getExternalFilesDir(null), "config.json");
                if (!file.exists()) {
                    return "";
                }
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return sb.toString();
            } catch (Exception e) {
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        }

        @JavascriptInterface
        public String fetchUrl(String urlString) {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.74 Mobile Safari/537.36");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                } else {
                    return "Error: HTTP Code " + responseCode;
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (Exception e) {}
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }
}
