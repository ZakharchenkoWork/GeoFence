package com.znshadows.geofence;

import android.location.Location;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Created by Konstantyn Zakharchenko on 24.12.2017.
 */
@RunWith(RobolectricTestRunner.class)
public class FenceControllerTest {
    FenceController fenceController;
    Location lastLocation;
    Location centerLocation;
    int radiusDistance = 10; // meters
    String wifiName = "wifi";
    @Before
    public void setUp() throws Exception {
        fenceController = new FenceController();
        lastLocation = new Location("");
        centerLocation = new Location("");
        lastLocation.setLatitude(46.480950);
        lastLocation.setLongitude(30.7116550);
        centerLocation.setLatitude(46.480900);
        centerLocation.setLongitude(30.7116500);

    }

    @Test
    public void isDataReadyTest() throws Exception {
        fenceController.setLastLocation(lastLocation);
        assertFalse(fenceController.isDataReady());
        fenceController.setCenterLatitude(centerLocation.getLatitude());
        assertFalse(fenceController.isDataReady());
        fenceController.setCenterLongitude(centerLocation.getLongitude());
        assertFalse(fenceController.isDataReady());
        fenceController.setRadiusDistance(radiusDistance);
        assertFalse(fenceController.isDataReady());
        fenceController.setWifiFenceName(wifiName);
        assertTrue(fenceController.isDataReady());

    }

    @Test
    public void checkWifiName() throws Exception {
        fenceController.setWifiFenceName(null);
        assertFalse(fenceController.checkWifiName(""));
        assertFalse(fenceController.checkWifiName("any"));
        fenceController.setWifiFenceName("");
        assertFalse(fenceController.checkWifiName(""));
        assertFalse(fenceController.checkWifiName("any"));
        fenceController.setWifiFenceName(wifiName);
        assertFalse(fenceController.checkWifiName(""));
        assertTrue(fenceController.checkWifiName(wifiName));

    }

    @Test
    public void getDistanceToFence() throws Exception {
        isDataReadyTest();
        assertTrue(""+fenceController.getDistanceToFence(),fenceController.getDistanceToFence() == 5.5712814f);
        assertTrue(fenceController.getDistanceToFence() < fenceController.getRadiusDistance());
    }

}