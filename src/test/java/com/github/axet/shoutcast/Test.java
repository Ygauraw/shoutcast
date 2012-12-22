package com.github.axet.shoutcast;

import java.net.MalformedURLException;
import java.net.URL;

public class Test {

    public Test() {
    }

    public static void main(String[] args) {
        // SHOUTcast c = new SHOUTcast();
        // c.extract();
        // for (SHOUTgenre g : c.getGenres()) {
        // System.out.println(g.getParent().getName() + " - " + g.getName());
        // }
        // SHOUTgenre g = c.getGenres().get(0);
        SHOUTstations s = new SHOUTstations();
        try {
            s.extract(new SHOUTgenre(null, new URL("http://www.shoutcast.com/radio/Adult%20Alternative"), null));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        for (SHOUTstation t : s.getStations()) {
            System.out.println(t.getName());
        }
    }

}
