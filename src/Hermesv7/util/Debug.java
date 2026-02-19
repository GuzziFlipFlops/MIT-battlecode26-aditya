package Hermesv7.util;
import battlecode.common.*;
import Hermesv7.*;
public class Debug {
    public static void setIndicator(String msg) {
        try {
            Globals.rc.setIndicatorString(msg);
        } catch (Exception e) {
        }
    }
    public static void dot(MapLocation loc, int r, int g, int b) {
        try {
            Globals.rc.setIndicatorDot(loc, r, g, b);
        } catch (Exception e) {
        }
    }
    public static void line(MapLocation from, MapLocation to, int r, int g, int b) {
        try {
            Globals.rc.setIndicatorLine(from, to, r, g, b);
        } catch (Exception e) {
        }
    }
}