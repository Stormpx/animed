package org.stormpx.rss;

import java.time.LocalDateTime;

public class Item {
    private String title;
    private String link;
    private String description;
    private String author;
    private String category;
    private String comments;
    private Enclosure enclosure;
    private String guid;
    private LocalDateTime pubDate;
    private String source;

    public String getTitle() {
        return title;
    }

    public Item setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Item setLink(String link) {
        this.link = link;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Item setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Item setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getComments() {
        return comments;
    }

    public Item setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public Enclosure getEnclosure() {
        return enclosure;
    }

    public Item setEnclosure(Enclosure enclosure) {
        this.enclosure = enclosure;
        return this;
    }

    public String getGuid() {
        return guid;
    }

    public Item setGuid(String guid) {
        this.guid = guid;
        return this;
    }

    public LocalDateTime getPubDate() {
        return pubDate;
    }

    public Item setPubDate(LocalDateTime pubDate) {
        this.pubDate = pubDate;
        return this;
    }

    public String getSource() {
        return source;
    }

    public Item setSource(String source) {
        this.source = source;
        return this;
    }

    @Override
    public String toString() {
        return "Item{" + "title='" + title + '\'' + ", link='" + link + '\'' + ", description='" + description + '\'' + ", author='" + author + '\'' + ", category='" + category + '\'' + ", comments='" + comments + '\'' + ", enclosure=" + enclosure + ", guid='" + guid + '\'' + ", pubDate=" + pubDate + ", source='" + source + '\'' + '}';
    }
}
