package com.dougwoos.spaceparty_receiver.spaceparty_receiver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class DrawView extends View {
    Paint paint = new Paint();
    short[] wave;
    int w;
    int h;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        this.w = w;
        this.h = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public DrawView(Context context, AttributeSet set){
        super(context, set);
        paint.setColor(Color.BLACK);
    }

    public void setShorts(short[] wave){
        this.wave = wave;
    }

    @Override
    public void onDraw(Canvas canvas){
        float scale_y = h/(2.0f*Short.MAX_VALUE);
        float y = h;
        if(wave==null) return;
        for(int i = 0; i < wave.length; ++i){
            canvas.drawLine(i*w/wave.length, y/2.0f, i*w/wave.length, wave[i]*scale_y+y/2.0f, paint);

        }
    }
}
