package com.example.android.sunshine.app.wearable;

import android.util.Log;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by jill on 3/31/16.
 */
public class SunshineWearableListenerService extends WearableListenerService {

    private static final String TAG = SunshineWearableListenerService.class.getSimpleName();

    private static final String WEATHER_PATH = "/weather";

    //This event gets generated in SunshineWatchFaceService.onConnect().
    //Call the sync adapter, which will retrieve the latest forecast and generate
    //a WEATHER_FORECAST_PATH message (handled by SunshineWatchFaceService.onDataChanged).
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                String path = dataEvent.getDataItem().getUri().getPath();
                Log.d(TAG, path);
                if (path.equals(WEATHER_PATH)) {
                    Log.d(TAG, "Syncing now");
                    SunshineSyncAdapter.syncImmediately(this);
                } else {
                    Log.d(TAG, "Ignoring " + path);
                }
            }
        }
    }
}
