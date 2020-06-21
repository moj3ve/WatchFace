package com.vitataf.watchface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.palette.graphics.Palette;

import android.os.Parcel;
import android.os.PowerManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.support.wearable.watchface.decomposition.ComplicationComponent;
import android.support.wearable.watchface.decomposition.FontComponent;
import android.support.wearable.watchface.decomposition.ImageComponent;
import android.support.wearable.watchface.decomposition.NumberComponent;
import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionDrawable;
import android.support.wearable.watchface.decompositionface.DecompositionWatchFaceService;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends DecompositionWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    // Unique IDs for each complication. The settings activity that supports allowing users
    // to select their complication data provider requires numbers to be >= 0.
    public static final int LEFT_COMPLICATION_ID = 100;
    public static final int RIGHT_COMPLICATION_ID = 101;

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID};

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_LONG_TEXT
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE,
                    ComplicationData.TYPE_LONG_TEXT
            }
    };

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config activity.
    public static int getComplicationId(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }


    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config activity.
    public static int[] getSupportedComplicationTypes(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            default:
                return new int[] {};
        }
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    @Override
    protected WatchFaceDecomposition buildDecomposition() {
        DisplayMetrics dm = getApplication().getResources().getDisplayMetrics();
        int ambientOffset = getApplicationContext().getResources()
                .getDimensionPixelOffset(R.dimen.decomposed_ambient_offset);
        int ambientWidth = dm.widthPixels - ambientOffset;
        int ambientHeight = dm.heightPixels - ambientOffset;
        float ambientCenterX = ambientWidth / 2f;
        float ambientCenterY = ambientHeight / 2f;

        // Background
        Bitmap bgBitmap = Bitmap.createBitmap(ambientWidth, ambientHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bgBitmap);
        canvas.drawColor(Color.BLACK);
        {
            Paint paint = new Paint(mTickAndCirclePaint);
            paint.setAntiAlias(false);
            paint.clearShadowLayer();

            float innerTickRadius = ambientCenterX - 10;
            float outerTickRadius = ambientCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(ambientCenterX + innerX, ambientCenterY + innerY,
                        ambientCenterX + outerX, ambientCenterY + outerY, paint);
            }

        }
        Icon bgIcon = Icon.createWithBitmap(bgBitmap);
        ImageComponent bgComponent = new ImageComponent.Builder()
                .setComponentId(0)
                .setZOrder(0)
                .setImage(bgIcon)
                .build();

        // Hour hand
        int hourHeight = (int)(CENTER_GAP_AND_CIRCLE_RADIUS + sHourHandLength);
        Bitmap hourBitmap = Bitmap.createBitmap(
                (int)CENTER_GAP_AND_CIRCLE_RADIUS, hourHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(hourBitmap);
        {
            Paint paint = new Paint(mHourPaint);
            paint.setAntiAlias(false);
            paint.clearShadowLayer();
            canvas.drawLine(
                    canvas.getWidth() / 2f,
                    0f,
                    canvas.getWidth() / 2f,
                    canvas.getHeight(),
                    paint);
        }
        float xOffset = (hourBitmap.getWidth() / 2f) / ambientWidth;
        float yOffset = (float)hourBitmap.getHeight() / ambientHeight;
        RectF offset = new RectF(
                0.5f - xOffset,
                0.5f - yOffset,
                0.5f + xOffset,
                0.5f + yOffset);
        ImageComponent hourComponent = new ImageComponent.Builder(ImageComponent.Builder.HOUR_HAND)
                .setComponentId(1)
                .setZOrder(1)
                .setImage(Icon.createWithBitmap(hourBitmap))
                .setBounds(offset)
                .build();

        // Minute hand
        int minuteHeight = (int)(CENTER_GAP_AND_CIRCLE_RADIUS + sMinuteHandLength);
        Bitmap minuteBitmap = Bitmap.createBitmap(
                (int)CENTER_GAP_AND_CIRCLE_RADIUS, minuteHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(minuteBitmap);
        {
            Paint paint = new Paint(mMinutePaint);
            paint.setAntiAlias(false);
            paint.clearShadowLayer();
            canvas.drawLine(
                    canvas.getWidth() / 2f,
                    0f,
                    canvas.getWidth() / 2f,
                    canvas.getHeight(),
                    paint);
        }
        xOffset = (minuteBitmap.getWidth() / 2f) / ambientWidth;
        yOffset = (float)minuteBitmap.getHeight() / ambientHeight;
        offset = new RectF(
                0.5f - xOffset,
                0.5f - yOffset,
                0.5f + xOffset,
                0.5f + yOffset);
        ImageComponent minuteComponent = new ImageComponent.Builder(
                ImageComponent.Builder.MINUTE_HAND)
                .setComponentId(2)
                .setZOrder(2)
                .setImage(Icon.createWithBitmap(minuteBitmap))
                .setBounds(offset)
                .build();

        // Second hand
        int secondHeight = (int)(CENTER_GAP_AND_CIRCLE_RADIUS + mSecondHandLength);
        Bitmap secondBitmap = Bitmap.createBitmap(
                (int)CENTER_GAP_AND_CIRCLE_RADIUS, secondHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(secondBitmap);
        {
            Paint paint = new Paint(mSecondPaint);
//            paint.setAntiAlias(false);
            paint.clearShadowLayer();
            canvas.drawLine(
                    canvas.getWidth() / 2f,
                    0f,
                    canvas.getWidth() / 2f,
                    canvas.getHeight(),
                    paint);
        }
        xOffset = (secondBitmap.getWidth() / 2f) / ambientWidth;
        yOffset = (float)secondBitmap.getHeight() / ambientHeight;
        offset = new RectF(
                0.5f - xOffset,
                0.5f - yOffset,
                0.5f + xOffset,
                0.5f + yOffset);
        ImageComponent secondComponent = new ImageComponent.Builder(
                ImageComponent.Builder.TICKING_SECOND_HAND)
                .setComponentId(3)
                .setZOrder(3)
                .setImage(Icon.createWithBitmap(secondBitmap))
                .setBounds(offset)
                .build();

        Icon font = Icon.createWithResource(getApplicationContext(), R.drawable.rounded_semibold_8);
        FontComponent fontComponent = new FontComponent.Builder()
                .setImage(font)
                .setComponentId(6)
                .setDigitCount(10)
                .build();
        Drawable fontDrawable = font.loadDrawable(getApplicationContext());
        float minuteStartX = dm.widthPixels / 2f - fontDrawable.getMinimumWidth();
        float minuteEndX = dm.widthPixels / 2f + fontDrawable.getMinimumWidth();
        float clockStartY = dm.widthPixels / 2f - fontDrawable.getMinimumHeight() / 2 / 10;
        float clockEndY = dm.widthPixels / 2f + fontDrawable.getMinimumHeight() / 2 / 10;
        NumberComponent minuteDigit = new NumberComponent.Builder(NumberComponent.Builder.MINUTES)
                .setComponentId(7)
                .setFontComponent(fontComponent)
                .setZOrder(1)
                .setPosition(new PointF(minuteStartX / dm.widthPixels, clockStartY / dm.widthPixels))
                .build();

        Icon colon = Icon.createWithResource(getApplicationContext(), R.drawable.colon_8);
        int colonWidth = colon.loadDrawable(getApplicationContext()).getMinimumWidth();
        float leftColonStartX = minuteStartX - colonWidth;
        RectF leftColonBounds = new RectF(
                leftColonStartX / dm.widthPixels,
                clockStartY / dm.widthPixels,
                minuteStartX / dm.widthPixels,
                clockEndY / dm.widthPixels);
        ImageComponent colonComponent = new ImageComponent.Builder()
                .setComponentId(8)
                .setZOrder(1)
                .setImage(colon)
                .setBounds(leftColonBounds)
                .build();

        float hourStartX = leftColonStartX - (fontDrawable.getMinimumWidth() * 2);
        NumberComponent hourDigit = new NumberComponent.Builder(NumberComponent.Builder.HOURS_12)
                .setComponentId(9)
                .setFontComponent(fontComponent)
                .setZOrder(2)
                .setPosition(new PointF(hourStartX / dm.widthPixels, clockStartY / dm.widthPixels))
                .setMinDigitsShown(2)
                .build();

        Icon blockingFont = Icon.createWithResource(getApplicationContext(), R.drawable.rounded_semibold_short_8);
        FontComponent blockingFontComponent = new FontComponent.Builder()
                .setImage(blockingFont)
                .setComponentId(10)
                .setDigitCount(3)
                .build();
        NumberComponent blockingDigit = new NumberComponent.Builder(NumberComponent.Builder.HOURS_12)
                .setComponentId(11)
                .setFontComponent(blockingFontComponent)
                .setMsPerIncrement(3 * TimeUnit.HOURS.toMillis(1L))
                .setZOrder(3)
                .setPosition(new PointF(hourStartX / dm.widthPixels, clockStartY / dm.widthPixels))
                .setLowestValue(0L)
                .setHighestValue(2L)
                .build();

        font = Icon.createWithResource(getApplicationContext(), R.drawable.days_8);
        FontComponent daysFontComponent = new FontComponent.Builder()
                .setImage(font)
                .setComponentId(12)
                .setDigitCount(7)
                .build();
        NumberComponent dayComponent = new NumberComponent.Builder(NumberComponent.Builder.DAY_OF_WEEK)
                .setComponentId(13)
                .setFontComponent(daysFontComponent)
                .setZOrder(2)
                .setPosition(new PointF(0.6F, 0.5F))
                .build();

        // Center circle
        Bitmap circleBitmap = Bitmap.createBitmap(
                (int)CENTER_GAP_AND_CIRCLE_RADIUS * 4,
                (int)CENTER_GAP_AND_CIRCLE_RADIUS * 4,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(circleBitmap);
        {
            Paint paint = new Paint(mTickAndCirclePaint);
            paint.setAntiAlias(false);
            paint.clearShadowLayer();
            Paint innerPaint = new Paint(paint);
            innerPaint.setColor(Color.BLACK);
            innerPaint.setStyle(Paint.Style.FILL);
            canvas.translate(canvas.getWidth() / 2f, canvas.getHeight() / 2f);
            canvas.drawCircle(0, 0, CENTER_GAP_AND_CIRCLE_RADIUS, innerPaint);
            canvas.drawCircle(0, 0, CENTER_GAP_AND_CIRCLE_RADIUS, paint);
        }
        xOffset = circleBitmap.getWidth() / (ambientWidth * 2f);
        yOffset = circleBitmap.getHeight() / (ambientHeight * 2f);
        offset = new RectF(
                0.5f - xOffset,
                0.5f - yOffset,
                0.5f + xOffset,
                0.5f + yOffset);
        ImageComponent circleComponent = new ImageComponent.Builder()
                .setComponentId(4)
                .setZOrder(4)
                .setImage(Icon.createWithBitmap(circleBitmap))
                .setBounds(offset)
                .build();

        // Complication
        xOffset = circleBitmap.getWidth() / (ambientWidth * 2f);
        yOffset = circleBitmap.getHeight() / (ambientHeight * 2f);
        offset = new RectF(
                mLeftComplicationBounds.left / (float)(dm.widthPixels),
                mLeftComplicationBounds.top / (float)(dm.heightPixels),
                mLeftComplicationBounds.right / (float)(dm.widthPixels),
                mLeftComplicationBounds.bottom / (float)(dm.heightPixels));
        ComplicationDrawable cd = mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
        ComplicationComponent cc = new ComplicationComponent.Builder()
                .setWatchFaceComplicationId(LEFT_COMPLICATION_ID)
                .setComplicationDrawable(cd)
                .setComponentId(5)
                .setZOrder(1)
                .setComplicationTypes(COMPLICATION_SUPPORTED_TYPES[0])
                .setBounds(offset)
                .build();



        System.out.println("cd.getBounds():  " + cc.getBounds());
        System.out.println("offset: " + offset);


        return new WatchFaceDecomposition.Builder().addImageComponents(
                bgComponent, hourComponent, minuteComponent, secondComponent, circleComponent)
                .addImageComponents(colonComponent)
                .addComplicationComponents(cc)
                .addFontComponents(fontComponent, blockingFontComponent, daysFontComponent)
                .addNumberComponents(minuteDigit, hourDigit, blockingDigit, dayComponent)
                .build();
    }

    @Override
    public Engine onCreateEngine() {
        mCalendar = Calendar.getInstance();

        // Used throughout watch face to pull user's preferences.
        Context context = getApplicationContext();
        mSharedPref =
                context.getSharedPreferences(
                        getString(R.string.analog_complication_preference_file_key),
                        Context.MODE_PRIVATE);

        mCalendar = Calendar.getInstance();

        initializeComplicationsAndBackground();
        initializeWatchFace();
        initializeBackground();
        updateWatchHandStyle();

        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private void initializeComplicationsAndBackground() {
        mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        // Creates a ComplicationDrawable for each location where the user can render a
        // complication on the watch face. In this watch face, we create one for left, right,
        // and background, but you could add many more.
        ComplicationDrawable leftComplicationDrawable =
                new ComplicationDrawable(getApplicationContext());

        ComplicationDrawable rightComplicationDrawable =
                new ComplicationDrawable(getApplicationContext());

        // Adds new complications to a SparseArray to simplify setting styles and ambient
        // properties for all complications, i.e., iterate over them all.
        mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
        mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);

        setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);
    }


    /* Sets active/ambient mode colors for all complications.
     *
     * Note: With the rest of the watch face, we update the paint colors based on
     * ambient/active mode callbacks, but because the ComplicationDrawable handles
     * the active/ambient colors, we only set the colors twice. Once at initialization and
     * again if the user changes the highlight color via AnalogComplicationConfigActivity.
     */
    private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
        int complicationId;
        ComplicationDrawable complicationDrawable;

        for (int id : COMPLICATION_IDS) {
            complicationId = id;
            complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

            // Active mode colors.
            complicationDrawable.setBorderColorActive(primaryComplicationColor);
            complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);

            // Ambient mode colors.
            complicationDrawable.setBorderColorAmbient(Color.RED);
            complicationDrawable.setRangedValuePrimaryColorAmbient(Color.RED);
        }
    }

    private void initializeBackground() {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

        /* Extracts colors from background image to improve watchface style. */
        Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                if (palette != null) {
                    mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
                    mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
                    mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                    updateWatchHandStyle();
                }
            }
        });
    }

    private void initializeWatchFace() {
        /* Set defaults for colors */
        mWatchHandColor = Color.WHITE;
        mWatchHandHighlightColor = Color.RED;
        mWatchHandShadowColor = Color.BLACK;

        mHourPaint = new Paint();
        mHourPaint.setColor(mWatchHandColor);
        mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
        mHourPaint.setAntiAlias(true);
        mHourPaint.setStrokeCap(Paint.Cap.ROUND);
        mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        mMinutePaint = new Paint();
        mMinutePaint.setColor(mWatchHandColor);
        mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
        mMinutePaint.setAntiAlias(true);
        mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
        mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        mSecondPaint = new Paint();
        mSecondPaint.setColor(mWatchHandHighlightColor);
        mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
        mSecondPaint.setAntiAlias(true);
        mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
        mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        mTickAndCirclePaint = new Paint();
        mTickAndCirclePaint.setColor(mWatchHandColor);
        mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
        mTickAndCirclePaint.setAntiAlias(true);
        mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
        mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        DisplayMetrics dm = getApplication().getResources().getDisplayMetrics();

        /*
         * Find the coordinates of the center point on the screen, and ignore the window
         * insets, so that, on round watches with a "chin", the watch face is centered on the
         * entire screen, not just the usable portion.
         */
        mCenterX = dm.widthPixels / 2f;
        mCenterY = dm.heightPixels / 2f;

        /*
         * Calculate lengths of different hands based on watch screen size.
         */
        mSecondHandLength = (float) (mCenterX * 0.875);
        sMinuteHandLength = (float) (mCenterX * 0.75);
        sHourHandLength = (float) (mCenterX * 0.5);

        /*
         * Calculates location bounds for right and left circular complications. Please note,
         * we are not demonstrating a long text complication in this watch face.
         *
         * We suggest using at least 1/4 of the screen width for circular (or squared)
         * complications and 2/3 of the screen width for wide rectangular complications for
         * better readability.
         */

        // For most Wear devices, width and height are the same, so we just chose one (width).
        int sizeOfComplication = dm.widthPixels / 4;
        int midpointOfScreen = dm.widthPixels / 2;

        int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
        int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

        mLeftComplicationBounds =
                // Left, Top, Right, Bottom
                new Rect(
                        horizontalOffset,
                        verticalOffset,
                        (horizontalOffset + sizeOfComplication),
                        (verticalOffset + sizeOfComplication));

        ComplicationDrawable leftComplicationDrawable =
                mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
        leftComplicationDrawable.setBounds(mLeftComplicationBounds);

        mRightComplicationBounds =
                // Left, Top, Right, Bottom
                new Rect(
                        (midpointOfScreen + horizontalOffset),
                        verticalOffset,
                        (midpointOfScreen + horizontalOffset + sizeOfComplication),
                        (verticalOffset + sizeOfComplication));

        ComplicationDrawable rightComplicationDrawable =
                mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
        rightComplicationDrawable.setBounds(mRightComplicationBounds);
    }

    private void updateWatchHandStyle() {
        if (mAmbient) {
            mHourPaint.setColor(Color.WHITE);
            mMinutePaint.setColor(Color.WHITE);
            mSecondPaint.setColor(Color.WHITE);
            mTickAndCirclePaint.setColor(Color.WHITE);

            mHourPaint.setAntiAlias(false);
            mMinutePaint.setAntiAlias(false);
            mSecondPaint.setAntiAlias(false);
            mTickAndCirclePaint.setAntiAlias(false);

            mHourPaint.clearShadowLayer();
            mMinutePaint.clearShadowLayer();
            mSecondPaint.clearShadowLayer();
            mTickAndCirclePaint.clearShadowLayer();

        } else {
            mHourPaint.setColor(mWatchHandColor);
            mMinutePaint.setColor(mWatchHandColor);
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mTickAndCirclePaint.setColor(mWatchHandColor);

            mHourPaint.setAntiAlias(true);
            mMinutePaint.setAntiAlias(true);
            mSecondPaint.setAntiAlias(true);
            mTickAndCirclePaint.setAntiAlias(true);

            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }
    }


    private static final float HOUR_STROKE_WIDTH = 5f;
    private static final float MINUTE_STROKE_WIDTH = 3f;
    private static final float SECOND_TICK_STROKE_WIDTH = 2f;

    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

    private static final int SHADOW_RADIUS = 6;

    private Calendar mCalendar;
    private boolean mRegisteredTimeZoneReceiver = false;
    private boolean mMuteMode;
    private float mCenterX;
    private float mCenterY;
    private float mSecondHandLength;
    private float sMinuteHandLength;
    private float sHourHandLength;
    /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
    private int mWatchHandColor;
    private int mWatchHandHighlightColor;
    private int mWatchHandShadowColor;
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mSecondPaint;
    private Paint mTickAndCirclePaint;
    private Paint mBackgroundPaint;
    private Bitmap mBackgroundBitmap;
    private Bitmap mGrayBackgroundBitmap;
    private boolean mAmbient;
    private boolean mLowBitAmbient;
    private boolean mBurnInProtection;

    private Rect mLeftComplicationBounds;
    private Rect mRightComplicationBounds;


    /* Maps active complication ids to the data for that complication. Note: Data will only be
     * present if the user has chosen a provider via the settings activity for the watch face.
     */
    private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

    /* Maps complication ids to corresponding ComplicationDrawable that renders the
     * the complication data on the watch face.
     */
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    // Used to pull user's preferences for background color, highlight color, and visual
    // indicating there are unread notifications.
    SharedPreferences mSharedPref;


    private class Engine extends DecompositionWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            //super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());
            @SuppressLint({"InvalidWakeLockTag", "WrongConstant"}) PowerManager.WakeLock ambientUpdateWakelock = ((PowerManager)MyWatchFace.this.getSystemService("power")).newWakeLock(1, "WatchFaceService[AmbientUpdate]");
            try {
                Field f = WatchFaceService.Engine.class.getDeclaredField("mAmbientUpdateWakelock");
                f.setAccessible(true);
                f.set(this, ambientUpdateWakelock);
                Field f1 = DecompositionWatchFaceService.Engine.class.getDeclaredField("decompositionDrawable");
                Field f2 = DecompositionWatchFaceService.Engine.class.getDeclaredField("decomposition");
                Field f3 = DecompositionWatchFaceService.Engine.class.getDeclaredField("drawableCallback");
                Field f4 = DecompositionWatchFaceService.Engine.class.getDeclaredField("stepIntervalMs");
                Method m1 = DecompositionWatchFaceService.Engine.class.getDeclaredMethod("setDefaultsAndActivateComplications");
                f1.setAccessible(true);
                f2.setAccessible(true);
                f3.setAccessible(true);
                f4.setAccessible(true);
                m1.setAccessible(true);
                WatchFaceDecomposition decomposition = MyWatchFace.this.buildDecomposition();
                Drawable.Callback drawableCallback = (Drawable.Callback) f3.get(this);
                DecompositionDrawable dd = new CustomDecompositionDrawable(getApplicationContext(),  mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID));
                dd.setDecomposition(decomposition, false);
                dd.setCallback(drawableCallback);
                f1.set(this, dd);
                f2.set(this, decomposition);
                f4.set(this, DecompositionDrawable.calculateStepIntervalMs(decomposition, 0.3F));
                m1.invoke(this);
                updateDecomposition(decomposition);
            } catch (Exception e) {
                e.printStackTrace();
            }


            updateDecompositionDrawable();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }


        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int complicationId : COMPLICATION_IDS) {
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:

                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y);

                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            long now = System.currentTimeMillis();
            //drawComplications(canvas, now);
            if (isVisible() && false) {
                mCalendar.setTimeInMillis(now);

                drawBackground(canvas);
                drawComplications(canvas, now);
                drawWatchFace(canvas);
            }
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int id : COMPLICATION_IDS) {
                complicationId = id;
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawWatchFace(Canvas canvas) {

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();
        }

        private void updateDecompositionDrawable() {

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            System.out.println("onVisibilityChanged: " + visible);

            if (visible) {
                setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);

                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
