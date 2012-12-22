package com.github.axet.shoutcast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.axet.wget.Direct;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.ex.DownloadError;

public class SHOUTstations {

    ArrayList<SHOUTstation> list = new ArrayList<SHOUTstation>();

    public SHOUTstations() {

    }

    public void extract(SHOUTgenre g) {
        extract(g, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void extract(SHOUTgenre g, AtomicBoolean stop, final Runnable notify) {
        list.clear();

        URL url = g.getURL();

        String html = WGet.getHtml(url, new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
                notify.run();
            }

            @Override
            public void notifyMoved() {
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                notify.run();
            }
        }, stop);

        int max = extract(g, html, url);
        while (list.size() <= max) {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url.toString());
            try {
                html = "";
                post.setHeader("Referer", url.toString());
                post.setHeader("Origin", "http://www.shoutcast.com");
                post.setHeader("User-Agent", Direct.USER_AGENT);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("strIndex", Integer.toString(list.size())));
                nameValuePairs.add(new BasicNameValuePair("count", "10"));
                nameValuePairs.add(new BasicNameValuePair("ajax", "true"));
                nameValuePairs.add(new BasicNameValuePair("mode", "listeners"));
                nameValuePairs.add(new BasicNameValuePair("order", "desc"));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = client.execute(post);
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    html += line;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            max = extract(g, html, url);
        }

    }

    int extract(SHOUTgenre g, String html, URL url) {
        try {
            Document doc = Jsoup.parse(html, url.toString());
            Elements divRadioPickers = doc.select("div[class=dirlist]");
            if (divRadioPickers.size() == 0)
                return 0;

            for (int i = 0; i < divRadioPickers.size(); i++) {
                Element li = divRadioPickers.get(i);
                Elements as = li.select("a[class*=clickabletitle]");
                Element a = as.get(0);

                String href = a.attr("abs:href");
                String name = a.attr("title");
                String id = a.attr("id");

                Elements dll = li.select("div[class=dirlistners]");
                Element dl = dll.get(0);
                String listeners = dl.text();

                Elements dbb = li.select("div[class=dirbitrate]");
                Element db = dbb.get(0);
                String bitrate = db.text();

                Elements dff = li.select("div[class=dirtype]");
                Element df = dff.get(0);
                String format = df.text();

                if (href != null && !href.isEmpty())
                    list.add(new SHOUTstation(g, new URL(href), name, id, Long.decode(listeners), Long.decode(bitrate),
                            format));
                else
                    throw new DownloadError("bad href rss " + href);
            }

            Elements nn = doc.getElementsByClass("numfound");
            Element n = nn.get(0);
            String numfound = n.attr("value");

            return Integer.decode(numfound);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SHOUTstation> getStations() {
        return list;
    }

}
