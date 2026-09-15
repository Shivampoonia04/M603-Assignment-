package com.shivampoonia.hallgrid.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SlotSets {

    private SlotSets() {
    }

    public static Map<String, Set<String>> empty(CampusModel model) {
        Map<String, Set<String>> map = new HashMap<>();
        for (Timeslot t : model.timeslots()) {
            map.put(t.id(), new HashSet<>());
        }
        return map;
    }

    public static boolean groupBlocked(CampusModel model, Course course, Set<String> busyGroups) {
        for (String g : model.groupsOf(course.id())) {
            if (busyGroups.contains(g)) {
                return true;
            }
        }
        return false;
    }
}
