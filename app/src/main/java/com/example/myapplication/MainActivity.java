package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    TextToSpeech mTTS = null;
    private final int ACT_CHECK_TTS_DATA = 1000;
    private final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2000;
    private int permissionCount = 0;
    private String mAudioFilename = "";
    private final String mUtteranceID = "totts";
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText ettext = (EditText)findViewById(R.id.ettext);
        final Button bsay = (Button)findViewById(R.id.bsay);
        bsay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saySomething(ettext.getText().toString().trim(), 1);
            }
        });


        // perform the dynamic permission request
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);

        // Create audio file location
        File sddir = new File(Environment.getExternalStorageDirectory() + "/TutorialOnTTS/");
        sddir.mkdirs();
        mAudioFilename = sddir.getAbsolutePath() + "/" + mUtteranceID + ".wav";

        // Check to see if we have TTS voice data
        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, ACT_CHECK_TTS_DATA);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    public ListView mList;


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    permissionCount++;
            default:
                break;
        }
    }

    private void saveToAudioFile(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTTS.synthesizeToFile(text, null, new File(mAudioFilename), mUtteranceID);
        } else {
            HashMap<String, String> hm = new HashMap();
            hm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, mUtteranceID);
            mTTS.synthesizeToFile(text, hm, mAudioFilename);
        }

        mTTS.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            public void onUtteranceCompleted(String uid) {
                if (uid.equals(mUtteranceID)) {
                    Toast.makeText(MainActivity.this, "Saved to " + mAudioFilename, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saySomething(String text, int qmode) {
        if (qmode == 1)
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACT_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // data exists, so we instantiate the TTS engine
                mTTS = new TextToSpeech(this, this);
            } if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
                // Fill the list view with the strings the recognizer thought it
                // could have heard
                ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mList.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, matches));
                // matches is the result of voice input. It is a list of what the
                // user possibly said.
                // Using an if statement for the keyword you want to use allows the
                // use of any activity if keywords match
                // it is possible to set up multiple keywords to use the same
                // activity so more than one word will allow the user
                // to use the activity (makes it so the user doesn't have to
                // memorize words from a list)
                // to use an activity from the voice input information simply use
                // the following format;
                // if (matches.contains("keyword here") { startActivity(new
                // Intent("name.of.manifest.ACTIVITY")

                if (matches.contains("information")) {
                    saySomething("Recognized", 1);
                }
            } else {
                // data is missing, so we start the TTS installation process
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (mTTS != null) {
                int result = mTTS.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                } else {
                    saySomething("TTS is ready", 0);
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

}
