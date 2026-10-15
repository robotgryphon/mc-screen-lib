package dev.robotgryphon.screenlib.graph.evaluation;

import java.util.List;
import java.util.Map;

public record NodeSnapshot(List<String> errors, Map<String, Object> values) {
    public boolean valid() {
        return this.errors.isEmpty();
    }
}
