package com.shivampoonia.hallgrid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScheduleGuard {

    private ScheduleGuard() {
    }

    public static List<String> violations(CampusModel model, Schedule schedule) {
        List<String> issues = new ArrayList<>();
        Map<String, List<Placement>> bySlot = new HashMap<>();
        for (Placement p : schedule.scheduled()) {
            Course course = model.course(p.classId());
            if (course == null) {
                issues.add("Unknown class " + p.classId());
                continue;
            }
            Room room = room(model, p.roomId());
            if (room == null) {
                issues.add(p.classId() + " uses unknown room " + p.roomId());
                continue;
            }
            if (room.capacity() < course.enrolled()) {
                issues.add(p.classId() + " overfills " + room.id());
            }
            bySlot.computeIfAbsent(p.timeslotId(), k -> new ArrayList<>()).add(p);
        }
        for (Map.Entry<String, List<Placement>> e : bySlot.entrySet()) {
            Set<String> rooms = new HashSet<>();
            Set<String> profs = new HashSet<>();
            Set<String> groups = new HashSet<>();
            for (Placement p : e.getValue()) {
                Course course = model.course(p.classId());
                if (!rooms.add(p.roomId())) {
                    issues.add("Double-booked room " + p.roomId() + " at " + e.getKey());
                }
                if (!profs.add(course.professorId())) {
                    issues.add("Professor clash " + course.professorId() + " at " + e.getKey());
                }
                for (String g : model.groupsOf(course.id())) {
                    if (!groups.add(g)) {
                        issues.add("Student group clash " + g + " at " + e.getKey());
                    }
                }
            }
        }
        return issues;
    }

    private static Room room(CampusModel model, String id) {
        for (Room r : model.rooms()) {
            if (r.id().equals(id)) {
                return r;
            }
        }
        return null;
    }
}
