package jp.nita.tonnetzvizforandroid;

import androidx.appcompat.app.AppCompatActivity;
import android.webkit.*;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = (WebView)findViewById(R.id.webView1);

        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebViewClient( new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {

            }
        });

        mWebView.loadUrl("file:///android_asset/tonnetz-viz/index.html");
    }

}