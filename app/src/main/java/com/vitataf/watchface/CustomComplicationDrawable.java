package com.vitataf.watchface;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcel;
import android.support.wearable.complications.rendering.ComplicationDrawable;

import java.lang.reflect.Constructor;

public class CustomComplicationDrawable extends ComplicationDrawable {
    public static final Creator<CustomComplicationDrawable> CREATOR = new Creator<CustomComplicationDrawable>() {
        public CustomComplicationDrawable createFromParcel(Parcel source) {
            return new CustomComplicationDrawable(source);
        }

        public CustomComplicationDrawable[] newArray(int size) {
            return new CustomComplicationDrawable[size];
        }
    };

    private CustomComplicationDrawable(Parcel in) {
        try {
            Constructor<ComplicationDrawable> constructor = ComplicationDrawable.class.getDeclaredConstructor(Parcel.class);
            constructor.setAccessible(true);
            constructor.newInstance(in);
        } catch (Exception ignored) {

        }
    }

    public CustomComplicationDrawable(Context context) {
        super(context);
    }

    @Override
    public void draw(Canvas canvas) {

    }
}
