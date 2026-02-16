package com.example.batteryalert;

public class BatteryLogic {
    public enum AlertLevel {
        NONE, NORMAL, URGENT, CRITICAL
    }

    public static AlertLevel getAlertLevel(float batteryPct, int threshold, int urgentOffset, int criticalOffset) {
        if (batteryPct > threshold) {
            return AlertLevel.NONE;
        } else if (batteryPct <= threshold - criticalOffset) {
            return AlertLevel.CRITICAL;
        } else if (batteryPct <= threshold - urgentOffset) {
            return AlertLevel.URGENT;
        } else {
            return AlertLevel.NORMAL;
        }
    }
}
