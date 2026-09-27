package com.discipline309.app.data.model;

public class HabitRecord {
    private final String id;
    private String name;
    private boolean enabled;
    private int targetPerWeek;

    public HabitRecord(String id, String name) {
        this.id = id;
        this.name = name;
        this.enabled = true;
        this.targetPerWeek = 7;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public int getTargetPerWeek() { return targetPerWeek; }

    public void setName(String name) { this.name = name; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTargetPerWeek(int targetPerWeek) {
        this.targetPerWeek = Math.max(1, Math.min(7, targetPerWeek));
    }
}
