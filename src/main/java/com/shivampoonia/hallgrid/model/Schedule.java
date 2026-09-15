package com.shivampoonia.hallgrid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Schedule {

    private final String stageName;
    private final Map<String, Placement> byClass;

    public Schedule(String stageName, Map<String, Placement> byClass) {
        this.stageName = stageName;
        this.byClass = new LinkedHashMap<>(byClass);
    }

    public String stageName() {
        return stageName;
    }

    public Placement of(String classId) {
        return byClass.get(classId);
    }

    public List<Placement> all() {
        return new ArrayList<>(byClass.values());
    }

    public List<Placement> scheduled() {
        return byClass.values().stream().filter(Placement::isScheduled).collect(Collectors.toList());
    }

    public List<Placement> unscheduled() {
        return byClass.values().stream().filter(p -> !p.isScheduled()).collect(Collectors.toList());
    }

    public int scheduledCount() {
        return scheduled().size();
    }

    public int totalWaste() {
        return scheduled().stream().mapToInt(Placement::waste).sum();
    }

    public Map<String, String> slotOf() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Placement p : scheduled()) {
            map.put(p.classId(), p.timeslotId());
        }
        return map;
    }

    public static Schedule fromPlacements(String stageName, List<Course> courses, Map<String, Placement> found) {
        Map<String, Placement> all = new LinkedHashMap<>();
        for (Course c : courses) {
            all.put(c.id(), Objects.requireNonNullElseGet(found.get(c.id()), () -> Placement.unscheduled(c.id())));
        }
        return new Schedule(stageName, all);
    }

    public List<String> unscheduledIds() {
        return unscheduled().stream().map(Placement::classId).collect(Collectors.toList());
    }

    public Schedule relabel(String newName) {
        return new Schedule(newName, byClass);
    }

    public List<Placement> ordered() {
        List<Placement> list = new ArrayList<>(byClass.values());
        Collections.sort(list, (a, b) -> {
            if (a.isScheduled() != b.isScheduled()) {
                return a.isScheduled() ? -1 : 1;
            }
            if (!a.isScheduled()) {
                return a.classId().compareTo(b.classId());
            }
            int cmp = a.timeslotId().compareTo(b.timeslotId());
            return cmp != 0 ? cmp : a.classId().compareTo(b.classId());
        });
        return list;
    }
}
