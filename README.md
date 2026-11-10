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
    #bangumi 条目 id。留空时会自动推断，推断不出来（候选有歧义）时手动填写即可
    bgm_id: 402390
    #下载器id
    downloader: aria2
    #传递到下载器的下载地址 可配置相对路径
    download_path: /downloads/VIDEO/anima/水星的魔女/Season 2

#下载器配置
downloader:
    #下载器id
  - id: aria2
    #只支持aria2 POST
    type: aria2
    uri: http://127.0.0.1:6800/jsonrpc
    token: foobar
    #下载器的下载目录 如果anime的下载路径时配置了相对路径则将根据该目录进行'相对'
    download_path: /downloads/anime

#mcp服务配置
mcp:
  enable: true
  host: 127.0.0.1
  port: 3001

#使用代理读取RSS源 只支持HTTP
proxies:
  - http://192.168.31.24:19999
```
### bangumi 集数（bgm_id / bgm_eps）

每个 worker 在 `{data_path}/{id}.json` 里维护 bangumi 条目信息，和下载进度存在一起：

```json
{ "id": "witch-from-mercury", "chapter": 13.0, "entrys": [], "bgm_id": 402390, "bgm_eps": 24 }
```

每次 `start()` 都会先对齐这部分信息（`updateBgmInfo`）：

1. **id** 的优先级：配置里的 `bgm_id` > 自己保存的 `bgm_id` > 根据 RSS 条目标题搜索推断
2. **eps** 一律通过 `GET /v0/subjects/{id}` 详情接口获取：搜索接口对未定档/分割放送的条目经常返回 0
3. 配置里的 `bgm_id` 改了就跟着改（并重新拉一次 eps），配置不填就沿用自己保存的，两边都空才去搜索

搜索推断的判据：

- **名字**：归一化后（繁转简、日文新字体转简体、片假名折叠、去标点）比对 `name`、`name_cn` 与全部别名，低于 0.6 直接淘汰
- **季/部分标记**：`第二季`、`第2部分`、`第IIクール`、`Season 2`、`S2` 等会被剥离出来单独比对，用来区分本体与续作
- **开播日期**：候选的开播日期必须与 RSS 条目的发布时间相容（2011 年开播的作品不可能有 2026 年的连载资源）
- **集号**：条目的集号不应超过该条目的集数

最高分同时满足"名字相似度 ≥ 0.85"且"领先第二名 ≥ 0.6"才采用，否则**不猜**：日志会打印候选排名并发通知，
此时请在配置里手动填写 `bgm_id`（或通过 MCP 工具的 `bgmId` 参数），手动指定后不再触发搜索。
热度（收藏/在看人数）刻意不参与打分：新番或冷门作品的热度不可靠。

### 完结判定

- `final_episode`：到达该集数后不再读取 RSS（和以前一致）
- `bgm_eps`：已下载集数**超过** bgm 总集数时（分割放送、字幕组连续编号、bgm 数据不准），
  RSS 改为最多 **24 小时**检查一次
- bgm 迟迟没给集数时（新番/未定档条目常见）最多 **24 小时**去问一次详情接口，不会每个刷新周期都打接口
- 想手动纠正：改配置里的 `bgm_id` 会被立刻采用并重新拉集数

