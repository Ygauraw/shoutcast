package com.github.axet.shoutcast;

import java.net.URL;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("SHOUTgenre")
public class SHOUTgenre {

    URL url;
    String name;
    SHOUTparent parent;

    public SHOUTgenre(SHOUTparent sup, URL url, String name) {
        this.parent = sup;
        this.url = url;
        this.name = name;
    }

    public SHOUTparent getParent() {
        return parent;
    }

    public URL getURL() {
        return url;
    }

    public String getName() {
        return name;
    }
}
