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
import com.github.axet.wget.RetryWrap;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.ex.DownloadError;

public class SHOUTcast {

    public enum States {

        // downoading main genres (main page)
        EXTRACTING_MAIN,

        // downlading sub genre (main + click)
        EXTRACTING_SUB,

        // IO error, retyring
        RETRY,

        // all done
        DONE
    }

    private States state = States.DONE;
    private int delay;
    private Throwable e;

    ArrayList<SHOUTgenre> list = new ArrayList<SHOUTgenre>();

    public SHOUTcast() {
    }

    public void extract() {
        extract(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void extract(AtomicBoolean stop, final Runnable notify) {
        list.clear();

        List<SHOUTparent> list = extractGenres(stop, notify);

        for (SHOUTparent g : list) {
            List<SHOUTgenre> sub = extractGenres(g, stop, notify);
            this.list.addAll(sub);
        }

        setState(States.DONE);
    }

    void setRetry(int delay, Throwable e) {
        this.setState(States.RETRY);
        this.setDelay(delay);
        this.setE(e);
    }

    List<SHOUTparent> extractGenres(final AtomicBoolean stop, final Runnable notify) {
        try {
            URL url = new URL("http://www.shoutcast.com/");

            String html = WGet.getHtml(url, new WGet.HtmlLoader() {
                @Override
                public void notifyRetry(int delay, Throwable e) {
                    setRetry(delay, e);
                    notify.run();
                }

                @Override
                public void notifyMoved() {
                    setState(States.RETRY);
                    notify.run();
                }

                @Override
                public void notifyDownloading() {
                    setState(States.EXTRACTING_MAIN);
                    notify.run();
                }
            }, stop);

            Document doc = Jsoup.parse(html, url.toString());
            Elements divRadioPickers = doc.select("div[id=radiopicker]");
            if (divRadioPickers.size() == 0)
                return null;

            Element divRadioPicker = divRadioPickers.get(0);
            Elements ulRadioPickers = divRadioPicker.getElementsByTag("ul");
            Element ulRadioPicker = ulRadioPickers.get(0);
            Elements liRadioPickers = ulRadioPicker.getElementsByTag("li");

            ArrayList<SHOUTparent> list = new ArrayList<SHOUTparent>();

            for (int i = 0; i < liRadioPickers.size(); i++) {
                Element li = liRadioPickers.get(i);
                Elements as = li.getElementsByTag("a");
                Element a = as.get(0);
                String href = a.attr("abs:href");
                String name = a.text();
                String id = li.attr("id");
                if (href != null && !href.isEmpty())
                    list.add(new SHOUTparent(Long.decode(id), new URL(href), name));
                else
                    throw new DownloadError("bad href rss " + href);
            }

            return list;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    List<SHOUTgenre> extractGenres(final SHOUTparent g, final AtomicBoolean stop, final Runnable notify) {
        try {
            String html = RetryWrap.wrap(stop, new RetryWrap.WrapReturn<String>() {
                URL u = g.getURL();

                @Override
                public void retry(int delay, Throwable e) {
                    setRetry(delay, e);
                    notify.run();
                }

                @Override
                public void moved(URL url) {
                    setState(States.RETRY);
                    u = url;
                    notify.run();
                }

                @Override
                public String download() throws IOException {
                    setState(States.EXTRACTING_SUB);
                    notify.run();

                    HttpPost post = new HttpPost("http://www.shoutcast.com/genre.jsp");

                    String html = "";

                    HttpClient client = new DefaultHttpClient();
                    post.setHeader("Referer", u.toString());
                    post.setHeader("User-Agent", Direct.USER_AGENT);
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("genre", g.getName()));
                    nameValuePairs.add(new BasicNameValuePair("id", g.getId().toString()));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = client.execute(post);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        html += line;
                    }
                    return html;
                }
            });

            Document doc = Jsoup.parse(html, g.getURL().toString());
            Elements liRadioPickers = doc.select("li[class=secgen]");

            ArrayList<SHOUTgenre> list = new ArrayList<SHOUTgenre>();

            for (int i = 0; i < liRadioPickers.size(); i++) {
                Element li = liRadioPickers.get(i);
                Elements as = li.getElementsByTag("a");
                Element a = as.get(0);
                String href = a.attr("abs:href");
                String name = a.text();
                if (href != null && !href.isEmpty())
                    list.add(new SHOUTgenre(g, new URL(href), name));
                else
                    throw new DownloadError("bad href rss " + href);
            }

            return list;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SHOUTgenre> getGenres() {
        return list;
    }

    synchronized public States getState() {
        return state;
    }

    synchronized public void setState(States state) {
        this.state = state;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay) {
        this.delay = delay;
    }

    synchronized public Throwable getE() {
        return e;
    }

    synchronized public void setE(Throwable e) {
        this.e = e;
    }

}
