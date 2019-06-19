package pl.bclogic.pulsator4droid.library;

import android.graphics.Canvas;

public interface PulseShape {

    void setSize(float width, float height);

    void draw(Canvas canvas);
}
