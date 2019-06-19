package pl.bclogic.pulsator4droid.library.roundedrectangle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import pl.bclogic.pulsator4droid.library.PulseShape;

public class RoundedRectanglePulsatorLayout extends PulsatorLayout {

    private static final int ALPHA_MAX = 0x88;

    private int mMaskWidth = 0;
    private int mMaskHeight = 0;

    private long mStartTime;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint;
    private Paint mMaskPaint;

    public RoundedRectanglePulsatorLayout(Context context) {
        this(context, null, 0);
    }

    public RoundedRectanglePulsatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedRectanglePulsatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBitmapPaint = new Paint();
        mMaskPaint = new Paint();
        mMaskPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        RectF pulseRectangle = ((PulseRoundedRectangle) mPulseShape).getRect();

        if (shouldMaskBeInitialized()) {

            mMask = new Path();
            float left = (pulseRectangle.right - pulseRectangle.left - mMaskWidth) * 0.5f;
            float right = left + mMaskWidth;
            float top = (pulseRectangle.bottom - pulseRectangle.top - mMaskHeight) * 0.5f;
            float bottom = top + mMaskHeight;
            mMask.addRoundRect(
                    left, top, right, bottom, Integer.MAX_VALUE, Integer.MAX_VALUE, Direction.CW);
        }

        if (mBitmap == null || mCanvas == null) {
            mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        float width = getWidth();
        float height = getHeight();

        mCanvas.drawColor(Color.BLACK, Mode.CLEAR);
        for (int i = 0; i < mCount; i++) {

            float maskLeft = (pulseRectangle.right - pulseRectangle.left - mMaskWidth) / 2;
            float maskTop = (pulseRectangle.bottom - pulseRectangle.top - mMaskHeight) / 2;

            float progress = ((System.currentTimeMillis() - mStartTime) % (float) mDuration) / mDuration;
            float offsetProgress = (1.0f / mCount * i + progress) % 1.0f;
            float radius = height / 2;

            float left = maskLeft - (maskLeft * offsetProgress);
            float top = maskTop - (maskTop * offsetProgress);
            float right = width - left;
            float bottom = height - top;

            mPaint.setAlpha((int) (ALPHA_MAX * (1 - offsetProgress)));
            mCanvas.drawRoundRect(left, top, right, bottom, radius, radius, mPaint);
        }

        if (mMask != null) {
            mCanvas.drawPath(mMask, mMaskPaint);
        }

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        if (isStarted()) {
            invalidate();
        }
    }

    @Override
    protected PulseShape getPulseShape(Paint paint) {
        return new PulseRoundedRectangle(paint);
    }

    @Override
    public void start() {
        if (mPulseShape instanceof PulseRoundedRectangle) {
            mIsStarted = true;
            mStartTime = System.currentTimeMillis();
            invalidate();
        }
    }

    @Override
    public void stop() {
        mIsStarted = false;
    }

    @Override
    protected void reset() {
        boolean isStarted = isStarted();

        stop();
        clear();

        if (isStarted) {
            start();
        }
    }

    private boolean shouldMaskBeInitialized(){
        return mMask == null && mMaskWidth > 0 && mMaskHeight > 0;
    }

    private void clear() {
        mBitmap = null;
        mCanvas = null;
    }

    /**
     * The pulse animation will be clipped in a rounded rectangle of the given size with a given
     * radius. Useful when we need transparency in the middle.
     *
     * @param width Width in pixels for the rounded rectangle masking out the animation in the
     * center. Providing a value &lt;=0 disables the masking.
     * @param height Height in pixels for the rounded rectangle masking out the animation in the
     * center. Providing a value &lt;=0 disables the masking.
     */
    public void setCenterRoundedRectangleMask(int width, int height) {
        if (mPulseShape instanceof PulseRoundedRectangle) {
            if (width < 1 || height < 1) {
                setMaskParams(0,0, false);
                mMask = null;
            } else {
                setMaskParams(width, height, true);
                // mask is being initialized on demand during #onDraw(Canvas) method (now enabled)
                // to make sure there has already been #onMeasure(int, int) pass
            }
        }
    }

    private void setMaskParams(int width, int height, boolean enableOnDraw){
        setWillNotDraw(!enableOnDraw);
        mMaskWidth = width;
        mMaskHeight = height;
    }
}
