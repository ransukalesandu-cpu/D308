package com.discipline309.app.data.model;

public class Mission {
    public enum Type { DAILY, WEEKLY, SPECIAL }

    private final String id;
    private final String title;
    private final String description;
    private final int xp;
    private final Type type;
    private boolean completed;

    public Mission(String id, String title, String description, int xp, Type type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.xp = Math.max(0, xp);
        this.type = type;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getXp() { return xp; }
    public Type getType() { return type; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
