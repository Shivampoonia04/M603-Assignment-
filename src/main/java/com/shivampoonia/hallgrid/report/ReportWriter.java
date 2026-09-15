package com.shivampoonia.hallgrid.report;

import com.shivampoonia.hallgrid.model.CampusModel;
import com.shivampoonia.hallgrid.model.Course;
import com.shivampoonia.hallgrid.model.Placement;
import com.shivampoonia.hallgrid.model.Schedule;
import com.shivampoonia.hallgrid.model.Timeslot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReportWriter {

    private ReportWriter() {
    }

    public static List<String> lines(CampusModel model, Schedule schedule) {
        List<String> out = new ArrayList<>();
        for (Placement p : schedule.ordered()) {
            Course c = model.course(p.classId());
            String title = c == null ? p.classId() : p.classId();
            if (p.isScheduled()) {
                String when = clock(model.timeslotLabel(p.timeslotId()));
                out.add("Scheduled " + title + " " + when + " " + p.roomId() + " " + p.remark());
            } else {
                out.add("Unscheduled " + title + " N/A N/A");
            }
        }
        return out;
    }

    public static String summary(Schedule schedule) {
        return schedule.stageName()
                + " | scheduled " + schedule.scheduledCount()
                + "/" + schedule.all().size()
                + " | waste " + schedule.totalWaste()
                + " seats | leftover "
                + String.join(", ", schedule.unscheduledIds().isEmpty()
                ? List.of("-") : schedule.unscheduledIds());
    }

    public static void writeText(Path file, CampusModel model, List<Schedule> stages, String extra) throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("HallGrid conflict report\n");
        sb.append(model.campus()).append('\n');
        sb.append("Largest student-group clique: ").append(model.cliqueHint())
                .append(" vs ").append(model.timeslots().size()).append(" timeslots\n\n");
        for (Schedule stage : stages) {
            sb.append("=== ").append(stage.stageName()).append(" ===\n");
            sb.append(summary(stage)).append('\n');
            for (String line : lines(model, stage)) {
                sb.append(line).append('\n');
            }
            sb.append('\n');
        }
        if (extra != null && !extra.isBlank()) {
            sb.append(extra);
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    public static void writeHtml(Path file, CampusModel model, List<Schedule> stages) throws IOException {
        Files.createDirectories(file.getParent());
        Schedule last = stages.get(stages.size() - 1);
        String[] days = {"Monday", "Tuesday", "Wednesday"};
        String[] hours = {"09:00", "11:00", "14:00", "16:00"};

        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8"/>
                <title>HallGrid - Media Campus block week</title>
                <style>
                  body { font-family: 'Segoe UI', Calibri, sans-serif; margin: 24px; background: #f4f1ea; color: #1d2430; }
                  h1 { margin: 0; font-size: 28px; }
                  .sub { color: #5b4636; margin: 4px 0 18px; }
                  table { border-collapse: collapse; width: 100%; background: #fff; margin: 0 0 22px; }
                  th, td { border: 1px solid #c9bda8; padding: 8px; font-size: 13px; vertical-align: top; }
                  th { background: #2f4f4f; color: #fff; text-align: left; }
                  .ok { color: #1b6b3a; font-weight: 600; }
                  .bad { color: #9b1c1c; font-weight: 600; }
                  .chip { display: inline-block; background: #e7d7b1; padding: 2px 8px; border-radius: 10px; margin-right: 6px; }
                  .cls { background: #f7f3ea; border: 1px solid #ddd0b8; border-radius: 6px; padding: 6px 8px; margin: 0 0 6px; }
                  .cls:last-child { margin-bottom: 0; }
                  .room { color: #5b4636; }
                </style>
                </head>
                <body>
                """);
        sb.append("<h1>HallGrid</h1>\n");
        sb.append("<div class='sub'>").append(esc(model.campus())).append(" · Shivam GH1061529</div>\n");
        sb.append("<p>");
        sb.append("<span class='chip'>").append(model.courses().size()).append(" classes</span>");
        sb.append("<span class='chip'>").append(model.rooms().size()).append(" rooms</span>");
        sb.append("<span class='chip'>").append(model.timeslots().size()).append(" slots</span>");
        sb.append("<span class='chip'>AI-Y1 clique ").append(model.cliqueHint()).append("</span>");
        sb.append("</p>\n");

        sb.append("<h2>Stage comparison</h2>\n");
        sb.append("<table>\n<tr><th>Stage</th><th>Scheduled</th><th>Unscheduled</th><th>Wasted seats</th></tr>\n");
        for (Schedule s : stages) {
            sb.append("<tr><td>").append(esc(s.stageName())).append("</td><td>")
                    .append(s.scheduledCount()).append("</td><td>")
                    .append(s.unscheduled().size()).append("</td><td>")
                    .append(s.totalWaste()).append("</td></tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Best-effort timetable</h2>\n");
        sb.append("<table>\n<tr><th>Time</th>");
        for (String day : days) {
            sb.append("<th>").append(day).append("</th>");
        }
        sb.append("</tr>\n");
        for (String hour : hours) {
            sb.append("<tr><th>").append(hour).append("</th>");
            for (String day : days) {
                sb.append("<td>");
                Timeslot slot = slotAt(model, day, hour);
                boolean any = false;
                if (slot != null) {
                    for (Placement p : last.scheduled()) {
                        if (!slot.id().equals(p.timeslotId())) {
                            continue;
                        }
                        any = true;
                        Course c = model.course(p.classId());
                        sb.append("<div class='cls'><strong>").append(esc(p.classId()))
                                .append("</strong> - ").append(esc(c.title()))
                                .append("<div class='room'>").append(esc(p.roomId()))
                                .append(" · ").append(c.enrolled()).append(" students</div>")
                                .append("<div class='ok'>").append(esc(p.remark())).append("</div></div>");
                    }
                }
                if (!any) {
                    sb.append("<span class='bad'>idle</span>");
                }
                sb.append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        sb.append("<h2>Manual fix log</h2>\n<ul>\n");
        if (last.unscheduled().isEmpty()) {
            sb.append("<li>No leftovers. Registrar can publish the grid.</li>\n");
        } else {
            for (Placement p : last.unscheduled()) {
                Course c = model.course(p.classId());
                sb.append("<li class='bad'>").append(p.classId()).append(" (").append(esc(c.title()))
                        .append(", ").append(c.enrolled()).append(" students, groups ")
                        .append(esc(String.join("/", model.groupsOf(c.id()))))
                        .append(") - no legal slot left in the 3-day block. Split the cohort or add a Thursday overflow slot.</li>\n");
            }
        }
        sb.append("</ul>\n</body>\n</html>\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static Timeslot slotAt(CampusModel model, String day, String hour) {
        String wanted = day + " " + hour;
        for (Timeslot t : model.timeslots()) {
            if (wanted.equals(t.label())) {
                return t;
            }
        }
        return null;
    }

    private static String clock(String label) {
        if (label.startsWith("Monday")) {
            return "Mon " + tail(label);
        }
        if (label.startsWith("Tuesday")) {
            return "Tue " + tail(label);
        }
        if (label.startsWith("Wednesday")) {
            return "Wed " + tail(label);
        }
        return label;
    }

    private static String tail(String label) {
        int sp = label.lastIndexOf(' ');
        return sp >= 0 ? label.substring(sp + 1) : label;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
