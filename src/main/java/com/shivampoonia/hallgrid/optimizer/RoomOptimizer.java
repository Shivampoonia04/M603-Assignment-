package com.shivampoonia.hallgrid.optimizer;

import com.shivampoonia.hallgrid.model.CampusModel;
import com.shivampoonia.hallgrid.model.Course;
import com.shivampoonia.hallgrid.model.Placement;
import com.shivampoonia.hallgrid.model.Room;
import com.shivampoonia.hallgrid.model.Schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 2: room allocation with dynamic programming, one timeslot at a time.
 *
 * <p>State {@code dp[i][mask]} is the best cost of seating classes {@code i..n-1}
 * when the bits in {@code mask} mark rooms that are already taken. The recurrence
 * is:
 * <pre>
 *   dp[n][mask] = 0
 *   dp[i][mask] = min(
 *       SKIP + dp[i+1][mask],
 *       min over free feasible rooms r of  (cap(r)-enrol(i) + dp[i+1][mask|r])
 *   )
 * </pre>
 * {@code SKIP = 100000} is larger than any possible waste, so the optimum first
 * maximises the number of seated classes and only then minimises leftover seats.
 * That avoids enumerating every bijection of classes to rooms directly
 * ({@code P(R,k)}).
 */
public final class RoomOptimizer {

    static final int SKIP = 100_000;

    public Schedule assign(CampusModel model, Map<String, String> slotOf, String stageName) {
        int roomCount = model.rooms().size();
        if (roomCount > 20) {
            throw new IllegalStateException("Bitmask DP is limited to 20 candidate rooms per slot.");
        }

        Map<String, List<Course>> bySlot = new HashMap<>();
        for (Map.Entry<String, String> e : slotOf.entrySet()) {
            Course course = model.course(e.getKey());
            if (course != null) {
                bySlot.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(course);
            }
        }

        Map<String, Placement> placed = new LinkedHashMap<>();
        for (Course course : model.courses()) {
            if (!slotOf.containsKey(course.id())) {
                placed.put(course.id(), Placement.unscheduled(course.id()));
            }
        }

        for (Map.Entry<String, List<Course>> e : bySlot.entrySet()) {
            placeSlot(model, e.getKey(), e.getValue(), placed);
        }
        return Schedule.fromPlacements(stageName, model.courses(), placed);
    }

    private void placeSlot(CampusModel model, String slotId, List<Course> group, Map<String, Placement> placed) {
        int n = group.size();
        int roomCount = model.rooms().size();
        Integer[][] memo = new Integer[n + 1][1 << roomCount];
        int[][] pick = new int[n + 1][1 << roomCount];
        for (int i = 0; i <= n; i++) {
            java.util.Arrays.fill(pick[i], -2);
        }
        cost(model, group, 0, 0, memo, pick);

        int mask = 0;
        for (int i = 0; i < n; i++) {
            int roomIndex = pick[i][mask];
            Course course = group.get(i);
            if (roomIndex < 0) {
                placed.put(course.id(), Placement.unscheduled(course.id()));
            } else {
                Room room = model.rooms().get(roomIndex);
                placed.put(course.id(), Placement.scheduled(
                        course.id(), slotId, room.id(), room.capacity() - course.enrolled()));
                mask |= 1 << roomIndex;
            }
        }
    }

    private int cost(CampusModel model, List<Course> group, int i, int mask, Integer[][] memo, int[][] pick) {
        if (i == group.size()) {
            return 0;
        }
        if (memo[i][mask] != null) {
            return memo[i][mask];
        }
        int best = SKIP + cost(model, group, i + 1, mask, memo, pick);
        int bestRoom = -1;
        Course course = group.get(i);
        for (int r = 0; r < model.rooms().size(); r++) {
            if ((mask & (1 << r)) != 0) {
                continue;
            }
            Room room = model.rooms().get(r);
            if (room.capacity() < course.enrolled()) {
                continue;
            }
            int waste = room.capacity() - course.enrolled();
            int cand = waste + cost(model, group, i + 1, mask | (1 << r), memo, pick);
            if (cand < best) {
                best = cand;
                bestRoom = r;
            }
        }
        memo[i][mask] = best;
        pick[i][mask] = bestRoom;
        return best;
    }
}
