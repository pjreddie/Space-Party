package com.dougwoos.spaceparty.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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


public class XmitActivity extends ActionBarActivity {
    public byte[] encode(String message){
        byte[] data = new byte[message.length() + 4];
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(message.length());
        System.arraycopy(b.array(),0,data,0,4);
        System.arraycopy(message.getBytes(), 0, data, 4, message.length());
        return data;
    }

    public short[] bell202_modulate(byte[] data, int samplesperbit, int hz){
        double mark_hz = 1200;
        double space_hz = 2200;
        short[] wave = new short[data.length*4*samplesperbit];
        double t = 0;
        int count = 0;
        for(int i = 0; i < data.length; ++i){
            byte b = data[i];
            for(int j = 0; j < 4; ++j){
                int bit = (b >> j) & 1;
                for(int k = 0; k < samplesperbit; ++k){
                    double step = 1./hz;
                    if(bit == 1) step *= mark_hz;
                    else step *= space_hz;
                    t += step;
                    wave[count++] = (short)(Math.sin(t*Math.PI*2)*Short.MAX_VALUE);
                }
            }
        }
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
                short wave[] = bell202_modulate(code, 2200, 44100);
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        wave.length,
                        AudioTrack.MODE_STREAM);
                audioTrack.play();
                audioTrack.write(wave, 0, wave.length);
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
