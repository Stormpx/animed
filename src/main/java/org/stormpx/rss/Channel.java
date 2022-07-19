package org.stormpx.rss;

import java.time.LocalDateTime;
import java.util.List;

public class Channel {
    private String title;
    private String link;
    private String description;

    private String language;
    private String copyright;
    private String managingEditor;
    private String webMaster;
    private LocalDateTime pubDate;
    private LocalDateTime lastBuildDate;
    private String category;
    private String generator;
    private String docs;
    private String cloud;
    private Integer ttl;
    private String image;
    private String rating;
    private String textInput;
    private String skipHours;
    private String skipDays;

    private List<Item> items;

    public Channel() {
    }

    public Channel(String title, String link, String description) {
        this.title = title;
        this.link = link;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public Channel setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getLink() {
        return link;
    }

    public Channel setLink(String link) {
        this.link = link;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Channel setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public Channel setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getCopyright() {
        return copyright;
    }

    public Channel setCopyright(String copyright) {
        this.copyright = copyright;
        return this;
    }

    public String getManagingEditor() {
        return managingEditor;
    }

    public Channel setManagingEditor(String managingEditor) {
        this.managingEditor = managingEditor;
        return this;
    }

    public String getWebMaster() {
        return webMaster;
    }

    public Channel setWebMaster(String webMaster) {
        this.webMaster = webMaster;
        return this;
    }

    public LocalDateTime getPubDate() {
        return pubDate;
    }

    public Channel setPubDate(LocalDateTime pubDate) {
        this.pubDate = pubDate;
        return this;
    }

    public LocalDateTime getLastBuildDate() {
        return lastBuildDate;
    }

    public Channel setLastBuildDate(LocalDateTime lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Channel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getGenerator() {
        return generator;
    }

    public Channel setGenerator(String generator) {
        this.generator = generator;
        return this;
    }

    public String getDocs() {
        return docs;
    }

    public Channel setDocs(String docs) {
        this.docs = docs;
        return this;
    }

    public String getCloud() {
        return cloud;
    }

    public Channel setCloud(String cloud) {
        this.cloud = cloud;
        return this;
    }

    public Integer getTtl() {
        return ttl;
    }

    public Channel setTtl(Integer ttl) {
        this.ttl = ttl;
        return this;
    }

    public String getImage() {
        return image;
    }

    public Channel setImage(String image) {
        this.image = image;
        return this;
    }

    public String getRating() {
        return rating;
    }

    public Channel setRating(String rating) {
        this.rating = rating;
        return this;
    }

    public String getTextInput() {
        return textInput;
    }

    public Channel setTextInput(String textInput) {
        this.textInput = textInput;
        return this;
    }

    public String getSkipHours() {
        return skipHours;
    }

    public Channel setSkipHours(String skipHours) {
        this.skipHours = skipHours;
        return this;
    }

    public String getSkipDays() {
        return skipDays;
    }

    public Channel setSkipDays(String skipDays) {
        this.skipDays = skipDays;
        return this;
    }


    public List<Item> getItems() {
        return items;
    }

    public Channel setItems(List<Item> items) {
        this.items = items;
        return this;
    }

    @Override
    public String toString() {
        return "Channel{" + "title='" + title + '\'' + ", link='" + link + '\'' + ", description='" + description + '\'' + ", language='" + language + '\'' + ", copyright='" + copyright + '\'' + ", managingEditor='" + managingEditor + '\'' + ", webMaster='" + webMaster + '\'' + ", pubDate=" + pubDate + ", lastBuildDate=" + lastBuildDate + ", category='" + category + '\'' + ", generator='" + generator + '\'' + ", docs='" + docs + '\'' + ", cloud='" + cloud + '\'' + ", ttl=" + ttl + ", image='" + image + '\'' + ", rating='" + rating + '\'' + ", textInput='" + textInput + '\'' + ", skipHours='" + skipHours + '\'' + ", skipDays='" + skipDays + '\'' + ", items=" + items + '}';
    }
}
