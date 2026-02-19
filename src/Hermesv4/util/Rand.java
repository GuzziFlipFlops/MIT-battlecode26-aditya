package Hermesv4.util;

import battlecode.common.*;
import Hermesv4.*;

public class Rand {

    private static int seed = 0;
    private static boolean initialized = false;

    private static void init() {
        if (!initialized) {
            seed = Globals.myID;
            initialized = true;
        }
    }

    public static int next(int max) {
        init();
        seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
        return seed % max;
    }

    public static Direction randomDirection() {
        return Globals.ALL_DIRECTIONS[next(8)];
    }
}
