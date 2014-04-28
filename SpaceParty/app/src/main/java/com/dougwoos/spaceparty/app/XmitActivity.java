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

import java.util.Arrays;

import java.nio.ByteBuffer;
import java.util.Random;


public class XmitActivity extends ActionBarActivity {
    public int TRANSMIT_HZ = 44100;
    public int SAMPLES_PER_BIT=74;

    String secret = "\0\0\0\0\0\0\0\0\0UaU";

    int min_buff = AudioTrack.getMinBufferSize(TRANSMIT_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);


    public byte[] random_bytes(int len){
        Random r = new Random(0);
        byte[] bytes = new byte[len];
        r.nextBytes(bytes);
        return bytes;
    }

    public int random_length = 1000;
    public byte[] random_bytes = random_bytes(random_length);


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public byte[] encode(byte[] message){
        byte[] data = new byte[secret.length() + message.length + 4];
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(message.length);
        System.arraycopy(secret.getBytes(), 0, data, 0, secret.length());
        System.arraycopy(b.array(),0,data,secret.length(),b.array().length);
        System.arraycopy(message, 0, data, 4+secret.length(), message.length);
        return data;
    }

    public short[] bell202_modulate(byte[] data){
        double mark_hz = 1200;
        double space_hz = 2400;

        short[] wave = new short[data.length*8*SAMPLES_PER_BIT];
        int count = 0;

        double t = 0;
        for(int i = 0; i < data.length; ++i){
            byte b = data[i];
            for(int j = 7; j >= 0; --j){
                int bit = (b >> j) & 1;
                for(int k = 0; k < SAMPLES_PER_BIT; ++k){
                    double step = 1./TRANSMIT_HZ;
                    if(bit == 1) step *= mark_hz;
                    else step *= space_hz;
                    t += step;
                    wave[count++] = (short)(Math.sin(t*Math.PI*2)*Short.MAX_VALUE);
                }
            }
        }
        return wave;
    }
    AudioTrack audioTrack;
    void send_message(byte[] s){
        byte code[] = encode(s);
        short wave[] = bell202_modulate(code);

        audioTrack.play();
        for(int i = 0; i < wave.length; i += 5*min_buff){
            int len = Math.min(wave.length - i, 5*min_buff);
            audioTrack.write(wave, i, len);
        }
        audioTrack.stop();
    }

    Button button;
    EditText text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Samples", String.valueOf(SAMPLES_PER_BIT));
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, TRANSMIT_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                min_buff*10,
                AudioTrack.MODE_STREAM);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmit);
        Log.v("Secret", secret);

        button = (Button) findViewById(R.id.button);
        text = (EditText) findViewById(R.id.editText);
        button.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0){
                String m = text.getText().toString();
                send_message(m.getBytes());
            }
        });

        Button test = (Button) findViewById(R.id.button2);
        test.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0){
                send_message(random_bytes);
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
