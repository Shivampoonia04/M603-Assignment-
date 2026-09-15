package com.shivampoonia.hallgrid;

import com.shivampoonia.hallgrid.backtrack.Backtracker;
import com.shivampoonia.hallgrid.io.ConstraintLoader;
import com.shivampoonia.hallgrid.model.CampusModel;
import com.shivampoonia.hallgrid.model.Schedule;
import com.shivampoonia.hallgrid.model.ScheduleGuard;
import com.shivampoonia.hallgrid.optimizer.RoomOptimizer;
import com.shivampoonia.hallgrid.report.ReportWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * HallGrid – the two strongest stages only: backtracking + DP.
 * Author: Shivam · GH1061529
 */
public final class HallGridApp {

    public static void main(String[] args) throws Exception {
        Path dataFile = resolveData(args);
        CampusModel model = ConstraintLoader.load(dataFile);

        System.out.println("HallGrid  |  " + model.campus());
        System.out.println("Author    |  Shivam  GH1061529");
        System.out.println("Loaded    |  " + model.courses().size() + " classes, "
                + model.rooms().size() + " rooms, " + model.timeslots().size() + " slots");
        System.out.println("Clique    |  largest student-group bundle = " + model.cliqueHint()
                + " (slots = " + model.timeslots().size() + ")");
        System.out.println();

        Backtracker back = new Backtracker();
        RoomOptimizer dp = new RoomOptimizer();
        Schedule timeslots = back.solve(model, null);
        Schedule rooms = dp.assign(model, back.bestSlots(), "Stage 2 - Dynamic programming (rooms)");
        timeslots = timeslots.relabel("Stage 1 - Backtracking (timeslots)");

        List<Schedule> stages = List.of(timeslots, rooms);
        for (Schedule stage : stages) {
            printStage(model, stage);
            List<String> issues = ScheduleGuard.violations(model, stage);
            if (!issues.isEmpty()) {
                System.out.println("  VALIDATION " + issues);
            }
        }

        System.out.println("Backtracking nodes " + back.nodesVisited()
                + (back.timedOut() ? " (stopped at time budget)" : " (search finished)"));

        String manual = """
                Manual fix log
                - MSC-AI-Y1 needs %d modules in a %d-slot block, so at least one leftover is structural.
                - A registrar should park the leftover seminar on a Thursday overflow, or split the GPU lab into two cohorts.
                - Do not force it into an occupied AI slot: that would put Year 1 AI students in two rooms at once.
                """.formatted(model.cliqueHint(), model.timeslots().size());

        Path outDir = projectRoot(dataFile).resolve("output");
        ReportWriter.writeText(outDir.resolve("conflict-report.txt"), model, stages, manual);
        ReportWriter.writeHtml(outDir.resolve("schedule.html"), model, stages);
        System.out.println("Wrote " + outDir.resolve("conflict-report.txt").toAbsolutePath());
        System.out.println("Wrote " + outDir.resolve("schedule.html").toAbsolutePath());
    }

    private static void printStage(CampusModel model, Schedule stage) {
        System.out.println("=== " + ReportWriter.summary(stage) + " ===");
        for (String line : ReportWriter.lines(model, stage)) {
            System.out.println(line);
        }
        System.out.println();
    }

    static Path resolveData(String[] args) {
        if (args != null && args.length > 0) {
            return Path.of(args[0]);
        }
        Path[] candidates = {
                Path.of("data", "constraints.json"),
                Path.of("hallgrid", "data", "constraints.json")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Cannot find data/constraints.json");
    }

    static Path projectRoot(Path dataFile) {
        Path dataDir = dataFile.toAbsolutePath().normalize().getParent();
        if (dataDir != null && "data".equals(dataDir.getFileName().toString()) && dataDir.getParent() != null) {
            return dataDir.getParent();
        }
        return dataDir != null ? dataDir : Path.of(".");
    }
}
