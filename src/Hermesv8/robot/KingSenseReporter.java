package Hermesv8.robot;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;
public class KingSenseReporter {
    private static int lastCheeseAmount = 0;
    public static void report() throws GameActionException {
        if (Clock.getBytecodesLeft() < 1500) return;
        for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 500; i++) {
            MapInfo info = Globals.nearbyMapInfos[i];
            if (info.hasCheeseMine()) {
                CommArray.reportCheeseMine(info.getMapLocation());
            }
        }
        for (int i = 0; i < Globals.nearbyCats.length && Clock.getBytecodesLeft() > 300; i++) {
            CommArray.reportCat(Globals.nearbyCats[i].getLocation());
        }
        for (int i = 0; i < Globals.nearbyEnemies.length && Clock.getBytecodesLeft() > 300; i++) {
            RobotInfo enemy = Globals.nearbyEnemies[i];
            if (enemy.getType() == UnitType.RAT_KING) {
                CommArray.reportEnemyKing(enemy.getLocation());
            }
        }
        int currentCheese = Globals.rc.getRawCheese();
        if (currentCheese > lastCheeseAmount) {
            int transferAmount = currentCheese - lastCheeseAmount;
            CommArray.addCheeseTransfer(transferAmount);
        }
        lastCheeseAmount = currentCheese;
        if (Clock.getBytecodesLeft() > 1000) {
            Message[] squeaks = Globals.rc.readSqueaks(-1);
            if (squeaks != null && squeaks.length > 0) {
                MapLocation myLoc = Globals.myLoc;
                for (int i = 0; i < squeaks.length && i < 10 && Clock.getBytecodesLeft() > 200; i++) {
                    Message squeak = squeaks[i];
                    if (squeak == null) continue;
                    int content = squeak.getBytes();
                    int type = content & 0xF0000000;
                    MapLocation loc = CommArray.unpackLocation(content & 0x0FFFFFFF);
                    if (type == Squeaker.SQUEAK_CHEESE && loc != null && loc.equals(myLoc)) {
                        CommArray.addCheeseTransfer(1);
                        break;
                    }
                }
            }
        }
    }
}