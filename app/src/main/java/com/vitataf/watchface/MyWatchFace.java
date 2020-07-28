package com.vitataf.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.decomposition.ComplicationComponent;
import android.support.wearable.watchface.decomposition.FontComponent;
import android.support.wearable.watchface.decomposition.ImageComponent;
import android.support.wearable.watchface.decomposition.NumberComponent;
import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionWatchFaceService;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static android.support.wearable.watchface.decomposition.WatchFaceDecomposition.Component.DISPLAY_AMBIENT;
import static android.support.wearable.watchface.decomposition.WatchFaceDecomposition.Component.DISPLAY_INTERACTIVE;

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
//    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
//    private static final int MSG_UPDATE_TIME = 0;


    // Unique IDs for each complication. The settings activity that supports allowing users
    // to select their complication data provider requires numbers to be >= 0.
    public static final int TOP_COMPLICATION_ID = 0;
    public static final int BOTTOM_COMPLICATION_ID = 1;

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID};

    // Left and right dial supported types.
//    private static final int[][] COMPLICATION_SUPPORTED_TYPES_OLD = {
//            {
//                    ComplicationData.TYPE_RANGED_VALUE,
//                    ComplicationData.TYPE_ICON,
//                    //ComplicationData.TYPE_SHORT_TEXT,
//                    ComplicationData.TYPE_SMALL_IMAGE,
//                    ComplicationData.TYPE_LONG_TEXT
//            },
//            {
//                    ComplicationData.TYPE_RANGED_VALUE,
//                    ComplicationData.TYPE_ICON,
//                    //ComplicationData.TYPE_SHORT_TEXT,
//                    ComplicationData.TYPE_SMALL_IMAGE,
//                    ComplicationData.TYPE_LONG_TEXT
//            }
//    };

    // Top and bottom dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_LONG_TEXT
            },
            {
                    ComplicationData.TYPE_LONG_TEXT
            }
    };

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config activity.
    public static int getComplicationId(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case TOP:
                return TOP_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
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
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[TOP_COMPLICATION_ID];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[BOTTOM_COMPLICATION_ID];
            default:
                return new int[] {};
        }
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    private WeakReference<int[]> colorTable = new WeakReference<>(null);

    // Should only be used in initialization methods
    // Crudely convert ARGB_8888 to ARGB_1332 color space
    private Bitmap get332Bitmap(Bitmap src, boolean transparent) {
        int[] table = colorTable.get();
        if (table == null)
            colorTable = new WeakReference<>(table = rgb888To332());
        Bitmap bm = src.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < src.getWidth(); i++) {
            for (int j = 0; j < src.getHeight(); j++) {
                int color = src.getPixel(i,j);
                int blue = (color >> 6) & 0x3;
                int green = (color >> 13) & 0x7;
                int red = (color >> 21) & 0x7;
                int alpha = color >>> 31;
                int bgr = (red << 5) | (green << 2) | blue;
                if (!transparent)
                    bgr |= alpha << 8;
                color = table[bgr];
                bm.setPixel(i,j,color);
            }
        }
        return bm;
    }

    // Should only be used in initialization methods
    // Crudely convert ARGB_8888 to ARGB_1332 color space
    private Bitmap get332BlueBitmap(Bitmap src) {
        Bitmap bm = src.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < src.getWidth(); i++) {
            for (int j = 0; j < src.getHeight(); j++) {
                int color = src.getPixel(i,j);
                int rgb = color & 0x00FFFFFF;
                if (rgb != 0)
                    rgb = 0x20A0C0;
                bm.setPixel(i,j,rgb);
            }
        }
        return bm;
    }

    private int[] rgb888To332() {
        System.out.println("Running rgb888To332");
        int[] map = new int[512];
        for (int i = 0; i < 512; i++) {
            int blue = i & 0x3;
            double prop = blue / 3f;
            map[i] |= (int)Math.round(255 * prop);

            int green = (i >> 2) & 0x7;
            prop = green / 7f;
            map[i] |= (int)Math.round(255 * prop) << 8;

            int red = (i >> 5) & 0x7;
            prop = red / 7f;
            map[i] |= (int)Math.round(255 * prop) << 16;

            int alpha = i >> 8;
            map[i] |= 0xFF000000 * alpha;
        }
        return map;
    }

    private List<ImageComponent> getBackgroundComponents() {
        List<ImageComponent> components = new ArrayList<>();
        int tickLength = 10;
        int radiusOffset = 0;
        int tickWidth = 5;

        int w = AMBIENT_DISPLAY_WIDTH + 3;

        Bitmap bitmap = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        ImageComponent background = new ImageComponent.Builder()
                .setComponentId(getNewComponentId())
                .setZOrder(0)
                .setImage(Icon.createWithBitmap(bitmap))
                .setBounds(new RectF(0f, 0f, 1f, 1f))
                .build();
        components.add(background);

        bitmap = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        Paint tickPaint = new Paint();
        tickPaint.setStrokeWidth(tickWidth);
        tickPaint.setAntiAlias(true);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        int outerRadius = w / 2 - radiusOffset;
        int innerRadius = outerRadius - tickLength;
        for (int i = 60; i --> 0;) {
            float angle = i * (float)Math.PI / 30;
            float innerX = (float)Math.sin(angle) * innerRadius;
            float innerY = (float)-Math.cos(angle) * innerRadius;
            float outerX = (float)Math.sin(angle) * outerRadius;
            float outerY = (float)-Math.cos(angle) * outerRadius;
            if (i % 5 == 0)
                tickPaint.setColor(Color.WHITE);
            else
                tickPaint.setColor(Color.GRAY);
            canvas.drawLine(w / 2f + innerX, w / 2f + innerY, w / 2f + outerX,
                    w / 2f + outerY, tickPaint);
        }
        ImageComponent ticks = new ImageComponent.Builder()
                .setComponentId(getNewComponentId())
                .setZOrder(3)
                .setImage(Icon.createWithBitmap(get332Bitmap(bitmap, false)))
                .setBounds(new RectF(0f, 0f, 1f, 1f))
                .build();
        components.add(ticks);

        return components;
    }

    private ImageComponent getSecondHandComponent() {
        int centerDia = 10;
        int strokeWidth = 3;
        int tailLength = 20;


        int w = AMBIENT_DISPLAY_WIDTH - 2;

        // Second hand
        int secondHeight = (w - centerDia) / 2;
        Bitmap secondBitmap = Bitmap.createBitmap(
                centerDia + strokeWidth,
                secondHeight + centerDia + tailLength,
                Bitmap.Config.ARGB_8888);
        //secondBitmap = Bitmap.createBitmap(dm.widthPixels, dm.widthPixels, Bitmap.Config.ARGB_8888);

        Paint secondPaint = new Paint();
        secondPaint.setStrokeWidth(strokeWidth);
        secondPaint.setAntiAlias(true);
//        secondPaint.setStrokeCap(Paint.Cap.ROUND);
        secondPaint.setColor(Color.RED);
        secondPaint.setStyle(Paint.Style.STROKE);
//        secondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        Canvas canvas = new Canvas(secondBitmap);
        canvas.drawLine(
                canvas.getWidth() / 2f,
                canvas.getHeight() - tailLength,
                canvas.getWidth() / 2f,
                canvas.getHeight(),
                secondPaint);
        canvas.drawCircle(
                canvas.getWidth() / 2f,
                secondHeight + centerDia / 2f,
                centerDia / 2f,
                secondPaint);
        canvas.drawLine(
                canvas.getWidth() / 2f,
                canvas.getHeight() - tailLength - centerDia,
                canvas.getWidth() / 2f,
                0,
                secondPaint);

        RectF bounds = new RectF(
                0.5f - secondBitmap.getWidth() / 2f / w,
                0.5f - (secondHeight + centerDia / 2f) / w,
                0.5f + secondBitmap.getWidth() / 2f / w,
                0.5f + (tailLength + centerDia / 2f) / w);

        return new ImageComponent.Builder(
                ImageComponent.Builder.TICKING_SECOND_HAND)
                .setComponentId(getNewComponentId())
                .setZOrder(6)
                .setImage(Icon.createWithBitmap(get332Bitmap(secondBitmap, false)))
                .setDisplayModes(DISPLAY_AMBIENT | DISPLAY_INTERACTIVE)
                .setBounds(bounds)
                .setPivot(new PointF(0.5f, 0.5f))
                .build();
    }

    private int mCurrentComponentId = 0;
    private static int AMBIENT_DISPLAY_WIDTH;

    private int getNewComponentId() {
        return mCurrentComponentId++;
    }

    private List<WatchFaceDecomposition.Component> getDigitalComponents() {
        List<WatchFaceDecomposition.Component> components = new ArrayList<>();
        int w = AMBIENT_DISPLAY_WIDTH;

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.rounded_semibold);
        Icon font = Icon.createWithBitmap(get332Bitmap(bm, true));
        FontComponent fontComponent = new FontComponent.Builder()
                .setImage(font)
                .setComponentId(getNewComponentId())
                .setDigitCount(10)
                .build();
        components.add(fontComponent);
        Drawable fontDrawable = font.loadDrawable(getApplicationContext());


        float minuteStartX = w / 2f - fontDrawable.getMinimumWidth() - 50;
//        float minuteEndX = w / 2f + fontDrawable.getMinimumWidth();
        float clockStartY = w / 2f - fontDrawable.getMinimumHeight() / 2f / 10;
        float clockEndY = w / 2f + fontDrawable.getMinimumHeight() / 2f / 10;
        NumberComponent minuteDigit = new NumberComponent.Builder(NumberComponent.Builder.MINUTES)
                .setComponentId(getNewComponentId())
                .setFontComponent(fontComponent)
                .setZOrder(1)
                .setPosition(new PointF(minuteStartX / w, clockStartY / w))
                .build();
        components.add(minuteDigit);

        bm = BitmapFactory.decodeResource(getResources(), R.drawable.colon_8);
        Icon colon = Icon.createWithBitmap(get332Bitmap(bm, true));
        int colonWidth = colon.loadDrawable(getApplicationContext()).getMinimumWidth();
        float leftColonStartX = minuteStartX - colonWidth;
        RectF leftColonBounds = new RectF(
                leftColonStartX / w,
                clockStartY / w,
                minuteStartX / w,
                clockEndY / w);
        ImageComponent colonComponent = new ImageComponent.Builder()
                .setComponentId(getNewComponentId())
                .setZOrder(1)
                .setImage(colon)
                .setBounds(leftColonBounds)
                .build();
        components.add(colonComponent);

        float hourStartX = leftColonStartX - (fontDrawable.getMinimumWidth() * 2);
        NumberComponent hourDigit = new NumberComponent.Builder(NumberComponent.Builder.HOURS_12)
                .setComponentId(getNewComponentId())
                .setFontComponent(fontComponent)
                .setZOrder(1)
                .setPosition(new PointF(hourStartX / w, clockStartY / w))
                .setMinDigitsShown(2)
                .build();
        components.add(hourDigit);

        bm = BitmapFactory.decodeResource(getResources(), R.drawable.rounded_semibold_short);
        Icon blockingFont = Icon.createWithBitmap(get332Bitmap(bm, false));
        FontComponent blockingFontComponent = new FontComponent.Builder()
                .setImage(blockingFont)
                .setComponentId(getNewComponentId())
                .setDigitCount(4)
                .build();
        components.add(blockingFontComponent);

        NumberComponent blockingDigit = new NumberComponent.Builder(NumberComponent.Builder.HOURS_12)
                .setComponentId(getNewComponentId())
                .setFontComponent(blockingFontComponent)
                .setMsPerIncrement(TimeUnit.HOURS.toMillis(3L))
                .setTimeOffsetMs(TimeUnit.HOURS.toMillis(2L))
                .setZOrder(2)
                .setPosition(new PointF(hourStartX / w, clockStartY / w))
                .setLowestValue(0L)
                .setHighestValue(3L)
                .build();
        components.add(blockingDigit);

        return components;
    }

    public List<WatchFaceDecomposition.Component> getDateComponents() {
        List<WatchFaceDecomposition.Component> components = new ArrayList<>();
        int w = AMBIENT_DISPLAY_WIDTH;

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.days_blue);
        float yOffset = 0.5f - bm.getHeight() / 7f / 2f / w;
        float xOffset = 0.03f;
        Icon font = Icon.createWithBitmap(get332BlueBitmap(bm));
        FontComponent daysFontComponent = new FontComponent.Builder()
                .setImage(font)
                .setComponentId(getNewComponentId())
                .setDigitCount(7)
                .build();
        components.add(daysFontComponent);

        NumberComponent dayComponent = new NumberComponent.Builder(NumberComponent.Builder.DAY_OF_WEEK)
                .setComponentId(getNewComponentId())
                .setFontComponent(daysFontComponent)
                .setZOrder(1)
                .setPosition(new PointF(0.49F + xOffset, yOffset))
                .build();
        components.add(dayComponent);

        bm = BitmapFactory.decodeResource(getResources(), R.drawable.rounded_semibold_38);
        font = Icon.createWithBitmap(get332Bitmap(bm, true));
        FontComponent smallFont = new FontComponent.Builder()
                .setImage(font)
                .setComponentId(getNewComponentId())
                .setDigitCount(10)
                .build();
        components.add(smallFont);

        NumberComponent dateComponent = new NumberComponent.Builder(NumberComponent.Builder.DAY_OF_MONTH)
                .setComponentId(getNewComponentId())
                .setFontComponent(smallFont)
                .setZOrder(1)
                .setPosition(new PointF(0.75F + xOffset, yOffset))
                .build();
        components.add(dateComponent);

        return components;
    }

    private List<WatchFaceDecomposition.Component> getComplicationComponents() {
        List<WatchFaceDecomposition.Component> components = new ArrayList<>();
        float w = AMBIENT_DISPLAY_WIDTH;
        int[] textSize = {23, 22};
        int[] titleSize = {35, 25};
        float[] width = {0.7f, 0.9f};
        float[] top = {50 / w, 220 / w};
        float[] height = {0.25f, 0.25f};
        float[] center = {0.5f, 0.45f};

        for (int id : COMPLICATION_IDS) {
            ComplicationDrawable c = mComplicationDrawableSparseArray.get(id);
            c.setBorderStyleAmbient(ComplicationDrawable.BORDER_STYLE_NONE);
            c.setTextTypefaceAmbient(getResources().getFont(R.font.rounded_thin));
            c.setTitleTypefaceAmbient(getResources().getFont(R.font.rounded_thin));
            c.setTextSizeAmbient(textSize[id]);
            c.setTitleSizeAmbient(titleSize[id]);

            RectF bounds = new RectF(
                    center[id] - width[id] / 2f,
                    top[id] + 0,
                    center[id] + width[id] / 2f,
                    top[id] + height[id]);
            ComplicationComponent cc = new ComplicationComponent.Builder()
                    .setWatchFaceComplicationId(id)
                    .setComplicationDrawable(c)
                    .setComponentId(getNewComponentId())
                    .setZOrder(1)
                    .setComplicationTypes(COMPLICATION_SUPPORTED_TYPES[id])
                    .setBounds(bounds)
                    .build();

            components.add(cc);
        }

        int blockingWidth = 100;
        int blockingHeight = 100;
        Bitmap bitmap = Bitmap.createBitmap(blockingWidth, blockingHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        RectF bounds = new RectF(
                center[BOTTOM_COMPLICATION_ID] - width[BOTTOM_COMPLICATION_ID] / 2f,
                top[BOTTOM_COMPLICATION_ID] + 0,
                center[BOTTOM_COMPLICATION_ID] - width[BOTTOM_COMPLICATION_ID] / 2f + blockingWidth / w,
                top[BOTTOM_COMPLICATION_ID] + blockingWidth / w);

        ImageComponent bottomBlockingComponent = new ImageComponent.Builder()
                .setComponentId(getNewComponentId())
                .setZOrder(2)
                .setImage(Icon.createWithBitmap(bitmap))
                .setBounds(bounds)
                .build();
        components.add(bottomBlockingComponent);

        return components;

    }

    @Override
    protected WatchFaceDecomposition buildDecomposition() {
        List<WatchFaceDecomposition.Component> components = new ArrayList<>();
        WatchFaceDecomposition.Builder builder = new WatchFaceDecomposition.Builder();

        components.addAll(getDigitalComponents());
        components.addAll(getDateComponents());
        components.addAll(getComplicationComponents());
        components.addAll(getBackgroundComponents());
        components.add(getSecondHandComponent());

        for (WatchFaceDecomposition.Component c : components) {
            if (c instanceof ImageComponent)
                builder.addImageComponents((ImageComponent)c);
            else if (c instanceof FontComponent)
                builder.addFontComponents((FontComponent)c);
            else if (c instanceof NumberComponent)
                builder.addNumberComponents((NumberComponent)c);
            else if (c instanceof ComplicationComponent)
                builder.addComplicationComponents((ComplicationComponent)c);
        }

        return builder.build();
    }

    @Override
    public Engine onCreateEngine() {
        mCalendar = Calendar.getInstance();

        // Used throughout watch face to pull user's preferences.
//        Context context = getApplicationContext();
//        mSharedPref =
//                context.getSharedPreferences(
//                        getString(R.string.analog_complication_preference_file_key),
//                        Context.MODE_PRIVATE);

        mCalendar = Calendar.getInstance();

        initializeComplicationsAndBackground();
        initializeWatchFace();
//        initializeBackground();
//        updateWatchHandStyle();

        return new Engine();
    }

//    private static class EngineHandler extends Handler {
//        private final WeakReference<MyWatchFace.Engine> mWeakReference;
//
//        public EngineHandler(MyWatchFace.Engine reference) {
//            mWeakReference = new WeakReference<>(reference);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            MyWatchFace.Engine engine = mWeakReference.get();
//            if (engine != null) {
//                switch (msg.what) {
//                    case MSG_UPDATE_TIME:
//                        engine.handleUpdateTimeMessage();
//                        break;
//                }
//            }
//        }
//    }

    private void initializeComplicationsAndBackground() {
//        mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        // Creates a ComplicationDrawable for each location where the user can render a
        // complication on the watch face. In this watch face, we create one for left, right,
        // and background, but you could add many more.
        ComplicationDrawable topComplicationDrawable =
                new ComplicationDrawable(getApplicationContext());
        ComplicationDrawable bottomComplicationDrawable =
                new ComplicationDrawable(getApplicationContext());

        mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

        mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
        mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
        /*
        topComplicationDrawable.setBorderColorAmbient(Color.RED);
        topComplicationDrawable.setBorderColorActive(Color.RED);
        topComplicationDrawable.setBorderRadiusAmbient(20);
        topComplicationDrawable.setHighlightColorAmbient(Color.RED);
        topComplicationDrawable.setTextColorAmbient(Color.RED);
        topComplicationDrawable.setBackgroundColorAmbient(Color.RED);
        topComplicationDrawable.setTitleColorAmbient(Color.RED);
        topComplicationDrawable.setIconColorAmbient(Color.RED);
        topComplicationDrawable.setTextTypefaceAmbient(getResources().getFont(R.font.rounded_semibold));
        topComplicationDrawable.setTextTypefaceActive(getResources().getFont(R.font.rounded_semibold));
        topComplicationDrawable.setTitleSizeActive(20);
        topComplicationDrawable.setTitleSizeAmbient(20);
        topComplicationDrawable.setBorderStyleActive(ComplicationDrawable.BORDER_STYLE_NONE);
        topComplicationDrawable.setBorderStyleAmbient(ComplicationDrawable.BORDER_STYLE_NONE);*/

        // Adds new complications to a SparseArray to simplify setting styles and ambient
        // properties for all complications, i.e., iterate over them all.




        //setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);
    }


    /* Sets active/ambient mode colors for all complications.
     *
     * Note: With the rest of the watch face, we update the paint colors based on
     * ambient/active mode callbacks, but because the ComplicationDrawable handles
     * the active/ambient colors, we only set the colors twice. Once at initialization and
     * again if the user changes the highlight color via AnalogComplicationConfigActivity.
     */
//    private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
//        int complicationId;
//        ComplicationDrawable complicationDrawable;
//
//        for (int id : COMPLICATION_IDS) {
//            complicationDrawable = mComplicationDrawableSparseArray.get(id);
//
//            // Active mode colors.
//            complicationDrawable.setBorderColorActive(primaryComplicationColor);
//            complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);
//
//            // Ambient mode colors.
//            complicationDrawable.setBorderColorAmbient(Color.RED);
//            complicationDrawable.setRangedValuePrimaryColorAmbient(Color.RED);
//        }
//    }
//
//    private void initializeBackground() {
//        mBackgroundPaint = new Paint();
//        mBackgroundPaint.setColor(Color.BLACK);
//        mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
//
//        /* Extracts colors from background image to improve watchface style. */
//        Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
//            @Override
//            public void onGenerated(Palette palette) {
//                if (palette != null) {
//                    mWatchHandHighlightColor = palette.getVibrantColor(Color.RED);
//                    mWatchHandColor = palette.getLightVibrantColor(Color.WHITE);
//                    mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
//                    updateWatchHandStyle();
//                }
//            }
//        });
//    }

    private void initializeWatchFace() {
        /* Set defaults for colors */
//        mWatchHandColor = Color.WHITE;
//        mWatchHandHighlightColor = Color.RED;
//        mWatchHandShadowColor = Color.BLACK;
//
//        mHourPaint = new Paint();
//        mHourPaint.setColor(mWatchHandColor);
//        mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
//        mHourPaint.setAntiAlias(true);
//        mHourPaint.setStrokeCap(Paint.Cap.ROUND);
//        mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//
//        mMinutePaint = new Paint();
//        mMinutePaint.setColor(mWatchHandColor);
//        mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
//        mMinutePaint.setAntiAlias(true);
//        mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
//        mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//
//        mSecondPaint = new Paint();
//        mSecondPaint.setColor(mWatchHandHighlightColor);
//        mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
//        mSecondPaint.setAntiAlias(true);
//        mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
//        mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//
//        mTickAndCirclePaint = new Paint();
//        mTickAndCirclePaint.setColor(mWatchHandColor);
//        mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
//        mTickAndCirclePaint.setAntiAlias(true);
//        mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
//        mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

        DisplayMetrics dm = getApplication().getResources().getDisplayMetrics();
        int AMBIENT_OFFSET = 12;
        AMBIENT_DISPLAY_WIDTH = dm.widthPixels - AMBIENT_OFFSET;

//        /*
//         * Find the coordinates of the center point on the screen, and ignore the window
//         * insets, so that, on round watches with a "chin", the watch face is centered on the
//         * entire screen, not just the usable portion.
//         */
//        mCenterX = dm.widthPixels / 2f;
//        mCenterY = dm.heightPixels / 2f;
//
//        /*
//         * Calculate lengths of different hands based on watch screen size.
//         */
//        mSecondHandLength = (float) (mCenterX * 0.875);
//        sMinuteHandLength = (float) (mCenterX * 0.75);
//        sHourHandLength = (float) (mCenterX * 0.5);

        /*
         * Calculates location bounds for right and left circular complications. Please note,
         * we are not demonstrating a long text complication in this watch face.
         *
         * We suggest using at least 1/4 of the screen width for circular (or squared)
         * complications and 2/3 of the screen width for wide rectangular complications for
         * better readability.
         */

        // For most Wear devices, width and height are the same, so we just chose one (width).
//        int sizeOfComplication = dm.widthPixels / 4;
//        int midpointOfScreen = dm.widthPixels / 2;
//        int complicationHeight = dm.widthPixels / 4;
//        int complicationWidth = dm.widthPixels * 2/3;
//
//        int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
//        int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);
//        horizontalOffset = (midpointOfScreen - complicationHeight) / 2;
//        verticalOffset = midpointOfScreen - (complicationWidth / 2);


//        mTopComplicationBounds =
//                // Left, Top, Right, Bottom
//                new Rect(
//                        horizontalOffset,
//                        verticalOffset,
//                        (horizontalOffset + complicationWidth),
//                        (verticalOffset + complicationHeight));
//
//        ComplicationDrawable leftComplicationDrawable =
//                mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
//        leftComplicationDrawable.setBounds(mTopComplicationBounds);
//
//        mRightComplicationBounds =
//                // Left, Top, Right, Bottom
//                new Rect(
//                        (midpointOfScreen + horizontalOffset),
//                        verticalOffset,
//                        (midpointOfScreen + horizontalOffset + sizeOfComplication),
//                        (verticalOffset + sizeOfComplication));
//
//        ComplicationDrawable rightComplicationDrawable =
//                mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
//        rightComplicationDrawable.setBounds(mRightComplicationBounds);
    }

//    private void updateWatchHandStyle() {
//        if (mAmbient) {
//            mHourPaint.setColor(Color.WHITE);
//            mMinutePaint.setColor(Color.WHITE);
//            mSecondPaint.setColor(Color.WHITE);
//            mTickAndCirclePaint.setColor(Color.WHITE);
//
//            mHourPaint.setAntiAlias(false);
//            mMinutePaint.setAntiAlias(false);
//            mSecondPaint.setAntiAlias(false);
//            mTickAndCirclePaint.setAntiAlias(false);
//
//            mHourPaint.clearShadowLayer();
//            mMinutePaint.clearShadowLayer();
//            mSecondPaint.clearShadowLayer();
//            mTickAndCirclePaint.clearShadowLayer();
//
//        } else {
//            mHourPaint.setColor(mWatchHandColor);
//            mMinutePaint.setColor(mWatchHandColor);
//            mSecondPaint.setColor(mWatchHandHighlightColor);
//            mTickAndCirclePaint.setColor(mWatchHandColor);
//
//            mHourPaint.setAntiAlias(true);
//            mMinutePaint.setAntiAlias(true);
//            mSecondPaint.setAntiAlias(true);
//            mTickAndCirclePaint.setAntiAlias(true);
//
//            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
//        }
//    }


//    private static final float HOUR_STROKE_WIDTH = 5f;
//    private static final float MINUTE_STROKE_WIDTH = 3f;
//    private static final float SECOND_TICK_STROKE_WIDTH = 2f;
//
//    private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
//
//    private static final int SHADOW_RADIUS = 6;

    private Calendar mCalendar;
    private boolean mRegisteredTimeZoneReceiver = false;
//    private boolean mMuteMode;
//    private float mCenterX;
//    private float mCenterY;
//    private float mSecondHandLength;
//    private float sMinuteHandLength;
//    private float sHourHandLength;
    /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
//    private int mWatchHandColor;
//    private int mWatchHandHighlightColor;
//    private int mWatchHandShadowColor;
//    private Paint mHourPaint;
//    private Paint mMinutePaint;
//    private Paint mSecondPaint;
//    private Paint mTickAndCirclePaint;
//    private Paint mBackgroundPaint;
//    private Bitmap mBackgroundBitmap;
//    private Bitmap mGrayBackgroundBitmap;
//    private boolean mAmbient;
//    private boolean mLowBitAmbient;
//    private boolean mBurnInProtection;


    /* Maps complication ids to corresponding ComplicationDrawable that renders the
     * the complication data on the watch face.
     */
    private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

    // Used to pull user's preferences for background color, highlight color, and visual
    // indicating there are unread notifications.
//    SharedPreferences mSharedPref;


    private class Engine extends DecompositionWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
//        private final Handler mUpdateTimeHandler = new EngineHandler(this);

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

//        @Override
//        public void onCreate(SurfaceHolder holder) {
//            super.onCreate(holder);
//
//            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
//                    .setAcceptsTapEvents(true)
//                    .build());/*
//            @SuppressLint({"InvalidWakeLockTag", "WrongConstant"}) PowerManager.WakeLock ambientUpdateWakelock = ((PowerManager)MyWatchFace.this.getSystemService("power")).newWakeLock(1, "WatchFaceService[AmbientUpdate]");
//            try {
//                Field f = WatchFaceService.Engine.class.getDeclaredField("mAmbientUpdateWakelock");
//                f.setAccessible(true);
//                f.set(this, ambientUpdateWakelock);
//                Field f1 = DecompositionWatchFaceService.Engine.class.getDeclaredField("decompositionDrawable");
//                Field f2 = DecompositionWatchFaceService.Engine.class.getDeclaredField("decomposition");
//                Field f3 = DecompositionWatchFaceService.Engine.class.getDeclaredField("drawableCallback");
//                Field f4 = DecompositionWatchFaceService.Engine.class.getDeclaredField("stepIntervalMs");
//                Method m1 = DecompositionWatchFaceService.Engine.class.getDeclaredMethod("setDefaultsAndActivateComplications");
//                f1.setAccessible(true);
//                f2.setAccessible(true);
//                f3.setAccessible(true);
//                f4.setAccessible(true);
//                m1.setAccessible(true);
//                WatchFaceDecomposition decomposition = MyWatchFace.this.buildDecomposition();
//                Drawable.Callback drawableCallback = (Drawable.Callback) f3.get(this);
//                DecompositionDrawable dd = new CustomDecompositionDrawable(getApplicationContext(),  mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID));
//                dd.setDecomposition(decomposition, false);
//                dd.setCallback(drawableCallback);
//                f1.set(this, dd);
//                f2.set(this, decomposition);
//                f4.set(this, DecompositionDrawable.calculateStepIntervalMs(decomposition, 0.3F));
//                m1.invoke(this);
//                updateDecomposition(decomposition);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//            updateDecompositionDrawable();*/
//        }
//
//        @Override
//        public void onDestroy() {
//            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
//            super.onDestroy();
//        }

//        @Override
//        public void onPropertiesChanged(Bundle properties) {
//            super.onPropertiesChanged(properties);
//            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
//            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
//        }
//

//        /*
//         * Called when there is updated data for a complication id.
//         */
//        @Override
//        public void onComplicationDataUpdate(
//                int complicationId, ComplicationData complicationData) {
//            super.onComplicationDataUpdate(complicationId, complicationData);/*
//
//            // Adds/updates active complication data in the array.
//            mActiveComplicationDataSparseArray.put(complicationId, complicationData);
//
//            // Updates correct ComplicationDrawable with updated data.
//            ComplicationDrawable complicationDrawable =
//                    mComplicationDrawableSparseArray.get(complicationId);
//            System.out.println("complicationData.getType(): " + complicationData.getType());
//            if (complicationData.getLongTitle() != null) {
//                System.out.println("complicationData.getLongTitle(): " + complicationData.getLongTitle().getText(getApplicationContext(), System.currentTimeMillis()));
//                try {
//                    Field f = ComplicationData.class.getDeclaredField("mFields");
//                    f.setAccessible(true);
//                    Bundle b = (Bundle)f.get(complicationData);
//                    ComplicationText ct = new ComplicationText.TimeDifferenceBuilder().setSurroundingText("Lol").build();
//                    b.putParcelable("LONG_TITLE", ct);
//                } catch (Exception ignored) {
//
//                }
//            }
//            if (complicationData.getShortText() != null)
//                System.out.println("complicationData.getLongText(): " + complicationData.getLongText().getText(getApplicationContext(), System.currentTimeMillis()));
//            complicationDrawable.setComplicationData(complicationData);
//
//            invalidate();*/
//        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }
//
//        @Override
//        public void onAmbientModeChanged(boolean inAmbientMode) {
//            super.onAmbientModeChanged(inAmbientMode);/*
//            mAmbient = inAmbientMode;
//
//            updateWatchHandStyle();
//
//            // Update drawable complications' ambient state.
//            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
//            // have to inform it to enter ambient mode.
//            ComplicationDrawable complicationDrawable;
//
//            for (int complicationId : COMPLICATION_IDS) {
//                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
//                complicationDrawable.setInAmbientMode(mAmbient);
//            }*/
//
//            /* Check and trigger whether or not timer should be running (only in active mode). */
//            updateTimer();
//        }
//
//        @Override
//        public void onInterruptionFilterChanged(int interruptionFilter) {
//            super.onInterruptionFilterChanged(interruptionFilter); /*
//            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
//
//            /* Dim display in mute mode.
//            if (mMuteMode != inMuteMode) {
//                mMuteMode = inMuteMode;
//                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
//                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
//                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
//                invalidate();
//            }*/
//        }
//
//        @Override
//        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            super.onSurfaceChanged(holder, format, width, height);
//
//            /* Scale loaded background image (more efficient) if surface dimensions change.
//            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();
//
//            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                    (int) (mBackgroundBitmap.getWidth() * scale),
//                    (int) (mBackgroundBitmap.getHeight() * scale), true);
//
//            /*
//             * Create a gray version of the image only if it will look nice on the device in
//             * ambient mode. That means we don't want devices that support burn-in
//             * protection (slight movements in pixels, not great for images going all the way to
//             * edges) and low ambient mode (degrades image quality).
//             *
//             * Also, if your watch face will know about all images ahead of time (users aren't
//             * selecting their own photos for the watch face), it will be more
//             * efficient to create a black/white version (png, etc.) and load that when you need it.
//             */
//            if (!mBurnInProtection && !mLowBitAmbient) {
//                initGrayBackgroundBitmap();
//            }
//        }
//
//        private void initGrayBackgroundBitmap() {
//            mGrayBackgroundBitmap = Bitmap.createBitmap(
//                    mBackgroundBitmap.getWidth(),
//                    mBackgroundBitmap.getHeight(),
//                    Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
//            Paint grayPaint = new Paint();
//            ColorMatrix colorMatrix = new ColorMatrix();
//            colorMatrix.setSaturation(0);
//            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
//            grayPaint.setColorFilter(filter);
//            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
//        }
//
//        /**
//         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
//         * used for implementing specific logic to handle the gesture.
//         */
//        @Override
//        public void onTapCommand(int tapType, int x, int y, long eventTime) {
//            super.onTapCommand(tapType, x, y, eventTime);/*
//            switch (tapType) {
//                case TAP_TYPE_TAP:
//
//                    // If your background complication is the first item in your array, you need
//                    // to walk backward through the array to make sure the tap isn't for a
//                    // complication above the background complication.
//                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
//                        int complicationId = COMPLICATION_IDS[i];
//                        ComplicationDrawable complicationDrawable =
//                                mComplicationDrawableSparseArray.get(complicationId);
//
//                        boolean successfulTap = complicationDrawable.onTap(x, y);
//
//                        if (successfulTap) {
//                            return;
//                        }
//                    }
//                    break;
//            }*/
//        }
//
//        @Override
//        public void onDraw(Canvas canvas, Rect bounds) {
////            long now = System.currentTimeMillis();
//            super.onDraw(canvas, bounds);
///*            drawComplications(canvas, now);
//            if (isVisible() && false) {
//                mCalendar.setTimeInMillis(now);
//
//                drawBackground(canvas);
//                drawComplications(canvas, now);
//                drawWatchFace(canvas);
//            }*/
//        }
//
//        private void drawSecondHand(Canvas canvas) {
//
//            /*
//             * These calculations reflect the rotation in degrees per unit of time, e.g.,
//             * 360 / 60 = 6 and 360 / 12 = 30.
//             */
//            final float seconds =
//                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
//            final float secondsRotation = seconds * 6f;
//
//            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;
//
//            /*
//             * Save the canvas state before we can begin to rotate it.
//             */
//            canvas.save();
//
//
//            /*
//             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
//             * Otherwise, we only update the watch face once a minute.
//             */
//            if (!mAmbient) {
//                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
//                int centerDia = 10;
//                int strokeWidth = 3;
//                int tailLength = 20;
//
//
//                int w = AMBIENT_DISPLAY_WIDTH - 2;
//
//                // Second hand
//                int secondHeight = (w - centerDia) / 2;
//
//                Paint secondPaint = mSecondPaint;
//
//                canvas.drawLine(
//                        w / 2f,
//                        w - tailLength,
//                        w / 2f,
//                        w,
//                        secondPaint);
//                canvas.drawCircle(
//                        w / 2f,
//                        w / 2f,
//                        centerDia / 2f,
//                        secondPaint);
//                canvas.drawLine(
//                        w / 2f,
//                        w - tailLength - centerDia,
//                        w / 2f,
//                        0,
//                        secondPaint);
//
//            }
//
//            /* Restore the canvas' original orientation. */
//            canvas.restore();
//        }
//
//        private void drawBackground(Canvas canvas) {
//
//            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
//                canvas.drawColor(Color.BLACK);
//            } else if (mAmbient) {
//                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
//            } else {
//                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
//            }
//        }
//
//        private void drawComplications(Canvas canvas, long currentTimeMillis) {
//            int complicationId;
//            ComplicationDrawable complicationDrawable;
//
//            for (int id : COMPLICATION_IDS) {
//                complicationId = id;
//                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
//
//                complicationDrawable.draw(canvas, currentTimeMillis);
//            }
//        }
//
//        private void drawWatchFace(Canvas canvas) {
//
//            /*
//             * Draw ticks. Usually you will want to bake this directly into the photo, but in
//             * cases where you want to allow users to select their own photos, this dynamically
//             * creates them on top of the photo.
//             */
//            float innerTickRadius = mCenterX - 10;
//            float outerTickRadius = mCenterX;
//            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
//                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
//                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
//                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
//                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
//                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
//                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
//                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
//            }
//
//            /*
//             * These calculations reflect the rotation in degrees per unit of time, e.g.,
//             * 360 / 60 = 6 and 360 / 12 = 30.
//             */
//            final float seconds =
//                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
//            final float secondsRotation = seconds * 6f;
//
//            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;
//
//            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
//            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;
//
//            /*
//             * Save the canvas state before we can begin to rotate it.
//             */
//            canvas.save();
//
//            canvas.rotate(hoursRotation, mCenterX, mCenterY);
//            canvas.drawLine(
//                    mCenterX,
//                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
//                    mCenterX,
//                    mCenterY - sHourHandLength,
//                    mHourPaint);
//
//            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
//            canvas.drawLine(
//                    mCenterX,
//                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
//                    mCenterX,
//                    mCenterY - sMinuteHandLength,
//                    mMinutePaint);
//
//            /*
//             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
//             * Otherwise, we only update the watch face once a minute.
//             */
//            if (!mAmbient) {
//                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
//                canvas.drawLine(
//                        mCenterX,
//                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
//                        mCenterX,
//                        mCenterY - mSecondHandLength,
//                        mSecondPaint);
//
//            }
//            canvas.drawCircle(
//                    mCenterX,
//                    mCenterY,
//                    CENTER_GAP_AND_CIRCLE_RADIUS,
//                    mTickAndCirclePaint);
//
//            /* Restore the canvas' original orientation. */
//            canvas.restore();
//        }
//
//        private void updateDecompositionDrawable() {
//
//        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                //setComplicationsActiveAndAmbientColors(mWatchHandHighlightColor);

                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
//            updateTimer();
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

//        /**
//         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
//         */
//        private void updateTimer() {
//            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
//            if (shouldTimerBeRunning()) {
//                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
//            }
//        }
//
//        /**
//         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
//         * should only run in active mode.
//         */
//        private boolean shouldTimerBeRunning() {
//            return isVisible() && !mAmbient;
//        }
//
//        /**
//         * Handle updating the time periodically in interactive mode.
//         */
//        private void handleUpdateTimeMessage() {
//            invalidate();
//            if (shouldTimerBeRunning()) {
//                long timeMs = System.currentTimeMillis();
//                long delayMs = INTERACTIVE_UPDATE_RATE_MS
//                long delayMs = INTERACTIVE_UPDATE_RATE_MS
//                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
//                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
//            }
//        }
    }
}
