package jp.nita.tonnetzvizforandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

import jp.nita.utils.FileSelectDialog;

import static jp.nita.utils.FileSelectDialog.*;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestReadExternalStoragePermission();

        initTabs();

        mWebView = (WebView) findViewById(R.id.webView1);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {

            }
        });
        mWebView.loadUrl("file:///android_asset/tonnetz-viz/index.html");

        HashMap<Integer, Button> buttonKeyMap = new HashMap<Integer, Button>();
        buttonKeyMap.put(65, (Button)findViewById(R.id.button_a));
        buttonKeyMap.put(87, (Button)findViewById(R.id.button_w));
        buttonKeyMap.put(83, (Button)findViewById(R.id.button_s));
        buttonKeyMap.put(69, (Button)findViewById(R.id.button_e));
        buttonKeyMap.put(68, (Button)findViewById(R.id.button_d));
        buttonKeyMap.put(70, (Button)findViewById(R.id.button_f));
        buttonKeyMap.put(84, (Button)findViewById(R.id.button_t));
        buttonKeyMap.put(71, (Button)findViewById(R.id.button_g));
        buttonKeyMap.put(89, (Button)findViewById(R.id.button_y));
        buttonKeyMap.put(72, (Button)findViewById(R.id.button_h));
        buttonKeyMap.put(85, (Button)findViewById(R.id.button_u));
        buttonKeyMap.put(74, (Button)findViewById(R.id.button_j));

        final Integer keys[] = buttonKeyMap.keySet().toArray(new Integer[0]);
        for (int i = 0; i < keys.length; i++) {
            final Integer finalKey = keys[i];
            buttonKeyMap.get(keys[i]).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mWebView.evaluateJavascript("window.dispatchEvent(new KeyboardEvent(\"keydown\",{keyCode:" + finalKey + " }));",
                                    new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String reply) {
                                            Log.d(this.getClass().toString(), "replay = " + reply);
                                        }
                                    });
                            break;
                        case MotionEvent.ACTION_UP:
                            mWebView.evaluateJavascript("window.dispatchEvent(new KeyboardEvent(\"keyup\",{keyCode: " + finalKey + " }));",
                                    new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String reply) {
                                            Log.d(this.getClass().toString(), "replay = " + reply);
                                        }
                                    });
                            break;
                        default:
                            break;
                    }
                    return false;
                }
            });
        }

        Button buttonSelectMidiFile = findViewById(R.id.button_select_midi_file);
        buttonSelectMidiFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] midiExtensions = {"mid", "midi"};

                FileSelectDialog dialog = new FileSelectDialog(v.getContext(), new OnFileSelectListener() {
                    @Override
                    public void onFileSelect(File file) {
                        ((TextView)findViewById(R.id.message)).setText(file.getAbsolutePath());
                    }
                });
                File initialDirectory = Environment.getExternalStorageDirectory();

                dialog.show(initialDirectory, midiExtensions);
            }
        });

    }

    private void requestReadExternalStoragePermission()
    {
        if( PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) )
        {
            return;
        }
        ActivityCompat.requestPermissions( this,
                new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults )
    {
        switch( requestCode )
        {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if( grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED )
                {
                    Toast.makeText( this, "Permission denied.", Toast.LENGTH_SHORT ).show();
                    finish();
                    return;
                }
                break;
            default:
                break;
        }
    }

    protected void initTabs() {
        try {
            TabHost tabHost = (TabHost)findViewById(R.id.tabHost);
            tabHost.setup();
            TabHost.TabSpec spec;

            spec = tabHost.newTabSpec("Tab1")
                    .setIndicator(getString(R.string.tab_keyboard))
                    .setContent(R.id.tab1);
            tabHost.addTab(spec);

            spec = tabHost.newTabSpec("Tab2")
                    .setIndicator(getString(R.string.tab_midi_file))
                    .setContent(R.id.tab2);
            tabHost.addTab(spec);

            tabHost.setCurrentTab(0);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}