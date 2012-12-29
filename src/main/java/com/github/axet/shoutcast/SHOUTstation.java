package com.github.axet.shoutcast;

import java.net.URL;

public class SHOUTstation {

    URL url;
    String name;
    SHOUTgenre parent;
    String id;
    Long listeners;
    Long bitrate;
    String format;

    public SHOUTstation(SHOUTgenre sup, URL url, String name, String id, Long listeners, Long bitrate, String format) {
        this.parent = sup;
        this.url = url;
        this.name = name;
        this.id = id;
        this.listeners = listeners;
        this.bitrate = bitrate;
        this.format = format;
    }

    public SHOUTgenre getParent() {
        return parent;
    }

    public URL getURL() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public Long getBitrate() {
        return bitrate;
    }

    public Long getListeners() {
        return listeners;
    }

    @Override
    public boolean equals(Object o) {
        return url.equals(((SHOUTstation) o).getURL());
    }
}
