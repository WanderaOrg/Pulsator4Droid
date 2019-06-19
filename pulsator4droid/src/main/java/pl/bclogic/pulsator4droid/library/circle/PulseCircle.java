package pl.bclogic.pulsator4droid.library.circle;

import android.graphics.Canvas;
import android.graphics.Paint;

import pl.bclogic.pulsator4droid.library.PulseShape;

public class PulseCircle implements PulseShape {

    private float mCenterX;
    private float mCenterY;
    private float mRadius;
    private Paint mPaint;

    PulseCircle(Paint paint) {
        mPaint = paint;
    }

    @Override
    public void setSize(float width, float height) {
        mCenterX = width * 0.5f;
        mCenterY = height * 0.5f;
        mRadius = Math.min(mCenterX, mCenterY);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint);
    }

    public float getCenterX(){
        return mCenterX;
    }

    public float getCenterY(){
        return mCenterY;
    }
}
