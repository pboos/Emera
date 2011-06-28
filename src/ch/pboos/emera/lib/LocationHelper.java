/*
 * Copyright (C) 2011 Tonchidot Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.pboos.emera.lib;

import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class LocationHelper implements LocationListener {
    private static final String TAG = LocationHelper.class.getSimpleName();

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static Location mDetectedLocation;
    private LocationManager mLocationManager;
    private Handler mHandler;
    private Runnable mStopLocationDetectRunnable = new Runnable() {

        @Override
        public void run() {
            stopLocationDetect();
        }
    };

    public LocationHelper(Context context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mHandler = new Handler();
    }

    public Location getLastKnownLocation() {
        List<String> matchingProviders = mLocationManager.getAllProviders();
        Location currentBestLocation = mDetectedLocation;
        for (String provider : matchingProviders) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null && isBetterLocation(location, currentBestLocation)) {
                currentBestLocation = location;
            }
        }
        return currentBestLocation;
    }

    public void tryLocationDetect() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        String provider = mLocationManager.getBestProvider(criteria, true);
        mLocationManager.requestLocationUpdates(provider, 0, 0, this);

        Criteria criteriaFast = new Criteria();
        criteriaFast.setAccuracy(Criteria.ACCURACY_COARSE);
        criteriaFast.setPowerRequirement(Criteria.POWER_LOW);
        mLocationManager.requestLocationUpdates(0, 0, criteriaFast, this, Looper.myLooper());
        // mLocationManager.requestSingleUpdate(criteriaFast, this,
        // Looper.myLooper());

        mHandler.postDelayed(mStopLocationDetectRunnable, 60 * 1000);
    }

    public void stopLocationDetect() {
        mLocationManager.removeUpdates(this);
        mHandler.removeCallbacks(mStopLocationDetectRunnable);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, mDetectedLocation)) {
            Log.i(TAG, "New location: " + location);
            mDetectedLocation = location;
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     * 
     * @param location The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to
     *            compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use
        // the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public static interface QuickLocationListener {
        void onQuickLocationReceived(Location location);
    }
}
