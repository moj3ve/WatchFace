package com.vitataf.watchface;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.support.wearable.watchface.decompositionface.DecompositionDrawable;

public class CustomDecompositionDrawable extends DecompositionDrawable {
    CustomDecompositionDrawable(Context context) {
        super(context);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
