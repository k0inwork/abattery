package com.example.batteryalert;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BatteryLogicTest {

    @Test
    public void testAlertLevels() {
        int threshold = 20;
        int urgentOffset = 5;
        int criticalOffset = 10;

        // Above threshold
        assertEquals(BatteryLogic.AlertLevel.NONE, BatteryLogic.getAlertLevel(25, threshold, urgentOffset, criticalOffset));
        assertEquals(BatteryLogic.AlertLevel.NONE, BatteryLogic.getAlertLevel(21, threshold, urgentOffset, criticalOffset));

        // Normal alert (<= threshold, but > threshold - urgentOffset)
        assertEquals(BatteryLogic.AlertLevel.NORMAL, BatteryLogic.getAlertLevel(20, threshold, urgentOffset, criticalOffset));
        assertEquals(BatteryLogic.AlertLevel.NORMAL, BatteryLogic.getAlertLevel(16, threshold, urgentOffset, criticalOffset));

        // Urgent alert (<= threshold - urgentOffset, but > threshold - criticalOffset)
        assertEquals(BatteryLogic.AlertLevel.URGENT, BatteryLogic.getAlertLevel(15, threshold, urgentOffset, criticalOffset));
        assertEquals(BatteryLogic.AlertLevel.URGENT, BatteryLogic.getAlertLevel(11, threshold, urgentOffset, criticalOffset));

        // Critical alert (<= threshold - criticalOffset)
        assertEquals(BatteryLogic.AlertLevel.CRITICAL, BatteryLogic.getAlertLevel(10, threshold, urgentOffset, criticalOffset));
        assertEquals(BatteryLogic.AlertLevel.CRITICAL, BatteryLogic.getAlertLevel(5, threshold, urgentOffset, criticalOffset));
        assertEquals(BatteryLogic.AlertLevel.CRITICAL, BatteryLogic.getAlertLevel(0, threshold, urgentOffset, criticalOffset));
    }
}
