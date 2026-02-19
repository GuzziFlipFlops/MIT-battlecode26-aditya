package Hermesv3.strategy;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;

public class StrategicDecisions {

    public static void make() throws GameActionException {
        BackstabEvaluator.evaluate();
        
        int phase = GamePhase.calculate();
        CommArray.setFlag(CommArray.FLAG_GAME_PHASE, phase);
    }
}
