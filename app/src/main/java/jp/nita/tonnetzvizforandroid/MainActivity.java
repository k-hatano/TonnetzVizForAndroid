package jp.nita.tonnetzvizforandroid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.Track;
import jp.nita.utils.FileSelectDialog;
import jp.nita.utils.Statics;

import static jp.nita.utils.FileSelectDialog.*;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    private Sequencer mSequencer = null;
    private SequenceRunner mSequenceRunner = null;

    private MidiManager mNativeMidiManager = null;
    private MidiDevice mNativeMidiDevice = null;
    private MidiInputPort mNativeMidiInputPort = null;
    private Handler mHandler = null;

    private List<MidiDeviceInfo> getMidiDevices(boolean isOutput) {
        ArrayList filteredMidiDevices = new ArrayList<MidiDeviceInfo>();

        for (MidiDeviceInfo midiDevice : mNativeMidiManager.getDevices()) {
            if (isOutput) {
                if (midiDevice.getOutputPortCount() > 0) filteredMidiDevices.add(midiDevice);
            } else {
                if (midiDevice.getInputPortCount() > 0) filteredMidiDevices.add(midiDevice);
            }
        }
        return filteredMidiDevices;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestReadExternalStoragePermission();

        mHandler = new Handler(Looper.getMainLooper());

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
        buttonKeyMap.put(65, (Button) findViewById(R.id.button_a));
        buttonKeyMap.put(87, (Button) findViewById(R.id.button_w));
        buttonKeyMap.put(83, (Button) findViewById(R.id.button_s));
        buttonKeyMap.put(69, (Button) findViewById(R.id.button_e));
        buttonKeyMap.put(68, (Button) findViewById(R.id.button_d));
        buttonKeyMap.put(70, (Button) findViewById(R.id.button_f));
        buttonKeyMap.put(84, (Button) findViewById(R.id.button_t));
        buttonKeyMap.put(71, (Button) findViewById(R.id.button_g));
        buttonKeyMap.put(89, (Button) findViewById(R.id.button_y));
        buttonKeyMap.put(72, (Button) findViewById(R.id.button_h));
        buttonKeyMap.put(85, (Button) findViewById(R.id.button_u));
        buttonKeyMap.put(74, (Button) findViewById(R.id.button_j));

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
                        ((TextView) findViewById(R.id.filePath)).setText(file.getAbsolutePath());
                    }
                });
                File initialDirectory = Environment.getExternalStorageDirectory();

                dialog.show(initialDirectory, midiExtensions);
            }
        });

        Button buttonPlay = findViewById(R.id.button_play);
        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tvMessage = findViewById(R.id.filePath);
                if (tvMessage.getText().length() <= 0) {
                    Toast.makeText(MainActivity.this, getString(R.string.message_no_file_selected), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mNativeMidiInputPort == null) {
                    Toast.makeText(MainActivity.this, getText(R.string.message_no_midi_device_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mSequencer != null) {
                    emergencySequencerStop();
                }

                try {
                    final int[] allControllersMask = new int[128];
                    for (int i = 0; i < allControllersMask.length; i++) {
                        allControllersMask[i] = i;
                    }

                    mSequencer = MidiSystem.getSequencer();

                    mSequencer.open();
                } catch (MidiUnavailableException exc) {
                    exc.printStackTrace();
                    Toast.makeText(MainActivity.this, getString(R.string.message_playing_midi_file_failed), Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    TextView tvFilePath = findViewById(R.id.filePath);
                    String midiFilePath = tvFilePath.getText().toString();

                    File file = new File(midiFilePath);
                    Sequence sequence = MidiSystem.getSequence(file);

                    mSequencer.setSequence(sequence);

                    mSequenceRunner = new SequenceRunner(sequence, MainActivity.this);
                    mSequenceRunner.run();

                    mSequencer.start();
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, getString(R.string.message_playing_midi_file_failed), Toast.LENGTH_SHORT).show();
                    emergencySequencerStop();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, getString(R.string.message_playing_midi_file_failed), Toast.LENGTH_SHORT).show();
                    emergencySequencerStop();
                    return;
                }
            }
        });

        Button buttonStop = findViewById(R.id.button_stop);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tvDuration = findViewById(R.id.duration);
                tvDuration.setText("");

                emergencySequencerStop();
            }
        });

        mNativeMidiManager = (MidiManager) getSystemService(Context.MIDI_SERVICE);
        List<MidiDeviceInfo> midiDevices = getMidiDevices(false);
        if (midiDevices.size() > 0) {
            mNativeMidiManager.openDevice(midiDevices.get(0),
                    new MidiManager.OnDeviceOpenedListener() {
                        @Override
                        public void onDeviceOpened(MidiDevice device) {
                            mNativeMidiDevice = device;
                            mNativeMidiInputPort = device.openInputPort(0);
                        }
                    }, new Handler(Looper.getMainLooper()));
        } else {
            Toast.makeText(this, getText(R.string.message_no_midi_device_found), Toast.LENGTH_SHORT).show();
        }

        mNativeMidiManager.registerDeviceCallback(new MidiManager.DeviceCallback() {
            public void onDeviceAdded(MidiDeviceInfo info) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "MIDI Device added.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            public void onDeviceRemoved(MidiDeviceInfo info) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "MIDI Device disconnected.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, new Handler(Looper.getMainLooper()));
    }

    public void emergencySequencerStop() {
        if (mSequenceRunner != null) {
            mSequenceRunner.stopAll();
            mSequenceRunner = null;
        }
        if (mSequencer != null) {
            if (mSequencer.isRunning()) {
                mSequencer.stop();
            }
            if (mSequencer.isOpen()) {
                mSequencer.close();
            }
        }
        mSequencer = null;

        mWebView.evaluateJavascript("tonnetz.panic();",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String reply) {
                        Log.d(this.getClass().toString(), "replay = " + reply);
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        emergencySequencerStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSequencer != null) {
            if (mSequencer.isRunning()) {
                mSequencer.stop();
            }
            if (mSequencer.isOpen()) {
                mSequencer.close();
            }
        }
        mSequencer = null;

        try {
            if (mNativeMidiInputPort != null) {
                mNativeMidiInputPort.close();
            }
            if (mNativeMidiDevice != null) {
                mNativeMidiDevice.close();
            }
        } catch (IOException exc) {

            exc.printStackTrace();
        }

    }

    private void requestReadExternalStoragePermission() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
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
            TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
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

    private class SequenceRunner {
        Thread runningThreads[] = new Thread[0];
        private Sequence mSequence;
        private Activity mActivity;
        private boolean isAlive;

        SequenceRunner(Sequence newSequence, Activity newActivity) {
            mSequence = newSequence;
            mActivity = newActivity;
            isAlive = true;
        }

        public void run() {
            ArrayList<Thread> runningThreadsList = new ArrayList<Thread>();
            for (int i = 0; i < mSequence.getTracks().length; i++) {
                Thread thread = generateRunningThread(mSequence, mSequence.getTracks()[i]);
                runningThreadsList.add(thread);
            }

            runningThreads = runningThreadsList.toArray(new Thread[0]);
            for (int i = 0; i < runningThreads.length; i++) {
                runningThreads[i].start();
            }
        }

        public void stopAll() {
            isAlive = false;
            runningThreads = new Thread[0];
        }

        public Thread generateRunningThread(final Sequence sequence, Track track) {
            final Track finalTrack = track;
            Thread thread = new Thread() {
                public void run() {
                    long lastTick = 0;
                    for (int i = 0; i < finalTrack.size() && isAlive; i++) {
                        MidiEvent event = finalTrack.get(i);
                        MidiMessage message = event.getMessage();
                        byte messageBytes[] = message.getMessage();

                        try {
                            Thread.sleep((event.getTick() - lastTick) * 400 / sequence.getResolution());
                            // TODO: sleep時間の算出を正確に
                        } catch (InterruptedException ignore) {

                        }
                        lastTick = event.getTick();

                        Log.i(this.getClass().toString(), Statics.bin2hex(messageBytes));

                        int messageCommand = (int) (messageBytes[0] & 0xFF);

                        String script = "";
                        if ((messageBytes[0] & 0xFF) >= 0x90 && (messageBytes[0] & 0xFF) <= 0x9F) {
                            if ((messageBytes[2] & 0xFF) == 0x00) {
                                script = "tonnetz.noteOff(" + (int) (messageBytes[0] & 0x0F) + "," + (int) (messageBytes[1] & 0xFF) + ");";
                            } else {
                                script = "tonnetz.noteOn(" + (int) (messageBytes[0] & 0x0F) + "," + (int) (messageBytes[1] & 0xFF) + ");";
                            }
                        } else if ((messageBytes[0] & 0xFF) >= 0x80 && (messageBytes[0] & 0xFF) <= 0x8F) {
                            script = "tonnetz.noteOff(" + (int) (messageBytes[0] & 0x0F) + "," + (int) (messageBytes[1] & 0xFF) + ");";
                        }

                        if ((messageBytes[0] & 0xFF) <= 0x78 && (messageBytes[0] & 0xFF) >= 0x7F) {
                            for (int j = 0; j < 16; j++) {
                                script += "tonnetz.allNotesOff(" + j + ");";
                            }
                        }
                        Log.i(this.getClass().toString(), script);

                        final String finalScript = script;

                        if (script.length() > 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    WebView webView = mActivity.findViewById(R.id.webView1);
                                    webView.evaluateJavascript(finalScript,
                                            new ValueCallback<String>() {
                                                @Override
                                                public void onReceiveValue(String reply) {
                                                    Log.d(this.getClass().toString(), "reply = " + reply);
                                                }
                                            });
                                }
                            });
                        }
                    }
                }
            };
            return thread;
        }
    }

}

