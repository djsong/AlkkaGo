package com.djsong.alkkago;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends ActionBarActivity {

    Button mAITrainingBn_No;
    Button mAITrainingBn_10Sec;
    Button mAITrainingBn_30Sec;
    Button mAITrainingBn_1Min;
    Button mAITrainingBn_3Min;
    Button mQuitBn;

    public static final String INTENT_KEY_TrainingTimeSec = "AITrainingTimeSec";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAITrainingBn_No = (Button)findViewById(R.id.AI_no_training);
        mAITrainingBn_10Sec = (Button)findViewById(R.id.AI_training_10_sec);
        mAITrainingBn_30Sec = (Button)findViewById(R.id.AI_training_30_sec);
        mAITrainingBn_1Min = (Button)findViewById(R.id.AI_training_1_min);
        mAITrainingBn_3Min = (Button)findViewById(R.id.AI_training_3_min);
        mQuitBn = (Button)findViewById(R.id.QuitApp);

        mAITrainingBn_No.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITrainingBnCBCommon(v, 0.0f);
            }
        });
        mAITrainingBn_10Sec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITrainingBnCBCommon(v, 10.0f);
            }
        });
        mAITrainingBn_30Sec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITrainingBnCBCommon(v, 30.0f);
            }
        });
        mAITrainingBn_1Min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITrainingBnCBCommon(v, 60.0f);
            }
        });
        mAITrainingBn_3Min.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AITrainingBnCBCommon(v, 180.0f);
            }
        });
        mQuitBn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });
    }

    /**
     * Common functionalities for AITrainingButtons
     * */
    private void AITrainingBnCBCommon(View InView, float TrainingTimeSec)
    {
        // Open Alkkagi activity.

        // For any data that need to be transfer
        Intent AlkkagiActIntent = new Intent(getApplicationContext(), AlkkagiActivity.class);
        AlkkagiActIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // A simple means to send some data. Should use the same key string when retrieve the data
        AlkkagiActIntent.putExtra(INTENT_KEY_TrainingTimeSec, TrainingTimeSec);

        // Not sure about request code..
        startActivityForResult(AlkkagiActIntent, 1001);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
