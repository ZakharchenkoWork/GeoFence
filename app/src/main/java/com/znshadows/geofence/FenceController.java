package com.znshadows.geofence;

import android.location.Location;

/**
 * Created by Konstantyn Zakharchenko on 24.12.2017.
 */

public class FenceController {
    private Location lastLocation = null;
    private Location fenceCenterLocation = new Location("");
    private int radiusDistance = 0;
    private String wifiFenceName = "";

    public Location getLastLocation() {
        return lastLocation;
    }


    public void setCenterLatitude(double latitude) {
        fenceCenterLocation.setLatitude(latitude);
    }

    public void setCenterLongitude(double longitude) {
        fenceCenterLocation.setLongitude(longitude);
    }

    public void setLastLocation(Location lastLocation) {
        if(lastLocation != null) {
            this.lastLocation = lastLocation;
        }
    }

    public int getRadiusDistance() {
        return radiusDistance;
    }

    public void setRadiusDistance(int radiusDistance) {
        this.radiusDistance = radiusDistance;
    }

    public void setWifiFenceName(String wifiFenceName) {
        if (wifiFenceName == null) {
            this.wifiFenceName = "";
        } else {
            this.wifiFenceName = wifiFenceName;
        }
    }


    public boolean isDataReady() {
        return lastLocation != null && fenceCenterLocation != null && wifiFenceName != null && !wifiFenceName.equals("") && radiusDistance != 0;
    }

    public boolean checkWifiName(String wifiName) {
        if (wifiFenceName != null &&!wifiFenceName.equals("") && wifiFenceName.equals(wifiName)) {
            return true;
        } else {
            return false;
        }
    }
    public float getDistanceToFence(){
        float[] results = new float[1];
        Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), fenceCenterLocation.getLatitude(), fenceCenterLocation.getLongitude(), results);
        return results[0];

    }
}
