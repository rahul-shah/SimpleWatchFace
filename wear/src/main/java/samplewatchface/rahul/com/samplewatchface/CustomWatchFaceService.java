package samplewatchface.rahul.com.samplewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

public class CustomWatchFaceService extends CanvasWatchFaceService
{
    @Override
    public Engine onCreateEngine() {
        //Associates watch face service with code that drives display
        return new WatchFaceEngine();
    }

    private class WatchFaceEngine extends Engine
    {
        // Handles system events like screen turn off or going to ambient mode

        //Drives the watchface

        //Handles
        // 1. Timers
        //2. Displaying user interface
        //3. Moving in and out of ambient mode
        //4. Getting info about physical watch display

        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create(Typeface.SERIF,Typeface.NORMAL);
        private static final int MSG_UPDATE_TIME_ID = 42;
        private long mUpdateRateMs = 1000;

        private Time mDisplayTime;
        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsInMuteMode;
        private boolean mIsLowBitAmbient;

        private float mXOffset;
        private float mYOffset;

        private int mBackgroundColor = Color.parseColor("black");
        private int mTextColor = Color.parseColor("red");

        //Broadcast for resetting the timezone
        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear(intent.getStringExtra("time-zone"));
                mDisplayTime.setToNow();
            }
        };

        private final Handler mTimeHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                switch ((msg.what))
                {
                    case MSG_UPDATE_TIME_ID:
                    {
                        //invalidate current view for redrawing
                        invalidate();

                        //check if screen is visible and not in ambient mode
                        if(isVisible() && !isInAmbientMode())
                        {
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - (currentTimeMillis % mUpdateRateMs);
                            mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME_ID,delay);
                        }
                    }
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            //Background of notification cards to briefly show if card type is interruptive.

            setWatchFaceStyle(new WatchFaceStyle.Builder(CustomWatchFaceService.this)
                                . setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                                . setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                                . setShowSystemUiTime(false)
                                . build());

            mDisplayTime = new Time();

            initBackground();
            initDisplayText();
        }

        private void initBackground()
        {
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor(mBackgroundColor);
        }

        private void initDisplayText()
        {
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor(mTextColor);
            mTextColorPaint.setTypeface(WATCH_TEXT_TYPEFACE);
            mTextColorPaint.setAntiAlias(true);
            mTextColorPaint.setTextSize(getResources().getDimension(R.dimen.text_size));
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            // Check if watch face is visible
            if(visible)
            {
                //If watch face visible, check if broadcast receiver is registered
                if(!mHasTimeZoneReceiverBeenRegistered)
                {
                    //if not registered, register it
                    IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                    CustomWatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver,filter);
                    mHasTimeZoneReceiverBeenRegistered = true;
                }

                mDisplayTime.clear(TimeZone.getDefault().getID());
                mDisplayTime.setToNow();
            }
            //If watch face is not visible, check if broadcast receiver can be unregistered
            else
            {
                if(mHasTimeZoneReceiverBeenRegistered)
                {
                    CustomWatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            //invalidate and redraw the watch face
            updateTimer();
        }

        private void updateTimer()
        {
            //stops pending handler actions and check to see if another should be sent
            mTimeHandler.removeMessages(MSG_UPDATE_TIME_ID);
            if(isVisible() && !isInAmbientMode())
            {
                mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME_ID);
            }
        }

        //When service is associated with android wear -> onApplyWindowInsets() called
        //Used to determine if watch is round or square
        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mYOffset = getResources().getDimension(R.dimen.y_offset);

            if(insets.isRound())
            {
                mXOffset = getResources().getDimension(R.dimen.x_offset_round);
            }
            else
            {
                mXOffset = getResources().getDimension(R.dimen.x_offset_square);
            }
        }

        //Called when h/w properties of wear device are determined
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            if(properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,false))
            {
                mIsLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT,false);
            }
        }

        //Called when device moves in or out of ambient mode
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            //In ambient mode, change watch face to black and white to save battery
            if(inAmbientMode)
            {
                mTextColorPaint.setColor((Color.parseColor("white")));
            }
            //reverse when leaving ambient mode
            else
            {
                mTextColorPaint.setColor(Color.parseColor("red"));
            }

            if(mIsLowBitAmbient)
            {
                mTextColorPaint.setAntiAlias(!inAmbientMode);
            }

            invalidate();
            updateTimer();
        }
    }
}