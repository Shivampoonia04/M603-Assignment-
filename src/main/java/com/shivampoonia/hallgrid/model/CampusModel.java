package com.shivampoonia.hallgrid.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CampusModel {

    private final String campus;
    private final List<Timeslot> timeslots;
    private final List<Room> rooms;
    private final List<Course> courses;
    private final List<Professor> professors;
    private final Map<String, List<String>> groupsToCourses;
    private final Map<String, List<String>> courseToGroups;
    private final Map<String, Course> courseById;
    private final Map<String, Set<String>> adjacency;
    private final Map<String, Integer> degree;

    public CampusModel(String campus,
                       List<Timeslot> timeslots,
                       List<Room> rooms,
                       List<Course> courses,
                       List<Professor> professors,
                       Map<String, List<String>> groupsToCourses) {
        this.campus = campus;
        this.timeslots = List.copyOf(timeslots);
        this.rooms = List.copyOf(rooms);
        this.courses = List.copyOf(courses);
        this.professors = professors == null ? List.of() : List.copyOf(professors);
        this.groupsToCourses = copyLists(groupsToCourses);
        this.courseById = new LinkedHashMap<>();
        for (Course c : this.courses) {
            courseById.put(c.id(), c);
        }
        this.courseToGroups = invertGroups(this.groupsToCourses);
        this.adjacency = buildAdjacency();
        this.degree = new LinkedHashMap<>();
        for (Course c : this.courses) {
            degree.put(c.id(), adjacency.get(c.id()).size());
        }
    }

    private static Map<String, List<String>> copyLists(Map<String, List<String>> src) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (src == null) {
            return copy;
        }
        for (Map.Entry<String, List<String>> e : src.entrySet()) {
            copy.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return copy;
    }

    private Map<String, List<String>> invertGroups(Map<String, List<String>> groups) {
        Map<String, List<String>> invert = new LinkedHashMap<>();
        for (Course c : courses) {
            invert.put(c.id(), new ArrayList<>());
        }
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            for (String classId : e.getValue()) {
                invert.computeIfAbsent(classId, k -> new ArrayList<>()).add(e.getKey());
            }
        }
        for (Map.Entry<String, List<String>> e : invert.entrySet()) {
            e.setValue(List.copyOf(e.getValue()));
        }
        return invert;
    }

    private Map<String, Set<String>> buildAdjacency() {
        Map<String, Set<String>> adj = new LinkedHashMap<>();
        for (Course c : courses) {
            adj.put(c.id(), new HashSet<>());
        }
        for (int i = 0; i < courses.size(); i++) {
            Course a = courses.get(i);
            for (int j = i + 1; j < courses.size(); j++) {
                Course b = courses.get(j);
                if (conflicts(a, b)) {
                    adj.get(a.id()).add(b.id());
                    adj.get(b.id()).add(a.id());
                }
            }
        }
        return adj;
    }

    public boolean conflicts(Course a, Course b) {
        if (a.id().equals(b.id())) {
            return false;
        }
        if (a.professorId().equals(b.professorId())) {
            return true;
        }
        Set<String> ga = new HashSet<>(groupsOf(a.id()));
        ga.retainAll(groupsOf(b.id()));
        return !ga.isEmpty();
    }

    public String campus() {
        return campus;
    }

    public List<Timeslot> timeslots() {
        return timeslots;
    }

    public List<Room> rooms() {
        return rooms;
    }

    public List<Course> courses() {
        return courses;
    }

    public List<Professor> professors() {
        return professors;
    }

    public Course course(String id) {
        return courseById.get(id);
    }

    public List<String> groupsOf(String classId) {
        return courseToGroups.getOrDefault(classId, List.of());
    }

    public Map<String, List<String>> groupsToCourses() {
        return groupsToCourses;
    }

    public Set<String> neighbours(String classId) {
        return Collections.unmodifiableSet(adjacency.getOrDefault(classId, Set.of()));
    }

    public int degree(String classId) {
        return degree.getOrDefault(classId, 0);
    }

    public int cliqueHint() {
        int max = 0;
        for (List<String> members : groupsToCourses.values()) {
            max = Math.max(max, members.size());
        }
        return max;
    }

    public Room smallestFit(int enrolled, Set<String> takenRooms) {
        Room best = null;
        for (Room r : rooms) {
            if (takenRooms.contains(r.id()) || r.capacity() < enrolled) {
                continue;
            }
            if (best == null || r.capacity() < best.capacity()) {
                best = r;
            }
        }
        return best;
    }

    public Room firstFit(int enrolled, Set<String> takenRooms) {
        for (Room r : rooms) {
            if (!takenRooms.contains(r.id()) && r.capacity() >= enrolled) {
                return r;
            }
        }
        return null;
    }

    public String timeslotLabel(String id) {
        for (Timeslot t : timeslots) {
            if (t.id().equals(id)) {
                return t.label();
            }
        }
        return id;
    }

    public Map<String, String> professorNames() {
        Map<String, String> map = new HashMap<>();
        for (Professor p : professors) {
            map.put(p.id(), p.name());
        }
        return map;
    }
}
