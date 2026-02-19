package Hermes.strategy;

import battlecode.common.*;
import Hermes.*;

public class GamePhase {

    public static final int PHASE_EARLY = 0;
    public static final int PHASE_MID = 1;
    public static final int PHASE_LATE = 2;

    public static int calculate() {
        if (Globals.roundNum <= 300) {
            return PHASE_EARLY;
        } else if (Globals.roundNum <= 1000) {
            return PHASE_MID;
        } else {
            return PHASE_LATE;
        }
    }

    public static int getTargetArmySize() {
        int phase = calculate();
        switch (phase) {
            case PHASE_EARLY:
                return 15;
            case PHASE_MID:
                return 25;
            case PHASE_LATE:
                return 35;
            default:
                return 20;
        }
    }
}
