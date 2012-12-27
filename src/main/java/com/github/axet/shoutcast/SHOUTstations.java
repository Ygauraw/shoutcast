package com.github.axet.shoutcast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.axet.wget.Direct;
import com.github.axet.wget.RetryWrap;
import com.github.axet.wget.info.ex.DownloadError;

public class SHOUTstations {

    ArrayList<SHOUTstation> list = new ArrayList<SHOUTstation>();

    public enum States {
        RETRY, EXTRACT, DONE
    };

    private int delay;
    private int maxItems;
    private Throwable e;
    private States state;

    public SHOUTstations() {
        state = States.DONE;
    }

    void setRetry(int delay, Throwable e) {
        this.setDelay(delay);
        this.setE(e);
    }

    public void extract(SHOUTgenre g) {
        extract(g, new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public static URI toURI(URL url) {
        try {
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                    url.getQuery(), url.getRef());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void extract(final SHOUTgenre g, AtomicBoolean stop, final Runnable notify) {
        list.clear();

        URL url = g.getURL();

        final CookieStore store = new BasicCookieStore();
        final HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, store);

        final HttpClient client = new DefaultHttpClient();

        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Direct.CONNECT_TIMEOUT);
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, Direct.READ_TIMEOUT);

        String html = RetryWrap.wrap(stop, new RetryWrap.WrapReturn<String>() {
            URL u = g.getURL();

            @Override
            public void retry(int delay, Throwable e) {
                setRetry(delay, e);
                notify.run();
            }

            @Override
            public void moved(URL url) {
                u = url;

                setState(States.RETRY);
                notify.run();
            }

            @Override
            public String download() throws IOException {
                setState(States.EXTRACT);
                notify.run();

                String html = "";
                HttpGet get = new HttpGet(toURI(u));
                get.setHeader("Referer", u.toString());
                get.setHeader("User-Agent", Direct.USER_AGENT);

                HttpResponse response = client.execute(get, httpContext);
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    html += line;
                }

                return html;
            }
        });

        setMaxItems(extract(g, html, url));

        while (list.size() < getMaxItems()) {
            final URL uu;

            try {
                uu = new URL(String.format("http://www.shoutcast.com/genre-ajax/%s", g.getName()));
            } catch (MalformedURLException e1) {
                throw new RuntimeException(e1);
            }

            html = RetryWrap.wrap(stop, new RetryWrap.WrapReturn<String>() {
                URL u = uu;

                @Override
                public void retry(int delay, Throwable e) {
                    setRetry(delay, e);
                    notify.run();
                }

                @Override
                public void moved(URL url) {
                    u = url;

                    setState(States.RETRY);
                    notify.run();
                }

                @Override
                public String download() throws IOException {
                    setState(States.EXTRACT);
                    notify.run();

                    String html = "";
                    URI uuu = toURI(u);
                    HttpPost post = new HttpPost(uuu);
                    post.setHeader("Referer", uuu.toString());
                    post.setHeader("User-Agent", Direct.USER_AGENT);
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("strIndex", Integer.toString(list.size())));
                    nameValuePairs.add(new BasicNameValuePair("count", "10"));
                    nameValuePairs.add(new BasicNameValuePair("ajax", "true"));
                    nameValuePairs.add(new BasicNameValuePair("mode", "listeners"));
                    nameValuePairs.add(new BasicNameValuePair("order", "desc"));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = client.execute(post, httpContext);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        html += line;
                    }
                    return html;
                }
            });

            setMaxItems(extract(g, html, url));
        }

        setState(States.DONE);
        notify.run();
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

    synchronized public States getState() {
        return state;
    }

    synchronized public void setState(States state) {
        this.state = state;
    }

    synchronized public int getMaxItems() {
        return maxItems;
    }

    synchronized public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    synchronized public Throwable getE() {
        return e;
    }

    synchronized public void setE(Throwable e) {
        this.e = e;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay) {
        this.delay = delay;
    }
}
