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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import java.nio.CharBuffer;
import java.util.BitSet;
import java.util.Vector;

public class MainActivity extends ActionBarActivity {
    // buffer size must be <= recorder buffer size
    static short[] string_to_bits(String s){
        short[] bits = new short[s.length()*8];
        for(int i = 0; i < s.length(); ++i){
            char c = s.charAt(i);
            for(int j = 0; j < 8; ++j){
                bits[i*8+j] = (short)((c>>(7-j))&1);
            }
        }
        return bits;
    }
    static String bits_to_string(short[] bits){
        String s = "";
        for(int i = 0; i < bits.length/8; ++i){
            char c = 0;
            for(int j = 0; j < 8; ++j){
                if(bits[i*8+j] == 1) c = (char)(c+(1<<(7-j)));
            }
            s += c;
        }
        return s;
    }

    public static int TRANSMIT_HZ = 44100;
    private static final int RECORDER_BUFFER_SIZE = 5*44100;

    static String secret = "HaHA";
    static short[] secret_bits = string_to_bits(secret);

    private static final int SAMPLES_PER_BIT = 74;
    private static final int BITS_PER_READ = 10*secret_bits.length;
    private static final int BITS_PER_BUFF = BITS_PER_READ*3;


    private static final int BUFFER_SIZE = SAMPLES_PER_BIT*BITS_PER_BUFF;
    private static final int READ_SIZE = SAMPLES_PER_BIT*BITS_PER_READ;

    private static final int MARK_HZ = 1200;
    private static final int SPACE_HZ = 2400;
    int read_left = -1;

    private static final float MARK_CROSS = 2.0f*MARK_HZ*SAMPLES_PER_BIT/TRANSMIT_HZ;
    private static final float SPACE_CROSS = 2.0f*SPACE_HZ*SAMPLES_PER_BIT/TRANSMIT_HZ;


    public int PREAMBLE_LENGTH=4000;
    public double THRESHOLD = 10.0;
    private short[] buffer = new short[BUFFER_SIZE];
    private short[] demod = new short[BUFFER_SIZE];
    private short[] decode = new short[BITS_PER_READ + secret_bits.length];
    private short[] received_bits;

    private int index = 0;
    private int read_index = -1;

    public double get_freq(int note){
        return Math.pow(2.0, (note)/12.0) * 440.0;
    }

    AudioRecord recorder;
    DrawView drawView;
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Mark cross", String.valueOf(MARK_CROSS));
        Log.v("Space cross", String.valueOf(SPACE_CROSS));

        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_main);

        final MainActivity that = this;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                   TRANSMIT_HZ, AudioFormat.CHANNEL_IN_MONO,
                                   AudioFormat.ENCODING_PCM_16BIT,
                                   Math.max(RECORDER_BUFFER_SIZE,
                                           AudioRecord.getMinBufferSize(TRANSMIT_HZ, AudioFormat.CHANNEL_IN_MONO,
                                                   AudioFormat.ENCODING_PCM_16BIT)));

        
        final TextView text = (TextView) findViewById(R.id.transmissionText);
        this.text = text;
        text.setText("Hello!");

        drawView = (DrawView) findViewById(R.id.draw);
        drawView.setBackgroundColor(Color.WHITE);
        drawView.setShorts(buffer, demod);

        new Thread(new Runnable(){
                    public void run(){
                        that.listen(text);
                    }
                }).start();

    }

    void find_secret(){
        read_left = -1;
        Vector<Integer> offsets = new Vector<Integer>();

        for(int offset = 0; offset < SAMPLES_PER_BIT; offset += SAMPLES_PER_BIT/30){
            int start_index = index - READ_SIZE - secret_bits.length + offset;
            for(int i = 0; i < BITS_PER_READ+secret_bits.length; ++i){
                decode[i] = decode(start_index+i*SAMPLES_PER_BIT);
            }

            for(int i = 0; i < decode.length - secret_bits.length; ++i){
                int match = 1;
                for(int j = 0; j < secret_bits.length; ++j){
                    if(decode[i+j] != secret_bits[j]){
                        match = 0;
                        break;
                    }
                }
                if (match==1){
                    offsets.add(i*SAMPLES_PER_BIT+start_index);
                }
            }
        }
        if(offsets.size() > 0) {
            read_index = (offsets.get(offsets.size()/2) + secret_bits.length*SAMPLES_PER_BIT + BUFFER_SIZE)%BUFFER_SIZE;
        }
    }

    void read_header(){
        String s = "";
        for(int i = 0; i < 8*4; ++i){
            short bit = decode(read_index);
            read_index += SAMPLES_PER_BIT;
            if(bit == 1)s += "1";
            else s +="0";
        }
        read_left = Integer.parseInt(s, 2)*8;
        Log.v("Message Length", String.valueOf(read_left));
        received_bits = new short[read_left];
    }
    void read_message(){
        int count = 0;
        while(count < BITS_PER_READ && read_left > 0){
            received_bits[received_bits.length-read_left] = decode(read_index);
            read_index = (read_index + SAMPLES_PER_BIT)%buffer.length;
            --read_left;
            ++count;
        }
        if(read_left == 0){
            read_index = -1;
            Log.v("Message!!:", bits_to_string(received_bits));
        }
    }

    private void poll() {
        int space = Math.min(buffer.length - index, READ_SIZE);
        recorder.read(buffer, index, space);
        index = (index + space)%buffer.length;

        if(read_index < 0) find_secret();
        else if(read_left <= 0) read_header();
        else read_message();

        drawView.postInvalidate();
    }

    private short decode(int index)
    {
        int count = 0;
        for(int i = index; i < index+SAMPLES_PER_BIT; ++i){
            if((buffer[(i-1+2*buffer.length)%buffer.length]<0) != (buffer[(i+2*buffer.length)%buffer.length]<0)) ++count;
        }
        if(Math.abs(count-MARK_CROSS) < Math.abs(count-SPACE_CROSS)){
            return 1;
        }else{
            return 0;
        }
    }

    private void listen(TextView text) {
        int preambleIndex = -1;
        recorder.startRecording();
        while (preambleIndex < 0 || true) {
            // spin while looking for the preamble
            poll();
            //preambleIndex = findPreamble();
            //Log.v("Found Preamble", String.valueOf(preambleIndex));
        }
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
