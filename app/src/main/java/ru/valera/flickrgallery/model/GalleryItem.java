package ru.valera.flickrgallery.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Android on 22.08.2016.
 */
public class GalleryItem {

    @SerializedName("title")
    private String caption;
    @SerializedName("id")
    private String id;
    @SerializedName("url_s")
    private String url;

    public String getCaption() {
        return caption;
    }
    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return caption;
    }
}
