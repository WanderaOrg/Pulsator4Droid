package pl.bclogic.pulsator4droid.library.circle;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Region.Op;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import pl.bclogic.pulsator4droid.library.PulseShape;
import pl.bclogic.pulsator4droid.library.R;

public class CirclePulsatorLayout extends PulsatorLayout {

    public static final int INTERP_LINEAR = 0;
    public static final int INTERP_ACCELERATE = 1;
    public static final int INTERP_DECELERATE = 2;
    public static final int INTERP_ACCELERATE_DECELERATE = 3;

    private static final int DEFAULT_REPEAT = INFINITE;
    private static final boolean DEFAULT_START_FROM_SCRATCH = true;
    private static final int DEFAULT_INTERPOLATOR = INTERP_LINEAR;
    private float mCircularMaskRadius = RADIUS_NONE;

    private final List<View> mViews = new ArrayList<>();

    /**
     * {@link android.animation.AnimatorSet} seems to be having issues with
     * {@link android.animation.ValueAnimator#setCurrentPlayTime(long) being used for its
     * encapsulated animations. We have to handle them (start them) manually one by one to avoid
     * that. More precisely Android versions O and P do not take current play time setting into
     * consideration and play all the animations at the same timing when started using an
     * {@link android.animation.AnimatorSet}.
     *
     * <p>(The play time can be fast-forwarded for the whole set, but not before API 26.)
     */
    private List<Animator> mAnimators;

    public CirclePulsatorLayout(Context context) {
        this(context, null, 0);
    }

    public CirclePulsatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CirclePulsatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // get attributes
        TypedArray attr = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.Pulsator4Droid, 0, 0);

        mRepeat = DEFAULT_REPEAT;
        mStartFromScratch = DEFAULT_START_FROM_SCRATCH;
        mInterpolator = DEFAULT_INTERPOLATOR;

        try {
            mRepeat = attr.getInteger(R.styleable.Pulsator4Droid_pulse_repeat, DEFAULT_REPEAT);
            mStartFromScratch = attr.getBoolean(R.styleable.Pulsator4Droid_pulse_startFromScratch,
                    DEFAULT_START_FROM_SCRATCH);
            mInterpolator = attr.getInteger(R.styleable.Pulsator4Droid_pulse_interpolator,
                    DEFAULT_INTERPOLATOR);

        } finally {
            attr.recycle();
        }

        // create views
        build();
    }

    @Override
    protected PulseShape getPulseShape(Paint paint) {
        return new PulseCircle(paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMask == null && mCircularMaskRadius > 0) {
            mMask = new Path();
            PulseCircle circle = (PulseCircle) mPulseShape;
            mMask.addCircle(
                    circle.getCenterX(), circle.getCenterY(), mCircularMaskRadius, Direction.CW);
        }
        if (mMask != null) {
            canvas.clipPath(mMask, Op.DIFFERENCE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mAnimators != null) {
            for (Animator animator : mAnimators) {
                animator.cancel();
            }
            mAnimators = null;
        }
    }

    @Override
    public void start() {

        if (mAnimators == null) {
            reset();
            if (mAnimators == null) {
                return;
            }
        }

        if (mIsStarted) {
            return;
        }

        for (int x = 0; x < mAnimators.size(); x++) {
            ObjectAnimator objectAnimator = (ObjectAnimator) mAnimators.get(x);

            if (!mStartFromScratch) {
                // instead of delaying the animation, fast-forward it
                long delay = objectAnimator.getStartDelay();
                objectAnimator.setStartDelay(0);

                // This is where it starts to get tricky. The documentation of
                // ValueAnimator#setCurrentPlayTime(long) is a bit confusing about whether it
                // should be called before or after starting the animation itself. The truth is,
                // it seems the behavior differs between Android versions. If it gets called at
                // a wrong time, only some of the animations will start while others will not or
                // the animated object won't be visible at all.
                boolean shouldStartBeforeSettingCurrentTime =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1;

                if (shouldStartBeforeSettingCurrentTime) {
                    objectAnimator.start();
                }
                objectAnimator.setCurrentPlayTime(mDuration - delay);
                if (!shouldStartBeforeSettingCurrentTime) {
                    objectAnimator.start();
                }
            } else {
                objectAnimator.start();
            }
        }
    }

    @Override
    public void stop() {

        if (mAnimators == null || !mIsStarted) {
            return;
        }
        for (Animator animator : mAnimators) {
            animator.end();
        }
    }

    @Override
    protected void reset() {
        boolean isStarted = isStarted();

        stop();
        clear();
        build();

        if (isStarted) {
            start();
        }
    }

    /**
     * Get current interpolator type used for animating.
     *
     * @return Interpolator type as int
     */
    public int getInterpolator() {
        return mInterpolator;
    }

    /**
     * Set current interpolator used for animating.
     *
     * @param type Interpolator type as int
     */
    public void setInterpolator(int type) {
        if (type != mInterpolator) {
            mInterpolator = type;
            reset();
            invalidate();
        }
    }

    /**
     * Set how many times the pulse should repeat.
     *
     * @param repeat {@link #INFINITE} for infinite repeat. Also the default value.
     */
    public void setRepeat(int repeat) {
        mRepeat = repeat;
    }

    /**
     * Determines whether the animation starts empty and pulses are added gradually making
     * the animation come from the center or have all the rings present at time of starting.
     * @param startFromScratch <code>true</code> to add pulses one by one from the center.
     * <code>true</code> by default.
     */
    public void setStartFromScratch(boolean startFromScratch) {
        mStartFromScratch = startFromScratch;
    }

    private void clear() {

        // remove old views
        for (View view : mViews) {
            removeView(view);
        }
        mViews.clear();
        mCircularMaskRadius = RADIUS_NONE;
    }

    /**
     * The pulse animation will be clipped in a concentric circle with a given radius. Useful when
     * we need transparencyin the middle.
     *
     * @param radius Radius in pixels for the circle masking out the animation in the center.
     * Providing a value &lt;=0 disables the masking.
     */
    public void setCenterCircularMaskRadius(float radius) {
        if (mPulseShape instanceof PulseCircle) {
            if (radius <= 0) {
                // disable the onDraw method (masking)
                setWillNotDraw(true);
                mCircularMaskRadius = RADIUS_NONE;
                mMask = null;
            } else {
                // enable the onDraw method (masking)
                setWillNotDraw(false);
                mCircularMaskRadius = radius;
                // mask is being initialized on demand during #onDraw(Canvas) method (now enabled)
                // to make sure there has already been #onMeasure(int, int) pass
            }
        }
    }

    /**
     * Build pulse views and animators.
     */
    private void build() {
        // create views and animators
        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        int repeatCount = (mRepeat == INFINITE) ? ObjectAnimator.INFINITE : mRepeat;

        mAnimators = new ArrayList<>(3 * mCount);
        for (int index = 0; index < mCount; index++) {
            // setup view
            PulseView pulseView = new PulseView(getContext());
            pulseView.setScaleX(0);
            pulseView.setScaleY(0);
            pulseView.setAlpha(1);

            addView(pulseView, index, layoutParams);
            mViews.add(pulseView);

            long delay = index * mDuration / mCount;

            // setup animators
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(pulseView, "ScaleX", 0f, 1f);
            scaleXAnimator.setStartDelay(delay);
            mAnimators.add(scaleXAnimator);

            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(pulseView, "ScaleY", 0f, 1f);
            scaleYAnimator.setStartDelay(delay);
            mAnimators.add(scaleYAnimator);

            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(pulseView, "Alpha", 1f, 0f);
            alphaAnimator.setStartDelay(delay);
            mAnimators.add(alphaAnimator);
        }

        for (Animator animator : mAnimators) {
            ObjectAnimator objectAnimator = (ObjectAnimator) animator;
            objectAnimator.setRepeatCount(repeatCount);
            objectAnimator.setRepeatMode(ObjectAnimator.RESTART);
            objectAnimator.setInterpolator(createInterpolator(mInterpolator));
            objectAnimator.setDuration(mDuration);
        }

        if (mAnimators.isEmpty()) {
            mAnimators = null;
        } else {
            mAnimators.get(0).addListener(mAnimatorStartListener);
            mAnimators.get(mAnimators.size() - 1).addListener(mAnimatorEndListener);
        }
    }

    private class PulseView extends View {

        public PulseView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            mPulseShape.draw(canvas);
        }

    }

    private class AnimatorSimpleListener implements Animator.AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

    private final AnimatorSimpleListener mAnimatorStartListener = new AnimatorSimpleListener() {

        @Override
        public void onAnimationStart(Animator animator) {
            mIsStarted = true;
        }

    };

    private final AnimatorSimpleListener mAnimatorEndListener = new AnimatorSimpleListener() {

        @Override
        public void onAnimationEnd(Animator animator) {
            mIsStarted = false;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mIsStarted = false;
        }

    };

    /**
     * Create interpolator from type.
     *
     * @param type Interpolator type as int
     * @return Interpolator object of type
     */
    protected static Interpolator createInterpolator(int type) {
        switch (type) {
            case INTERP_ACCELERATE:
                return new AccelerateInterpolator();
            case INTERP_DECELERATE:
                return new DecelerateInterpolator();
            case INTERP_ACCELERATE_DECELERATE:
                return new AccelerateDecelerateInterpolator();
            default:
                return new LinearInterpolator();
        }
    }
}
