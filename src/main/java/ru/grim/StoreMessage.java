package ru.grim;

public class StoreMessage {
    private String url;
    private Integer time;

    public StoreMessage(String url, Integer time){
        this.time = time;
        this.url = url;
    }

    //getters


    public String getUrl() {
        return url;
    }

    public Integer getTime() {
        return time;
    }
}
