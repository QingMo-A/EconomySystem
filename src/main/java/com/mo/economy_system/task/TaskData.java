package com.mo.economy_system.task;

public class TaskData {
    private static int taskid;
    private static String taskName;
    private static String taskContent;
    private static boolean taskState;

    public TaskData(int taskid, String taskName, String taskContent, boolean taskState) {
        this.taskid = taskid;
        this.taskName = taskName;
        this.taskContent = taskContent;
        this.taskState = taskState;
    }
    public TaskData() {

    }

    public int getTaskid() {
        return taskid;
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

    public void setTaskid(int taskid) {
        this.taskid = taskid;
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

}
