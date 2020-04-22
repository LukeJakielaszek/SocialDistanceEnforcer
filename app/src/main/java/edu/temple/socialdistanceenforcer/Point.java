package edu.temple.socialdistanceenforcer;

import androidx.annotation.NonNull;

public class Point {
    public int dist;
    public int RSSI;
    public int count;

    public Point(int dist, int RSSI, int count){
        this.dist = dist;
        this.RSSI = RSSI;
        this.count = count;
    }

    public int getDistance(int RSSI){
        return Math.abs(this.RSSI - RSSI);
    }

    @NonNull
    @Override
    public String toString() {
        return("(" + Integer.toString(dist) + "," + Integer.toString(RSSI) + "," + Integer.toString(count) + ")");
    }
}
