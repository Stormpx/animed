# animed

根据配置定期读取RSS源中更新的动画并调用第三方下载器进行下载，支持监听多个字幕组的相同剧集的资源。

### Example
```yaml
# 剧集进度保存在哪
data_path: /data

#监听动画配置 支持修改文件后及时更新
anime:
    #用于标记的动画id
  - id: witch-from-mercury
    #支持[dmhy.org,nyaa.si,bangumi.moe]的RSS源
    rss: https://dmhy.org/topics/rss/rss.xml?keyword=%E6%B0%B4%E6%98%9F%E7%9A%84%E9%AD%94%E5%A5%B3
    #是否在刷新配置后立即进行读取匹配
    immediately: false
    #指示匹配时至少要大于该配置才进行下载
    start_episode: 12
    #最后一集(到达该集数后不再进行读取)
    final_episode: 24
    #读取间隔(秒)
    refresh_interval: 18500
    #标题匹配 会尝试将配置的标题拆分成[字幕组,剧名称,集数,字幕]在后续的匹配中将使用4个属性进行匹配
    titles:
      - '[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第23话 Webrip 1080p MKV 简繁外挂'
    #配置时将标题集数位置标记为 #ep#. 匹配时将根据#ep#的位置提取集数来判断是否已下载
    patterns:
      - '[ANi] 機動戰士鋼彈 水星的魔女 Season 2 - #ep# [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]#mono#'
      - '[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第#ep#话 Webrip 1080p MKV 简繁外挂'
      - '[喵萌奶茶屋&LoliHouse] 机动战士高达 水星的魔女 / Mobile Suit Gundam THE WITCH FROM MERCURY - #ep# [WebRip 1080p HEVC-10bit AAC][简繁日内封字幕]'
    #下载器id
    downloader: aria2
    #传递到下载器的下载地址
    download_path: /downloads/VIDEO/anima/水星的魔女/Season 2

#下载器配置
downloader:
    #下载器id
  - id: aria2
    #只支持aria2 POST
    type: aria2
    uri: http://127.0.0.1:6800/jsonrpc
    token: foobar

#使用代理读取RSS源 只支持HTTP
proxies:
  - http://192.168.31.24:19999
```