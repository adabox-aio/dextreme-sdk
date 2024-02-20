package io.adabox.dextreme.utils;

public class SlotConverter {

    public static long timeToSlot(long timeMillis) {
        return (timeMillis / 1000) - 1591566291;
    }

    public static long slotToTime(long slot) {
        return (slot + 1591566291) * 1000;
    }
}
