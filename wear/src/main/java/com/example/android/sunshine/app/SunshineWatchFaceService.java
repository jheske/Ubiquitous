package com.example.android.sunshine.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by jill on 3/28/16.
 *
 * http://developer.android.com/training/wearables/data-layer/events.html
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private final String TAG = getClass().getSimpleName();

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    static final int MSG_UPDATE_TIME = 0;

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        Log.i(TAG,"onCreateEngine");
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        //Handler to update the time periodically in interactive mode.
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        //Prevent exception caused by unregistering an unregistered receiver
        boolean mRegisteredTimeZoneReceiver = false;

        //Text placement
        float mLineHeight;
        float mXOffsetTime;
        float mXOffsetTimeAmbient;
        float mXOffsetDate;
        float mYOffsetTime;
        float mYOffsetDate;
        float mYOffsetDivider;
        float mYOffsetWeather;
        //Colors
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mTextDatePaint;
        Paint mWeatherDividerPaint;
        Paint mTextDateAmbientPaint;
        Paint mTextTempHighPaint;
        Paint mTextTempLowPaint;
        Paint mTextTempLowAmbientPaint;
        Bitmap mWeatherIcon;

        //Booleans default to false, but set anyway for clarity
        boolean mAmbient=false;
        //Whether the display supports fewer bits for each color in ambient mode. When true, we
        //disable anti-aliasing in ambient mode.
        boolean mLowBitAmbient=false;

        //Calendar replaces deprecated Time used by older samples
        private Calendar mCalendar;

        //Weather strings
        private static final String WEATHER_PATH = "/weather";
        private static final String WEATHER_FORECAST_PATH = "/weather-forecast";
        private static final String KEY_HIGH = "high";
        private static final String KEY_LOW = "low";
        private static final String KEY_WEATHER_ID = "weatherId";
        String mHighTemp;
        String mLowTemp;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                long now = System.currentTimeMillis();
                mCalendar.setTimeInMillis(now);
            }
        };

        /* Play Services API for communicating with wearables */
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Service context = SunshineWatchFaceService.this;

            setWatchFaceStyle(new WatchFaceStyle.Builder(context)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFaceService.this.getResources();
            mYOffsetTime = resources.getDimension(R.dimen.digital_time_y_offset);
            mYOffsetDate = resources.getDimension(R.dimen.digital_date_y_offset);
            mYOffsetDivider = resources.getDimension(R.dimen.digital_divider_y_offset);
            mYOffsetWeather = resources.getDimension(R.dimen.digital_weather_y_offset);

            mLineHeight = resources.getDimension(R.dimen.digital_line_height);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.primary));
            mTextPaint = new Paint();
            mTextPaint = createTextPaint(ContextCompat.getColor(context, R.color.digital_text));

            mTextDatePaint = new Paint();
            mTextDatePaint = createTextPaint(ContextCompat.getColor(context,R.color.primary_light));


            mWeatherDividerPaint = new Paint();
            mWeatherDividerPaint = createTextPaint(ContextCompat.getColor(context,R.color.primary_weather));


            mTextDateAmbientPaint = new Paint();
            mTextDateAmbientPaint = createTextPaint(Color.WHITE);

            mTextTempHighPaint = createTextPaint(Color.WHITE,BOLD_TYPEFACE);
            mTextTempLowPaint = createTextPaint(ContextCompat.getColor(context,R.color.primary_light));
            mTextTempLowAmbientPaint = createTextPaint(Color.WHITE);

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mGoogleApiClient.connect();

                registerTimeZoneReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                long now = System.currentTimeMillis();
                mCalendar.setTimeInMillis(now);
            } else {
                unregisterTimeZoneReceiver();

                //Remove the listener if we're not visible to avoid leaking resources.
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffsetTime = resources.getDimension(isRound
                    ? R.dimen.digital_time_x_offset_round : R.dimen.digital_time_x_offset);
            mXOffsetDate = resources.getDimension(isRound
                    ? R.dimen.digital_date_x_offset_round : R.dimen.digital_date_x_offset);
            mXOffsetTimeAmbient = resources.getDimension(isRound
                    ? R.dimen.digital_time_x_offset_round_ambient : R.dimen.digital_time_x_offset_ambient);
            float timeTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_time_text_size_round : R.dimen.digital_time_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            float tempTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_temp_text_size_round : R.dimen.digital_temp_text_size);

            mTextPaint.setTextSize(timeTextSize);
            mTextDatePaint.setTextSize(dateTextSize);
            mTextDateAmbientPaint.setTextSize(dateTextSize);
            mTextTempHighPaint.setTextSize(tempTextSize);
            mTextTempLowAmbientPaint.setTextSize(tempTextSize);
            mTextTempLowPaint.setTextSize(tempTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mTextDatePaint.setAntiAlias(!inAmbientMode);
                    mTextDateAmbientPaint.setAntiAlias(!inAmbientMode);
                    mTextTempHighPaint.setAntiAlias(!inAmbientMode);
                    mTextTempLowAmbientPaint.setAntiAlias(!inAmbientMode);
                    mTextTempLowPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (mAmbient) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            boolean is24Hour = DateFormat.is24HourFormat(SunshineWatchFaceService.this);

            int minute = mCalendar.get(Calendar.MINUTE);
            int second = mCalendar.get(Calendar.SECOND);
            int am_pm  = mCalendar.get(Calendar.AM_PM);

            String timeText;
            if (is24Hour) {
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                timeText = mAmbient
                        ? String.format("%02d:%02d", hour, minute)
                        : String.format("%02d:%02d:%02d", hour, minute, second);
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }

                String amPmText = Utility.getAmPmString(getResources(),am_pm);

                timeText = mAmbient
                        ? String.format("%d:%02d %s", hour, minute, amPmText)
                        : String.format("%d:%02d:%02d %s", hour, minute, second, amPmText);
            }

            float xOffsetTime = mTextPaint.measureText(timeText) / 2;
            canvas.drawText(timeText, bounds.centerX() - xOffsetTime, mYOffsetTime, mTextPaint);

            // Decide which paint to use for the next bits dependent on ambient mode.
            Paint datePaint = mAmbient ? mTextDateAmbientPaint : mTextDatePaint;

            // Draw the date
            String dayOfWeekString = Utility.getDayOfWeekString(getResources(),
                    mCalendar.get(Calendar.DAY_OF_WEEK));
            String monthOfYearString = Utility.getMonthOfYearString(getResources(),
                    mCalendar.get(Calendar.MONTH));

            int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
            int year = mCalendar.get(Calendar.YEAR);

            String dateText = String.format("%s, %s %d %d", dayOfWeekString, monthOfYearString, dayOfMonth, year);
            float xOffsetDate = datePaint.measureText(dateText) / 2;
            canvas.drawText(dateText, bounds.centerX() - xOffsetDate, mYOffsetDate, datePaint);

            // Draw a line to separate date and time from weather elements
//          canvas.drawLine(bounds.centerX() - 20, mYOffsetDivider, bounds.centerX() + 20, mYOffsetDivider, datePaint);

            // Draw high and low temp if we have it
            if (mHighTemp != null && mLowTemp != null) {
                // Draw a line to separate date and time from weather elements
                canvas.drawLine(bounds.centerX() - 20, mYOffsetDivider, bounds.centerX() + 20,
                        mYOffsetDivider, mWeatherDividerPaint);
                float highTextLen = mTextTempHighPaint.measureText(mHighTemp);

                if (mAmbient) {
                    float lowTextLen = mTextTempLowAmbientPaint.measureText(mLowTemp);
                    float xOffset = bounds.centerX() - ((highTextLen + lowTextLen + 20) / 2);
                    canvas.drawText(mHighTemp, xOffset, mYOffsetWeather, mTextTempHighPaint);
                    canvas.drawText(mLowTemp, xOffset + highTextLen + 20, mYOffsetWeather, mTextTempLowAmbientPaint);
                } else {
                    float xOffset = bounds.centerX() - (highTextLen / 2);
                    canvas.drawText(mHighTemp, xOffset, mYOffsetWeather, mTextTempHighPaint);
                    canvas.drawText(mLowTemp, bounds.centerX() + (highTextLen / 2) + 20, mYOffsetWeather, mTextTempLowPaint);
                    float iconXOffset = bounds.centerX() - ((highTextLen / 2) + mWeatherIcon.getWidth() + 30);
                    canvas.drawBitmap(mWeatherIcon, iconXOffset, mYOffsetWeather - mWeatherIcon.getHeight(), null);
                }
            }
        }

        private Paint createTextPaint(int textColor) {
            return createTextPaint(textColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
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

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /** DataApi.DataListener interface methods **/
        //Once we add our DataApi listener to Google Play Services, we will receive
        //onDataChanged events.
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    Log.d(TAG, "[onDataChanged] " + path);
                    if (path.equals(WEATHER_FORECAST_PATH)) {
                        if (dataMap.containsKey(KEY_HIGH)) {
                            mHighTemp = dataMap.getString(KEY_HIGH);
                            Log.d(TAG, "High = " + mHighTemp);
                        } else {
                            Log.d(TAG, "Missing high temp");
                        }

                        if (dataMap.containsKey(KEY_LOW)) {
                            mLowTemp = dataMap.getString(KEY_LOW);
                            Log.d(TAG, "Low = " + mLowTemp);
                        } else {
                            Log.d(TAG, "Missing low temp");
                        }

                        if (dataMap.containsKey(KEY_WEATHER_ID)) {
                            int weatherId = dataMap.getInt(KEY_WEATHER_ID);
                            Log.d(TAG, "WeatherId = " + weatherId);
                            Drawable b = getResources().getDrawable(Utility.getIconResourceForWeatherCondition(weatherId));
                            Bitmap icon = ((BitmapDrawable) b).getBitmap();
                            float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
                            mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);

                        } else {
                            Log.d(TAG, "Missing WeatherId");
                        }

                        invalidate();
                    }
                }
            }
        }

        public void getWeatherInfoFromDevice() {
            Log.i(TAG,"getWeatherInfoFromDevice");
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
            putDataMapRequest.getDataMap().putString("uuid", UUID.randomUUID().toString());
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.i(TAG, "Failed asking phone for weather data");
                            } else {
                                Log.i(TAG, "Successfully asked for weather data");
                            }
                        }
                    });
        }

        /** GoogleApiClient.ConnectionCallbacks interface methods **/
        @Override
        public void onConnected(Bundle bundle) {
            //Notify Google Play Services that this service wants to listen for DataApi events
            Log.d(TAG,"onConnected");
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);

            getWeatherInfoFromDevice();
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        /** GoogleApiClient.OnConnectionFailedListener interface methods **/
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }

    }  //Engine inner class

    /*
     * Subclass the Handler.  Adding the Handler directly to the Engine class, as shown
     * in the Google WatchFace sample, produces a warning: "This Handler class should be static
     * or leaks might occur"
     */
    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFaceService.Engine> mEngineWeakReference;

        public EngineHandler(SunshineWatchFaceService.Engine engineReference) {
            mEngineWeakReference = new WeakReference<>(engineReference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFaceService.Engine engine = mEngineWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    } //EngineHandler inner class
}
