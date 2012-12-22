package com.github.axet.shoutcast;

import java.net.URL;

public class SHOUTparent {

    URL url;
    String name;
    Long id;

    public SHOUTparent(Long id, URL url, String name) {
        this.id = id;
        this.url = url;
        this.name = name;
    }

    public URL getURL() {
        return url;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
