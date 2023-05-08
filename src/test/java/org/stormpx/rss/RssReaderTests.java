package org.stormpx.rss;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class RssReaderTests {

    @Test
    public void test() throws XMLStreamException {
        var rss = """
                <?xml version="1.0" encoding="utf-8"?>
                <rss version="2.0"\s
                xmlns:content="http://purl.org/rss/1.0/modules/content/"\s
                xmlns:wfw="http://wellformedweb.org/CommentAPI/"\s
                >
                <channel>
                <item>
                <title><![CDATA[【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][11][1080p][简日双语][招募翻译]]]></title>
                <link>http://dmhy.anoneko.com/topics/view/624626_10_Isekai_Ojisan_11_720p.html</link>
                <pubDate>Sat, 17 Dec 2022 08:54:06 +0800</pubDate>
                <description>teststest</description>
                <enclosure url="magnet:?xt=urn:btih:YZCZR3SYAE4ASUERW3CZLWPDRODSXM4H&amp;dn=&amp;tr=http%3A%2F%2F104.143.10.186%3A8000%2Fannounce&amp;tr=udp%3A%2F%2F104.143.10.186%3A8000%2Fannounce&amp;tr=http%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&amp;tr=http%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&amp;tr=http%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&amp;tr=http%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&amp;tr=http%3A%2F%2Ftracker.prq.to%2Fannounce&amp;tr=http%3A%2F%2Fopen.acgtracker.com%3A1096%2Fannounce&amp;tr=https%3A%2F%2Ft-115.rhcloud.com%2Fonly_for_ylbud&amp;tr=http%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&amp;tr=http%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&amp;tr=udp%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&amp;tr=udp%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&amp;tr=udp%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&amp;tr=udp%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&amp;tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce&amp;tr=http%3A%2F%2Ft.nyaatracker.com%2Fannounce&amp;tr=http%3A%2F%2Ft.acg.rip%3A6699%2Fannounce&amp;tr=http%3A%2F%2Ftr.bangumi.moe%3A6969%2Fannounce&amp;tr=http%3A%2F%2Fshare.camoe.cn%3A8080%2Fannounce&amp;tr=http%3A%2F%2Fsukebei.tracker.wf%3A8888%2Fannounce&amp;tr=http%3A%2F%2Fopenbittorrent.com%3A80%2Fannounce&amp;tr=https%3A%2F%2Fopentracker.i2p.rocks%3A443%2Fannounce&amp;tr=https%3A%2F%2Ftracker.nanoha.org%3A443%2Fannounce&amp;tr=https%3A%2F%2Ftracker.cyber-hub.net%3A443%2Fannounce&amp;tr=https%3A%2F%2Ftr.burnabyhighstar.com%3A443%2Fannounce"  length="1"  type="application/x-bittorrent" ></enclosure>
                <author><![CDATA[nekomoekissaten]]></author>
                <guid isPermaLink="true" >http://dmhy.anoneko.com/topics/view/624626_10_Isekai_Ojisan_11_720p.html</guid>
                <category domain="http://dmhy.anoneko.com/topics/list/sort_id/2" ><![CDATA[動畫]]></category>
                </item>
                </channel>
                </rss>
                """;

        RSSReader rssReader = new RSSReader(new ByteArrayInputStream(rss.getBytes(StandardCharsets.UTF_8)));
        Channel channel = rssReader.read();
        System.out.println(channel);
    }

}
