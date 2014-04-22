package com.dougwoos.spaceparty_receiver.spaceparty_receiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.EditText;
import java.util.Arrays;


public class MainActivity extends ActionBarActivity {
    // buffer size must be <= recorder buffer size
    private static final int RECORDER_BUFFER_SIZE = 44100;
    private static final int BUFFER_SIZE = 3000;

    public int TRANSMIT_HZ = 44100;
    public int PREAMBLE_LENGTH=1000;
    public double THRESHOLD = 100;
    private short[] buffer1 = new short[BUFFER_SIZE];
    private short[] buffer2 = new short[BUFFER_SIZE];
    private boolean lastRead1 = false;
    private boolean neverRead = true;

    public double get_freq(int note){
        return Math.pow(2.0, (note)/12.0) * 440.0;
    }
    public short[] generate_preamble(){
        int notes[] = {20,22,26,28};
        short[] wave = new short[PREAMBLE_LENGTH];
        double t = 0;
        int count = 0;
        for(int i = 0; i < PREAMBLE_LENGTH; ++i){
            int note = notes[notes.length*i/PREAMBLE_LENGTH];

            wave[count++] = (short)(Math.sin(t*Math.PI*2)*Short.MAX_VALUE);
            t += get_freq(note)/TRANSMIT_HZ;
        }
        return wave;
    }
    public short[] preamble = generate_preamble();

    AudioRecord recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                   44100, AudioFormat.CHANNEL_IN_MONO,
                                   AudioFormat.ENCODING_PCM_16BIT,
                                   Math.max(RECORDER_BUFFER_SIZE,
                                           AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                                                   AudioFormat.ENCODING_PCM_16BIT)));

        
        Button button = (Button) findViewById(R.id.button);
        final TextView text = (TextView) findViewById(R.id.transmissionText);
        button.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0){
                listen(text);
            }
        });

    }

    private void poll() {
        // special-case neverRead
        if (neverRead) {
            recorder.read(buffer1, 0, BUFFER_SIZE);
            recorder.read(buffer2, 0, BUFFER_SIZE);
            neverRead = false;
            return;
        }
        if (!lastRead1) {
            recorder.read(buffer1, 0, BUFFER_SIZE);
        } else {
            recorder.read(buffer2, 0, BUFFER_SIZE);
        }
        lastRead1 = !lastRead1;
    }

    private int findPreamble() {
        short buffer[] = readFrom(0, BUFFER_SIZE*2);
        double max_sum = THRESHOLD;
        int max_i = -1;

        for(int i = 0; i < buffer.length-preamble.length; ++i){
            double sum = 0;
            for(int j = 0; j < preamble.length; ++j){
                sum += preamble[j]*buffer[i+j];
            }
            if(sum > max_sum){
                max_sum = sum;
                max_i = i;
            }
        }
        return max_i;
    }

    private short[] readFrom(int start, int size) {
        short[] result = new short[size];
        short[] buffer;
        int amountRead = 0;
        while (amountRead < size) {
            if (start >= BUFFER_SIZE) {
                poll();
                start -= BUFFER_SIZE;
                continue;
            }
            buffer = lastRead1? buffer2 : buffer1;
            int toRead = Math.min(size, BUFFER_SIZE - start);
            System.arraycopy(buffer, start, result, amountRead, toRead);
            amountRead += toRead;
            start += toRead;
        }
        return result;
    }

    private void listen(TextView text) {
        int preambleIndex = -1;
        recorder.startRecording();
        while (preambleIndex < 0 || true) {
            // spin while looking for the preamble
            poll();
            preambleIndex = findPreamble();
            Log.v("Found Preamble", String.valueOf(preambleIndex));
        }
        short[] msgLengthBuf = readFrom(preambleIndex, 2);
        int length = (((int)msgLengthBuf[0]) << 16) | msgLengthBuf[1];
        short[] transmission = readFrom(preambleIndex + 2, length);
        // cool, now we have a transmission.
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

}
