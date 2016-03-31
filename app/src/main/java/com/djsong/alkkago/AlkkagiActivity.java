package com.djsong.alkkago;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AlkkagiActivity extends ActionBarActivity {

    Button mMainMenuBn;

    AKGPlayView mAlkkagiView;
    AKGPlayWorld mAlkkagiWorld;

    private float SelectedTrainingTimeSec = 0.0f; // From main menu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alkkagi);

        // Alkkagi activity 를 위한 view 설정
        LinearLayout AlkkagiLayout = (LinearLayout) findViewById(R.id.AlkkagiLayout);
        mAlkkagiView = new AKGPlayView(this, this); // What's going on here.. kkkk
        mAlkkagiWorld = mAlkkagiView.mPlayWorld;

        //mAlkkagiView.setPadding(0,0,0,0);
        AlkkagiLayout.addView(mAlkkagiView);


        RetrieveDataFromIntent();

        // Initial kick off at AlkkagiWorld
        mAlkkagiWorld.SetAITrainingLengthInSec(SelectedTrainingTimeSec);
        if(SelectedTrainingTimeSec > 0.0f) {
            mAlkkagiWorld.StartAITraining();
        }else{
            mAlkkagiWorld.StartHumanAIMatch();
        }

        mMainMenuBn = (Button)findViewById(R.id.return_to_main_menu_btn);

        mMainMenuBn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReturnToMainMenu();
            }
        });
    }

    public void ReturnToMainMenu(){
        Intent ReturnIntent = new Intent();
        // If we need to send some data back to the main activity..
        //ReturnIntent.putExtra("SomeInfoKey", "SomeInfoContent");
        setResult(RESULT_OK, ReturnIntent);
        finish();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        mAlkkagiView.StopRenderingThread();
        mAlkkagiWorld.StopPhysicsThread();
        mAlkkagiWorld.StopAIThread();
    }

    @Override
    protected void onPause(){
        super.onPause();

        mAlkkagiView.StopRenderingThread();
        mAlkkagiWorld.StopPhysicsThread();
        mAlkkagiWorld.StopAIThread();
    }

    @Override
    protected void onResume(){
        super.onResume();

        mAlkkagiView.StartRenderingThread();
        mAlkkagiWorld.StartPhysicsThread();
        mAlkkagiWorld.StartAIThread();
    }


    private void RetrieveDataFromIntent()
    {
        Intent IntentFromMain = getIntent();
        Bundle ExtraBundle = IntentFromMain.getExtras();

        SelectedTrainingTimeSec = ExtraBundle.getFloat(MainActivity.INTENT_KEY_TrainingTimeSec);

        // Just temporary
        Toast.makeText(getApplicationContext(), "Training for " + SelectedTrainingTimeSec + "sec", Toast.LENGTH_LONG).show();

    }
}
