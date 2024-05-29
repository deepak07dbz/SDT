package com.example.database;

public class MsgModel {

    private String msg;
    private int id;

    public MsgModel(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public MsgModel(String msg) {
        this.msg = msg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
