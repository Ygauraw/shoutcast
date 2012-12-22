package com.github.axet.shoutcast;


public class Test {

    public Test() {
    }

    public static void main(String[] args) {
        SHOUTcast c = new SHOUTcast();
        c.extract();
        for (SHOUTgenre g : c.getGenres()) {
            System.out.println(g.getParent().getName() + " - " + g.getName());
        }
        SHOUTgenre g = c.getGenres().get(0);
        SHOUTstations s = new SHOUTstations();
        s.extract(g);
        for (SHOUTstation t : s.getStations()) {
            System.out.println(t.getName());
        }
    }

}
