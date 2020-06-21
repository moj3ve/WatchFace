package com.vitataf.watchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.decompositionface.DecompositionDrawable;
import android.util.SparseArray;

import java.lang.reflect.Field;

import static com.vitataf.watchface.MyWatchFace.LEFT_COMPLICATION_ID;

public class CustomDecompositionDrawable extends DecompositionDrawable {
    ComplicationDrawable mCd;

    CustomDecompositionDrawable(Context context) {
        super(context);
    }

    public CustomDecompositionDrawable(Context context, ComplicationDrawable cd) {
        super(context);
        mCd = cd;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        try {
            Field f = DecompositionDrawable.class.getDeclaredField("complicationDrawables");
            f.setAccessible(true);
            ComplicationDrawable c = ((SparseArray<ComplicationDrawable>)f.get(this)).get(LEFT_COMPLICATION_ID);
            c.draw(canvas, System.currentTimeMillis());
            //mCd.draw(canvas, System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
