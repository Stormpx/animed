package org.stormpx.rss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RSSReader {
    private final static Logger logger= LoggerFactory.getLogger(RSSReader.class);
    private final static XMLInputFactory FACTORY=XMLInputFactory.newInstance();
    static {
        FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

    }
    private XMLStreamReader xmlReader;

    //0: rss 1: channel 2: item
    private int state;
    private boolean rss;
    private Channel channel;
    private List<Item> items =new ArrayList<>();
    private Item item;

    public RSSReader(InputStream inputStream) throws XMLStreamException {
        Objects.requireNonNull(inputStream);
        this.xmlReader=FACTORY.createXMLStreamReader(inputStream);
    }


    private void skipUntil(String name) throws XMLStreamException {
        while (xmlReader.hasNext()){
            int next = xmlReader.next();
            if (next==XMLStreamConstants.END_ELEMENT&&Objects.equals(xmlReader.getLocalName(),name)){
                break;
            }
        }

    }

    private LocalDateTime parseDate(String text){
        if (text==null||text.isBlank())
            return null;
        return LocalDateTime.parse(text,DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    private void readChannelElement() throws XMLStreamException {
        Objects.requireNonNull(channel);
        String localName = xmlReader.getLocalName();
        if (localName==null||localName.isBlank())
            return;

        switch (localName){
            case "title"-> channel.setTitle(xmlReader.getElementText());
            case "link"-> channel.setLink(xmlReader.getElementText());
            case "description" -> channel.setDescription(xmlReader.getElementText());
            case "pubDate"-> channel.setPubDate(parseDate(xmlReader.getElementText()));
            default -> skipUntil(localName);
        }
    }

    private Enclosure readEnclosure(){
        String url = xmlReader.getAttributeValue("", "url");
        String _length = xmlReader.getAttributeValue("", "length");
        String type = xmlReader.getAttributeValue("", "type");
        long length=0L;

        try {
            length= Long.parseLong(_length);
        } catch (NumberFormatException e) {
            logger.warn("parse enclosure attr: length failed");
        }

        return new Enclosure(url, length,type);
    }
    private void readItemElement() throws XMLStreamException {
        Objects.requireNonNull(item);
        String localName = xmlReader.getLocalName();
        if (localName==null||localName.isBlank())
            return;
        switch (localName){
            case "title"-> item.setTitle(xmlReader.getElementText());
            case "link"-> item.setLink(xmlReader.getElementText());
            case "description" -> item.setDescription(xmlReader.getElementText());
            case "pubDate"-> item.setPubDate(parseDate(xmlReader.getElementText()));
            case "author" -> item.setAuthor(xmlReader.getElementText());
            case "category" ->item.setCategory(xmlReader.getElementText());
            case "enclosure" -> {
                item.setEnclosure(readEnclosure());
                skipUntil(localName);
            }
            default -> skipUntil(localName);
        }

    }

    public Channel read() throws XMLStreamException {
        while (xmlReader.hasNext()){
            xmlReader.next();
            if (xmlReader.isCharacters()&&xmlReader.isWhiteSpace()){
                continue;
            }
            if (state==0){
                if (xmlReader.isStartElement()){
                    if (!rss){
                        String localName = xmlReader.getLocalName();
                        if (!Objects.equals(localName, "rss")) {
                            throw new RuntimeException("");
                        }
                        if (!Objects.equals(xmlReader.getAttributeValue(null, "version"), "2.0")) {
                            throw new RuntimeException("");
                        }
                        this.rss = true;
                        continue;
                    }
                    xmlReader.require(XMLStreamConstants.START_ELEMENT,null,"channel");
                    this.channel=new Channel();
                    state=1;
                    continue;
                }

            }
            if (state==1){
                if (xmlReader.isStartElement()){
                    String localName = xmlReader.getLocalName();
                    if (Objects.equals(localName,"item")){
                        this.item =new Item();
                        state=2;
                        continue;
                    }
                    readChannelElement();
                    continue;
                }
                if (xmlReader.isEndElement()){
                    state=0;
                    continue;
                }
            }
            if (state==2){
                if (xmlReader.isEndElement()){
                    //back to channel
                    items.add(item);
                    this.item=null;
                    state=1;
                    continue;
                }
                if (xmlReader.isStartElement()) {
                    readItemElement();
                }
            }
        }
        this.channel.setItems(items);
        return channel;
    }
}
