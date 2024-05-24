package com.example.helloworld.models;

public class VideoItem {
    private String id;
    private String title;
    private String description;
    private String publishedAt;
    private String thumbnail;

    private String videoSnippet;
    private int listSize = -1; // playlist

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getVideoSnippet() {
        return videoSnippet;
    }

    public void setVideoSnippet(String videoSnippet) {
        this.videoSnippet = videoSnippet;
    }

    public int getListSize() {
        return listSize;
    }

    public void setListSize(int listSize) {
        this.listSize = listSize;
    }
}
