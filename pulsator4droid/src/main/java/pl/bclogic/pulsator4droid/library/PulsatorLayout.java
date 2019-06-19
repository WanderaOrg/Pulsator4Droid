package pl.bclogic.pulsator4droid.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;

/**
 * Created by booncol on 04.07.2016.
 */
public abstract class PulsatorLayout extends RelativeLayout {

    public static final int INFINITE = 0;
    public static final float RADIUS_NONE = 0;

    private static final int DEFAULT_COUNT = 4;
    private static final int DEFAULT_COLOR = Color.rgb(0, 116, 193);
    private static final int DEFAULT_DURATION = 7000;
  
    protected int mCount;
    protected int mDuration;
    protected int mRepeat;
    protected boolean mStartFromScratch;
    protected int mColor;
    protected int mInterpolator;

    protected PulseShape mPulseShape;

    protected Paint mPaint;
    protected Path mMask;

    protected boolean mIsStarted;

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context   The Context the view is running in, through which it can access the current
     *     theme, resources, etc.
     */
    public PulsatorLayout(Context context) {
        this(context, null, 0);
    }

    /**
     * Constructor that is called when inflating a view from XML.
     *
     * @param context   The Context the view is running in, through which it can access the current
     *     theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PulsatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     *
     * @param context   The Context the view is running in, through which it can access the current
     *     theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr  An attribute in the current theme that contains a reference to a style
     *     resource that supplies default values for the view. Can be 0 to not look for defaults.
     */
    public PulsatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // get attributes
        TypedArray attr = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.Pulsator4Droid, 0, 0);

        mCount = DEFAULT_COUNT;
        mDuration = DEFAULT_DURATION;
        mColor = DEFAULT_COLOR;

        try {
            mCount = attr.getInteger(R.styleable.Pulsator4Droid_pulse_count, DEFAULT_COUNT);
            mDuration = attr.getInteger(R.styleable.Pulsator4Droid_pulse_duration,
                    DEFAULT_DURATION);
            mColor = attr.getColor(R.styleable.Pulsator4Droid_pulse_color, DEFAULT_COLOR);

        } finally {
            attr.recycle();
        }

        // create paint
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColor);

        mPulseShape = getPulseShape(mPaint);
    }

    protected abstract PulseShape getPulseShape(Paint paint);

    /**
     * Start pulse animation. If the start request comes and there are no animations to run
     * (e.g. the view has been re-attached to a view before (and animations cleared), it is
     * reinitialized.
     */
    public abstract void start();

    /**
     * Stop pulse animation.
     */
    public abstract void stop();

    /**
     * Reset views and animations.
     */
    protected abstract void reset();

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    /**
     * Get number of pulses.
     *
     * @return Number of pulses
     */
    public int getCount() {
        return mCount;
    }

    /**
     * Set number of pulses.
     *
     * @param count Number of pulses
     */
    public void setCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }

        if (count != mCount) {
            mCount = count;
            reset();
            invalidate();
        }
    }

    /**
     * Get pulse duration.
     *
     * @return Duration of single pulse in milliseconds
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Set single pulse duration.
     *
     * @param millis Pulse duration in milliseconds
     */
    public void setDuration(int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        if (millis != mDuration) {
            mDuration = millis;
            reset();
            invalidate();
        }
    }

    /**
     * Gets the current color of the pulse effect in integer
     * Defaults to Color.rgb(0, 116, 193);
     *
     * @return an integer representation of color
     */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /**
     * Sets the current color of the pulse effect in integer
     * Takes effect immediately
     * Usage: Color.parseColor("<hex-value>") or getResources().getColor(R.color.colorAccent)
     *
     * @param color : an integer representation of color
     */
    public void setColor(@ColorInt int color) {
        if (color != mColor) {
            this.mColor = color;

            if (mPaint != null) {
                mPaint.setColor(color);
            }
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

        mPulseShape.setSize(width, height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
