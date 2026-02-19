package Hermesv7.util;
import battlecode.common.*;
import Hermesv7.*;
public class BytecodeUtil {
    public static int getUsedPercent() {
        int limit = Globals.myType.getBytecodeLimit();
        int used = Clock.getBytecodeNum();
        return (used * 100) / limit;
    }
    public static boolean isLow() {
        return Clock.getBytecodesLeft() < 1000;
    }
}