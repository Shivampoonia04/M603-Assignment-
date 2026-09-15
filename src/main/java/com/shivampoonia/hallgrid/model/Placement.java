package com.shivampoonia.hallgrid.model;

public record Placement(String classId, String timeslotId, String roomId, int waste, String remark) {

    public static Placement scheduled(String classId, String timeslotId, String roomId, int waste) {
        String remark = waste == 0 ? "Perfect Fit" : "Wasted " + waste + " seats";
        return new Placement(classId, timeslotId, roomId, waste, remark);
    }

    public static Placement unscheduled(String classId) {
        return new Placement(classId, null, null, 0, "Unscheduled");
    }

    public boolean isScheduled() {
        return timeslotId != null && roomId != null;
    }
}
