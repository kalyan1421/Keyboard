package com.example.ai_keyboard;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class SwipeKeyboardView extends KeyboardView {
    
    private boolean swipeEnabled = true;
    private boolean isSwipeInProgress = false;
    private List<float[]> swipePoints = new ArrayList<>();
    private Paint swipePaint;
    private Path swipePath;
    private SwipeListener swipeListener;
    private long swipeStartTime = 0;
    private static final long MIN_SWIPE_TIME = 300; // Minimum time for swipe (ms)
    private static final float MIN_SWIPE_DISTANCE = 100; // Minimum distance for swipe (pixels)
    
    public interface SwipeListener {
        void onSwipeDetected(List<Integer> swipedKeys, String swipePattern);
        void onSwipeStarted();
        void onSwipeEnded();
    }
    
    public SwipeKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public SwipeKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        swipePaint = new Paint();
        swipePaint.setColor(Color.parseColor("#2196F3"));
        swipePaint.setStrokeWidth(8f);
        swipePaint.setStyle(Paint.Style.STROKE);
        swipePaint.setAntiAlias(true);
        swipePaint.setAlpha(180);
        
        swipePath = new Path();
    }
    
    public void setSwipeEnabled(boolean enabled) {
        this.swipeEnabled = enabled;
    }
    
    public void setSwipeListener(SwipeListener listener) {
        this.swipeListener = listener;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (!swipeEnabled) {
            return super.onTouchEvent(me);
        }
        
        boolean handled = handleSwipeTouch(me);
        if (handled) {
            return true;
        }
        
        return super.onTouchEvent(me);
    }
    
    private boolean handleSwipeTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startSwipe(x, y);
                return false; // Let normal key press handling occur
                
            case MotionEvent.ACTION_MOVE:
                if (isSwipeInProgress) {
                    continueSwipe(x, y);
                    return true; // Consume the event
                } else {
                    // Check if user has moved enough to start swipe
                    if (swipePoints.size() > 0) {
                        float[] startPoint = swipePoints.get(0);
                        float distance = (float) Math.sqrt(
                            Math.pow(x - startPoint[0], 2) + Math.pow(y - startPoint[1], 2)
                        );
                        
                        if (distance > 50) { // Start swipe if moved 50 pixels
                            isSwipeInProgress = true;
                            if (swipeListener != null) {
                                swipeListener.onSwipeStarted();
                            }
                            continueSwipe(x, y);
                            return true;
                        }
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
                if (isSwipeInProgress) {
                    endSwipe(x, y);
                    return true;
                } else {
                    // Reset swipe data for normal key press
                    resetSwipe();
                    return false;
                }
                
            case MotionEvent.ACTION_CANCEL:
                resetSwipe();
                return isSwipeInProgress;
        }
        
        return false;
    }
    
    private void startSwipe(float x, float y) {
        swipePoints.clear();
        swipePoints.add(new float[]{x, y});
        swipeStartTime = System.currentTimeMillis();
        swipePath.reset();
        swipePath.moveTo(x, y);
        isSwipeInProgress = false; // Will be set to true when movement is detected
    }
    
    private void continueSwipe(float x, float y) {
        swipePoints.add(new float[]{x, y});
        swipePath.lineTo(x, y);
        invalidate(); // Redraw to show swipe path
    }
    
    private void endSwipe(float x, float y) {
        if (!isSwipeInProgress) return;
        
        swipePoints.add(new float[]{x, y});
        long swipeDuration = System.currentTimeMillis() - swipeStartTime;
        
        // Calculate total swipe distance
        float totalDistance = calculateTotalDistance();
        
        // Only process as swipe if it meets minimum criteria
        if (swipeDuration >= MIN_SWIPE_TIME && totalDistance >= MIN_SWIPE_DISTANCE) {
            processSwipe();
        }
        
        resetSwipe();
        
        if (swipeListener != null) {
            swipeListener.onSwipeEnded();
        }
    }
    
    private float calculateTotalDistance() {
        if (swipePoints.size() < 2) return 0;
        
        float totalDistance = 0;
        for (int i = 1; i < swipePoints.size(); i++) {
            float[] prev = swipePoints.get(i - 1);
            float[] curr = swipePoints.get(i);
            totalDistance += Math.sqrt(
                Math.pow(curr[0] - prev[0], 2) + Math.pow(curr[1] - prev[1], 2)
            );
        }
        return totalDistance;
    }
    
    private void processSwipe() {
        if (swipeListener == null || swipePoints.isEmpty()) return;
        
        // Convert swipe points to key positions
        List<Integer> swipedKeys = new ArrayList<>();
        String swipePattern = generateSwipePattern();
        
        // Get keys that were swiped over
        for (float[] point : swipePoints) {
            int keyIndex = getKeyIndices((int)point[0], (int)point[1], null);
            if (keyIndex >= 0 && getKeyboard() != null) {
                try {
                    int keyCode = getKeyboard().getKeys().get(keyIndex).codes[0];
                    if (!swipedKeys.contains(keyCode) && Character.isLetter(keyCode)) {
                        swipedKeys.add(keyCode);
                    }
                } catch (Exception e) {
                    // Ignore errors in key detection
                }
            }
        }
        
        swipeListener.onSwipeDetected(swipedKeys, swipePattern);
    }
    
    private String generateSwipePattern() {
        if (swipePoints.size() < 2) return "";
        
        StringBuilder pattern = new StringBuilder();
        float[] start = swipePoints.get(0);
        float[] end = swipePoints.get(swipePoints.size() - 1);
        
        // Simple pattern based on start and end positions
        float deltaX = end[0] - start[0];
        float deltaY = end[1] - start[1];
        
        // Determine general direction
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            pattern.append(deltaX > 0 ? "right" : "left");
        } else {
            pattern.append(deltaY > 0 ? "down" : "up");
        }
        
        return pattern.toString();
    }
    
    private void resetSwipe() {
        isSwipeInProgress = false;
        swipePoints.clear();
        swipePath.reset();
        invalidate(); // Clear the drawn path
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw swipe path if swiping
        if (isSwipeInProgress && !swipePath.isEmpty()) {
            canvas.drawPath(swipePath, swipePaint);
        }
    }
}
