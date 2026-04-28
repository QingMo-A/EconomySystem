package com.mo.economy_system.core.task_system;

public abstract class TaskBaseData {
    private int taskId;
    private String taskName;
    private String taskContent;
    private boolean taskState;
    private boolean isCompleted; //任务是否完成
    private long startTime; // 任务开始时间
    private long endTime;

    public TaskBaseData(int taskId, String taskName, String taskContent, long startTime, long endTime) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskContent = taskContent;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isCompleted = false;
    }
    public TaskBaseData() {

    }

    public int getTaskId() {
        return taskId;
    }
    public String getTaskName() {
        return taskName;
    }
    public String getTaskContent() {
        return taskContent;
    }
    public boolean getTaskState() {
        return taskState;
    }
    public boolean getIsCompleted() {
        return isCompleted;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }
    public void setTaskState(boolean taskState) {
        this.taskState = taskState;
    }
    public void setIsCompleted(boolean taskState) {
        this.isCompleted = taskState;
    }

    public long getTaskStartTime () {
        return startTime;
    }
    public long getTaskEndTime() {
        return endTime;
    }

}
