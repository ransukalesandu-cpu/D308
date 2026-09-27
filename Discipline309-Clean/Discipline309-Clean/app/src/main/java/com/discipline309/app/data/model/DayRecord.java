package com.discipline309.app.data.model;

public class DayRecord {
    public static final int STATUS_UPCOMING = 0;
    public static final int STATUS_PARTIAL = 1;
    public static final int STATUS_COMPLETE = 2;
    public static final int STATUS_MISSED = 3;

    private final int dayNumber;
    private int status;
    private int xp;
    private long updatedAt;

    public DayRecord(int dayNumber) {
        this.dayNumber = dayNumber;
        this.status = STATUS_UPCOMING;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getDayNumber() { return dayNumber; }
    public int getStatus() { return status; }
    public int getXp() { return xp; }
    public long getUpdatedAt() { return updatedAt; }

    public void setStatus(int status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
        this.updatedAt = System.currentTimeMillis();
    }
}
