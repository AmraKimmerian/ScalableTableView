package com.example.amra.scalabletableview;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public abstract class GesturesView extends View {

    private final String LOGTAG = "GESTURESVIEW";

    private float pointer1DownX;
    private float pointer1DownY;

    private float lastPointer0MoveX;
    private float lastPointer0MoveY;
    private float lastPointer1MoveX;
    private float lastPointer1MoveY;

    private int longPressTimeOut;

    // Threshold of move.
    // If moving > touchSlop - start move.
    // If moving < touchSlop after finger up - this is a tap.
    private int touchSlop;

    // User exceed touchSlop and child now dragging
    private boolean dragging;

    // For onFling event
    // Time to perform 1 step of fling moving
    private final int frameTime = 10;
    private final int frameRate = 1000/frameTime;
    // Velocities to perform a fling (in pixel per 1 frame)
    private int currentVelocityX, currentVelocityY;
    private VelocityTracker velocityTracker;
    private int minimumVelocity;
    private int maximumVelocity;

    // Handler and Runnable to perform a fling step
    private Handler flingHandler;
    private final Runnable flingStepRunnable = new Runnable() {
        @Override
        public void run() {
            //Log.i(logTagG, "currentVelocityX="+currentVelocityX+", currentVelocityY"+currentVelocityY);
            if (Math.abs(currentVelocityX)>1 || Math.abs(currentVelocityY)>1) {
                onDrag(-currentVelocityX, -currentVelocityY);
                //savePreferences();
                currentVelocityX *=0.95f;
                currentVelocityY *=0.95f;
                flingHandler.postDelayed(flingStepRunnable, frameTime);
            } else {
                onFlingStop();
            }

        }
    };

    private Handler longPressHandler;
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            longPressPerformed = true;
            onLongPress(pointer1DownX, pointer1DownY);
        }
    };
    private boolean longPressPerformed;

    // Distance between fingers when second finger touch the layout
    private float lastDistanceBetween2Pointers;

    public GesturesView(Context context) {
        super(context);
        init();
    }

    public GesturesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GesturesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Get default device parameters for touch events
        longPressTimeOut = ViewConfiguration.getLongPressTimeout();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // One finger
        if (event.getPointerCount() == 1) {

            notifyVelocityTracker(event);

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    // Save touch coords
                    pointer1DownX = event.getX();
                    pointer1DownY = event.getY();
                    //Log.i(LOGTAG, "ACTION_DOWN: pointer1DownX=" + pointer1DownX + ", pointer1DownY=" + pointer1DownY);

                    // * Stop current flinging anyway, if view is flinging.
                    // При этом возобновляем режим перетаскивания
                    if (isBeingFlinged()) {
                        onFlingStop();
                        dragging = true;
                        // Т. к. режим перетаскивания восстановлен, то ему нужны значение предыдущего события для вычисления текущего сдвига.
                        lastPointer0MoveX = event.getX();
                        lastPointer0MoveY = event.getY();
                    } else {
                        // Если прикосновение было обычным, то фиксируем событие onPress
                        onPress(pointer1DownX, pointer1DownY);
                        // А также запускаем отсчет времени для события долгого нажатия
                        startLongPress();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    //Log.i(LOGTAG, "ACTION_MOVE: dragging=" + dragging);

                    // Если ранее был установлен режим перетаскивания - продолжаем перетаскивать
                    if (dragging) {
                        onDrag(event);
                    } else {
                        // Если ранее не был установлен режим перетаскивания, вычисляем, не превышен ли порог движения,
                        // после которого можно считать, что перетаскивание начато.
                        if (isTouchSlopExceeded(event.getX(), event.getY(), pointer1DownX, pointer1DownY)) {
                            //Log.i(LOGTAG, "ACTION_MOVE: touchSlopExceeded");
                            dragging = true;
                            // Если порог превышен, то останавливаем процесс ожидания срабатывания долгого нажатия
                            stopLongPress();
                            // и начинаем перетаскивать
                            onDrag(event);
                        }
                    }

                    // В любом случае запоминаем последние координаты.
                    // Это приведет к тому что перетаскивание будеть чуть запаздывать от пальца,
                    // но будет происходить без рывка во время срабатывания порога
                    lastPointer0MoveX = event.getX();
                    lastPointer0MoveY = event.getY();
                    break;

                case MotionEvent.ACTION_UP:
                    Log.i(LOGTAG, "ACTION_UP");
                    // 3 cases:
                    // * dragged and stop dragged (onDrop)
                    // * dragged and flinged (onFling)
                    // * not dragged and release screen (onClick)
                    if (dragging) {
                        dragging = false;
                        // drop the layout
                        onDrop(event);
                        // and define - is it need to onFling
                        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                        int initialXVelocity = (int) velocityTracker.getXVelocity();
                        int initialYVelocity = (int) velocityTracker.getYVelocity();
                        if (Math.abs(initialXVelocity) + Math.abs(initialYVelocity) > minimumVelocity) {
                            onFling(-initialXVelocity, -initialYVelocity);
                        }
                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                    } else {
                        if (!longPressPerformed) {
                            stopLongPress();
                            onClick(pointer1DownX, pointer1DownY);
                        }
                    }
                    longPressPerformed = false;
                    break;
            }
        } else if (event.getPointerCount() == 2) {// Two fingers

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_POINTER_DOWN:
                    //Log.i(LOGTAG, "2 POINTERS ACTION_POINTER_DOWN");
                    dragging = true;

                    // Get coordinates of fingers
                    float x0 = event.getX(0);
                    float y0 = event.getY(0);
                    float x1 = event.getX(1);
                    float y1 = event.getY(1);

                    // calculate distance between 1st and 2nd fingers, when 2nd fingers touch the screen
                    lastDistanceBetween2Pointers = (float) Math.hypot(x1 - x0, y1 - y0);

                    lastPointer0MoveX = event.getX();
                    lastPointer0MoveY = event.getY();
                    lastPointer1MoveX = event.getX(1);
                    lastPointer1MoveY = event.getY(1);
                    break;

                case MotionEvent.ACTION_MOVE:
                    //Log.i(LOGTAG, "2 POINTERS ACTION_MOVE");
                    // Если опущено два пальца, то при любом движении фиксируем событий щипка,
                    onPinch(event);
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    //Log.i(LOGTAG, "2 POINTERS ACTION_POINTER_UP. lastPointer before:" + lastPointer0MoveX + ", " + lastPointer0MoveY);
                    // Set last action coordinates for leftover finger
                    if (event.getActionIndex()==1) {
                        lastPointer0MoveX = event.getX(0);
                        lastPointer0MoveY = event.getY(0);
                    }
                    if (event.getActionIndex()==0) {
                        lastPointer0MoveX = event.getX(1);
                        lastPointer0MoveY = event.getY(1);
                    }

                    //Log.i(LOGTAG, "2 POINTERS ACTION_POINTER_UP. lastPointer after:" + lastPointer0MoveX + ", " + lastPointer0MoveY);

                    break;
            }
        }
        return true;
    }

    private void startLongPress() {
        longPressHandler = new Handler();
        longPressHandler.postDelayed(longPressRunnable, longPressTimeOut);
    }

    private void stopLongPress() {
        if (longPressRunnable!=null && longPressHandler!=null) {
            longPressHandler.removeCallbacks(longPressRunnable);
            longPressHandler = null;
        }
    }

    //Adding event to velocity tracker
    private void notifyVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    /*
     * Is now view is flinged?
     * @return true if fling Handler exist have callbacks
     */
    private boolean isBeingFlinged() {
        return flingHandler != null;
    }

    /*
     * Define is event exceed touchSlop (is second tap or accidental move more than threshold)
     */
    private boolean isTouchSlopExceeded(float currEventX, float currEventY, float prevEventX, float prevEventY) {
        float xDiff = Math.abs(currEventX - prevEventX);
        float yDiff = Math.abs(currEventY - prevEventY);
        return (xDiff > touchSlop || yDiff > touchSlop);
    }


    /*
     * User drag across view with 1 finger
     * (getHistoricalAxisValue не используется, т. к. неясным способом уменьшает масштаб перемещения -
     * палец двигается дальше чем контент)
     */
    private void onDrag(MotionEvent event) {
        float dx = event.getX() - lastPointer0MoveX;
        float dy = event.getY() - lastPointer0MoveY;
        Log.i(LOGTAG, "onDrag: pointer "+event.getX()+","+event.getY()+" lastPointer "+ lastPointer0MoveX+","+lastPointer0MoveY+" delta " + dx + ", " + dy);
        onDrag(dx, dy);
    }

    /*
     * User pinch across view with 2 fingers
     * включает в себя и перемещение и масштабирование относительно точки
     */
    private void onPinch(MotionEvent event) {
        float x0 = event.getX();
        float y0 = event.getY();

        float x1 = event.getX(1);
        float y1 = event.getY(1);

        // Drag params
        float dx0 = x0 - lastPointer0MoveX;
        float dy0 = y0 - lastPointer0MoveY;

        float dx1 = x1 - lastPointer1MoveX;
        float dy1 = y1 - lastPointer1MoveY;

        float dx = (dx0 + dx1) / 2;
        float dy = (dy0 + dy1) / 2;


        // Scale params
        // Center point between fingers
        float centerX = (x0 + x1) / 2;
        float centerY = (y0 + y1) / 2;
        // Distance between 1st and 2nd fingers
        float distanceBetween2Pointers = (float) Math.hypot(x1 - x0, y1 - y0);
        // Scale factor
        float scaleStep = distanceBetween2Pointers / lastDistanceBetween2Pointers;


        onPinch(dx, dy, centerX, centerY, scaleStep);

        lastPointer0MoveX = x0;
        lastPointer0MoveY = y0;
        lastPointer1MoveX = x1;
        lastPointer1MoveY = y1;
        lastDistanceBetween2Pointers = distanceBetween2Pointers;

    }

    /*
     * User fling view: user release finger from layout while dragging with some minimum velocity
     */
    private void onFling(int velocityX, int velocityY) {
        //Log.i(logTagG, "onFling");
        // init velocity in pixel per 1 frame
        currentVelocityX = velocityX/frameRate;
        currentVelocityY = velocityY/frameRate;
        flingHandler = new Handler();
        flingHandler.post(flingStepRunnable);
    }

    /*
     * User touch the layout while it flinging
     */
    private void onFlingStop() {
        //Log.i(logTagG, "onFlingStop");
        if (flingStepRunnable!=null && flingHandler!=null) {
            flingHandler.removeCallbacks(flingStepRunnable);
            flingHandler = null;
        }
    }

    /*
     * User release finger from layout while dragging
     */
    private void onDrop(MotionEvent event) {
        //Log.i(logTagG, "onDrop");
        /*lastActionX = event.getX();
        lastActionY = event.getY();*/
    }

    abstract void onPress(float x, float y);

    abstract void onLongPress(float x, float y);

    abstract void onClick(float x, float y);

    abstract void onDrag(float dx, float dy);

    abstract void onPinch(float dx, float dy, float anchorX, float anchorY, float scaleStep);


}
