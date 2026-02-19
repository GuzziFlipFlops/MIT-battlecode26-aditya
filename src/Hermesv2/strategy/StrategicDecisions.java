package Hermesv2.strategy;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;

public class StrategicDecisions {

    public static void make() throws GameActionException {
        BackstabEvaluator.evaluate();
        
        int phase = GamePhase.calculate();
        CommArray.setFlag(CommArray.FLAG_GAME_PHASE, phase);
    }
}
