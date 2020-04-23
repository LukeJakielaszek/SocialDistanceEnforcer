package edu.temple.socialdistanceenforcer;

import androidx.annotation.NonNull;

public class Point {
    // known distance
    public int dist;

    // measured RSSI
    public int RSSI;

    // number of samples aggregated into this point
    public int count;

    // create our point
    public Point(int dist, int RSSI, int count){
        this.dist = dist;
        this.RSSI = RSSI;
        this.count = count;
    }

    // return the distance of this point from a supplied RSSI
    public int getDistance(int RSSI){
        return Math.abs(this.RSSI - RSSI);
    }

    @NonNull
    @Override
    public String toString() {
        return("(" + Integer.toString(dist) + "," + Integer.toString(RSSI) + "," + Integer.toString(count) + ")");
    }
}
