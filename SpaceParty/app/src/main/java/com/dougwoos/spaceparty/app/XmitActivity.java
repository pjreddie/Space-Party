package com.dougwoos.spaceparty.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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

    public short[] bell202_modulate(byte[] data, int samplesperbit){
        double markf = 1200;
        double spacef = 2200;
        short[] wave = new short[data.length*4*samplesperbit];
        int t = 0;
        for(int i = 0; i < data.length; ++i){
            byte b = data[i];
            for(int j = 0; j < 4; ++j){
                int bit = (b >> j) & 1;
                for(int k = 0; k < samplesperbit; ++k){
                    wave[t++] = (short)(Math.sin(1)*Short.MAX_VALUE);
                }
            }
        }
        return wave;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmit);
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
