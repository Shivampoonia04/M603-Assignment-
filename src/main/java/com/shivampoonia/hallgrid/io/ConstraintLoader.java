package com.shivampoonia.hallgrid.io;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shivampoonia.hallgrid.model.CampusModel;
import com.shivampoonia.hallgrid.model.Course;
import com.shivampoonia.hallgrid.model.Professor;
import com.shivampoonia.hallgrid.model.Room;
import com.shivampoonia.hallgrid.model.Timeslot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ConstraintLoader {

    private ConstraintLoader() {
    }

    public static CampusModel load(Path file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Raw raw = mapper.readValue(Files.readAllBytes(file), Raw.class);
        if (raw.classes == null || raw.rooms == null || raw.timeslots == null) {
            throw new IllegalArgumentException("constraints.json is missing classes, rooms or timeslots");
        }
        return new CampusModel(
                raw.campus == null ? "Campus" : raw.campus,
                raw.timeslots,
                raw.rooms,
                raw.classes,
                raw.professors,
                raw.studentGroups
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Raw {
        public String campus;
        public List<Timeslot> timeslots;
        public List<Room> rooms;
        public List<Course> classes;
        public List<Professor> professors;
        public Map<String, List<String>> studentGroups;
    }
}
