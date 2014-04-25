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

    public static int TRANSMIT_HZ = 44100;
    private static final int RECORDER_BUFFER_SIZE = 5*44100;

    static String secret = "UaU";
    static short[] secret_bits = string_to_bits(secret);

    private static final int SAMPLES_PER_BIT = 74;
    private static final int BYTES_PER_READ = 5*secret_bits.length/8;
    private static final int BITS_PER_READ = BYTES_PER_READ*8;
    private static final int BITS_PER_BUFF = BITS_PER_READ*5;

    private static final int BUFFER_SIZE = SAMPLES_PER_BIT*BITS_PER_BUFF;
    private static final int READ_SIZE = SAMPLES_PER_BIT*BITS_PER_READ;

    private static final int MARK_HZ = 1200;
    private static final int SPACE_HZ = 2400;
    int read_left = -1;

    private static final float MARK_CROSS = 2.0f*MARK_HZ*SAMPLES_PER_BIT/TRANSMIT_HZ;
    private static final float SPACE_CROSS = 2.0f*SPACE_HZ*SAMPLES_PER_BIT/TRANSMIT_HZ;


    private short[] buffer = new short[BUFFER_SIZE];
    private short[] decode = new short[BITS_PER_READ + secret_bits.length];
    private byte[] received_bytes;


    private int index = 0;
    private int read_index = -1;

    public byte[] random_bytes(int len){
        Random r = new Random(0);
        byte[] bytes = new byte[len];
        r.nextBytes(bytes);
        return bytes;
    }

    public int random_length = 1000;
    public byte[] random_bytes = random_bytes(random_length);


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
        drawView.setShorts(buffer);

        new Thread(new Runnable(){
                    public void run(){
                        that.listen(text);
                    }
                }).start();

    }

    void find_secret(){
        read_left = -1;
        int sum = 0;
        int count = 0;

        for(int offset = 0; offset < SAMPLES_PER_BIT; offset += SAMPLES_PER_BIT/20){
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
                    int next = i*SAMPLES_PER_BIT + start_index;
                    if(count == 0 || Math.abs(sum/count-next) < SAMPLES_PER_BIT){
                        sum += next;
                        ++count;
                    }
                }
            }
        }
        if(count > 0) {
            Log.v("Count:", String.valueOf(count));
            read_index = (sum/count + secret_bits.length*SAMPLES_PER_BIT + BUFFER_SIZE)%BUFFER_SIZE;
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
        if(s.charAt(0) == '1' || Integer.parseInt(s,2) > 1000000){
            Log.v("Rejecting", "Malformed or Too Long!");
            read_index = -1;
            return;
        }
        read_left = Integer.parseInt(s, 2);
        Log.v("Message Length", String.valueOf(read_left));
        received_bytes = new byte[read_left];
    }

    void read_byte(){
        int byte_index = received_bytes.length - read_left;
        byte b = 0;
        for(int i = 0; i < 8; ++i){
            short bit = decode(read_index);
            read_index = (read_index+SAMPLES_PER_BIT)%buffer.length;
            if(bit == 1) b = (byte)(b|1<<(7-i));
        }
        received_bytes[byte_index] = b;
        --read_left;
    }

    void read_message(){
        int count = 0;
        while(count < BYTES_PER_READ && read_left > 0){
            read_byte();
            ++count;
        }
        if(read_left == 0){
            read_index = -1;
            if(received_bytes.length == random_length){
                int errors = 0;
                for(int i = 0; i < random_length; ++i){
                    int diff = ((received_bytes[i]&0xff)^(random_bytes[i]&0xff));
                    errors += Integer.bitCount(diff);
                }
                Log.v("Error Rate:", String.format("%f%%",100.*errors/(random_length*8.)));
            }else{
                Log.v("Message!!:", new String(received_bytes));
                this.text.setText(new String(received_bytes));
            }
        }
    }

    private void poll() {
        recorder.read(buffer, index, READ_SIZE);
        index = (index + READ_SIZE)%buffer.length;

        if(read_index < 0) find_secret();
        else if(read_left <= 0) read_header();
        else read_message();

        drawView.postInvalidate();
    }
int log = 0;
    private short decode(int index)
    {
        int count = 0;
        for(int i = index; i < index+SAMPLES_PER_BIT; ++i){
            if((buffer[(i-1+2*buffer.length)%buffer.length]<0) != (buffer[(i+2*buffer.length)%buffer.length]<0)) ++count;
        }
        if(++log%100000 == 0){
            Log.v("Count", String.valueOf(count));
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
