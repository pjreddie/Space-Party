package com.dougwoos.spaceparty_receiver.spaceparty_receiver;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Random;


public class MainActivity extends ActionBarActivity {
    DrawView drawView;
    TextView text;
    Thread recordThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_main);

        final MainActivity that = this;

        final TextView text = (TextView) findViewById(R.id.transmissionText);
        this.text = text;
        text.setText("Hello!");

        drawView = (DrawView) findViewById(R.id.draw);
        drawView.setBackgroundColor(Color.WHITE);

        recordThread = new Thread(new Runnable(){
                    public void run(){
                        new Listener(text).listen(drawView);
                    }
                });
        recordThread.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onDestroy() {
        Log.v("Destroy called", "hooray");
        recordThread.interrupt();
        try {
            recordThread.join();
        } catch (InterruptedException e) {

        }
        Log.v("killed stuff", "really exiting");
        super.onDestroy();
    }

}
