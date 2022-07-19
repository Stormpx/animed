package org.stormpx.rss;

public class Enclosure {
    private String url;
    private Long length;
    private String type;

    public Enclosure(String url, Long length, String type) {
        this.url = url;
        this.length = length;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public Long getLength() {
        return length;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Enclosure{" + "url='" + url + '\'' + ", length=" + length + ", type='" + type + '\'' + '}';
    }
}
