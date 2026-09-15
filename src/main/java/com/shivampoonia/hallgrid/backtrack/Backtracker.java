package com.shivampoonia.hallgrid.backtrack;

import com.shivampoonia.hallgrid.model.CampusModel;
import com.shivampoonia.hallgrid.model.Course;
import com.shivampoonia.hallgrid.model.Schedule;
import com.shivampoonia.hallgrid.model.SlotSets;
import com.shivampoonia.hallgrid.model.Timeslot;
import com.shivampoonia.hallgrid.optimizer.RoomOptimizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backtracking over timeslots, scored by DP rooms. This is the stronger of
 * the two stages kept in HallGrid.
 */
public final class Backtracker {

    private static final long BUDGET_NS = 4_000_000_000L;

    private CampusModel model;
    private RoomOptimizer dp;
    private Map<String, String> hintSlots;
    private long deadline;
    private int bestCount;
    private int bestWaste;
    private Map<String, String> bestSlots;
    private Map<String, String> currentSlots;
    private Map<String, Set<String>> profBusy;
    private Map<String, Set<String>> groupBusy;
    private int placedCount;
    private int nodes;
    private boolean timedOut;

    public Schedule solve(CampusModel model, Schedule seed) {
        this.model = model;
        this.dp = new RoomOptimizer();
        this.hintSlots = seed == null ? Map.of() : seed.slotOf();
        this.deadline = System.nanoTime() + BUDGET_NS;
        this.bestSlots = new LinkedHashMap<>(this.hintSlots);
        Schedule seedRooms = this.dp.assign(model, this.bestSlots, "seed");
        this.bestCount = seedRooms.scheduledCount();
        this.bestWaste = seedRooms.totalWaste();
        this.currentSlots = new LinkedHashMap<>();
        this.profBusy = SlotSets.empty(model);
        this.groupBusy = SlotSets.empty(model);
        this.placedCount = 0;
        this.nodes = 0;
        this.timedOut = false;

        search(new ArrayList<>(model.courses()));
        return dp.assign(model, bestSlots, "Stage 1 - Backtracking (timeslots)");
    }

    public Map<String, String> bestSlots() {
        return bestSlots;
    }

    public int nodesVisited() {
        return nodes;
    }

    public boolean timedOut() {
        return timedOut;
    }

    private void search(List<Course> remaining) {
        nodes++;
        if (System.nanoTime() > deadline) {
            timedOut = true;
            consider();
            return;
        }
        if (remaining.isEmpty()) {
            consider();
            return;
        }
        if (placedCount + remaining.size() < bestCount) {
            return;
        }

        int pickIndex = mrvIndex(remaining);
        Course course = remaining.remove(pickIndex);
        List<Timeslot> options = openSlots(course);

        for (Timeslot slot : options) {
            apply(course, slot);
            search(remaining);
            undo(course, slot);
            if (timedOut) {
                remaining.add(pickIndex, course);
                return;
            }
        }

        search(remaining);
        remaining.add(pickIndex, course);
    }

    private void consider() {
        Schedule scored = dp.assign(model, currentSlots, "score");
        int count = scored.scheduledCount();
        int waste = scored.totalWaste();
        if (count > bestCount || (count == bestCount && waste < bestWaste)) {
            bestCount = count;
            bestWaste = waste;
            bestSlots = new LinkedHashMap<>(currentSlots);
        }
    }

    private int mrvIndex(List<Course> remaining) {
        int bestI = 0;
        int bestOpts = Integer.MAX_VALUE;
        int bestDeg = -1;
        for (int i = 0; i < remaining.size(); i++) {
            Course c = remaining.get(i);
            int opts = openSlots(c).size();
            int deg = model.degree(c.id());
            if (opts < bestOpts || (opts == bestOpts && deg > bestDeg)) {
                bestOpts = opts;
                bestDeg = deg;
                bestI = i;
            }
        }
        return bestI;
    }

    private List<Timeslot> openSlots(Course course) {
        List<Timeslot> list = new ArrayList<>();
        String hint = hintSlots.get(course.id());
        for (Timeslot slot : model.timeslots()) {
            if (slotOpen(course, slot.id())) {
                if (slot.id().equals(hint)) {
                    list.add(0, slot);
                } else {
                    list.add(slot);
                }
            }
        }
        return list;
    }

    private boolean slotOpen(Course course, String slotId) {
        if (profBusy.get(slotId).contains(course.professorId())) {
            return false;
        }
        return !SlotSets.groupBlocked(model, course, groupBusy.get(slotId));
    }

    private void apply(Course course, Timeslot slot) {
        currentSlots.put(course.id(), slot.id());
        profBusy.get(slot.id()).add(course.professorId());
        groupBusy.get(slot.id()).addAll(model.groupsOf(course.id()));
        placedCount++;
    }

    private void undo(Course course, Timeslot slot) {
        currentSlots.remove(course.id());
        profBusy.get(slot.id()).remove(course.professorId());
        for (String g : model.groupsOf(course.id())) {
            groupBusy.get(slot.id()).remove(g);
        }
        placedCount--;
    }
}
