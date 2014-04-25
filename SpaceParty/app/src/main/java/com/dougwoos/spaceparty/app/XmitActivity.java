package com.dougwoos.spaceparty.app;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.view.View;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.EditText;

import java.nio.ByteOrder;
import java.util.Arrays;

import java.nio.ByteBuffer;
import java.util.BitSet;


public class XmitActivity extends ActionBarActivity {
    public int TRANSMIT_HZ = 44100;
    public int PREAMBLE_LENGTH=4000;
    public int SAMPLES_PER_BIT=74;
    String secret = "HaHA";


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public byte[] encode(String message){
        byte[] data = new byte[secret.length() + message.length() + 4];
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(message.length());
        System.arraycopy(secret.getBytes(), 0, data, 0, secret.length());
        System.arraycopy(b.array(),0,data,secret.length(),b.array().length);
        System.arraycopy(message.getBytes(), 0, data, 4+secret.length(), message.length());
        return data;
    }
    public double get_freq(int note){
        return Math.pow(2.0, (note)/12.0) * 440.0;
    }
    public short[] generate_preamble(){
        int notes[] = {40,35,47,0,47};
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

    public short[] bell202_modulate(byte[] data){
        short preamble[] = generate_preamble();
        double mark_hz = 1200;
        double space_hz = 2400;
        int min_buff = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        //short[] wave = new short[Math.max(data.length*8*SAMPLES_PER_BIT+preamble.length, min_buff)];
        //System.arraycopy(preamble, 0, wave, 0, preamble.length);
        //int count = preamble.length;

        short[] wave = new short[Math.max(data.length*8*SAMPLES_PER_BIT, min_buff)];
        int count = 0;

        double t = 0;
        String s = "";
        for(int i = 0; i < data.length; ++i){
            byte b = data[i];
            for(int j = 7; j >= 0; --j){
                int bit = (b >> j) & 1;
                if(bit == 1) s = s + "1";
                else s = s + "0";
                for(int k = 0; k < SAMPLES_PER_BIT; ++k){
                    double step = 1./TRANSMIT_HZ;
                    if(bit == 1) step *= mark_hz;
                    else step *= space_hz;
                    t += step;
                    wave[count++] = (short)(Math.sin(t*Math.PI*2)*Short.MAX_VALUE);
                }
            }
        }
        Log.v("Bits", s);
        return wave;
    }
    Button button;
    EditText text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmit);

        button = (Button) findViewById(R.id.button);
        text = (EditText) findViewById(R.id.editText);
        button.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0){
                String m = text.getText().toString();
                byte code[] = encode(m);
                short wave[] = bell202_modulate(code);

                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, TRANSMIT_HZ,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        wave.length,
                        AudioTrack.MODE_STREAM);
                audioTrack.play();
                audioTrack.write(wave, 0, wave.length);
                audioTrack.stop();

                Log.v("EditText", m);
                Log.v("Code", Arrays.toString(code));
                //Log.v("Wave", Arrays.toString(wave));
                Log.v("Wave Size", String.valueOf(wave.length));

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.xmit, menu);
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
