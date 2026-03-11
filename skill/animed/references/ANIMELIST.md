基于用户指定的动漫资源网站和提供的关键词使用`animed-list`工具搜索可下载的番剧。

### 理解用户需求
- 当用户说类似'帮我去WWW搜索XXXX'的话时 将WWW(动漫资源网站)和XXXX(番剧名称)转换为本工具可接受的参数传入。
- 如: '帮我去动漫花园搜索攻壳机动队' 动漫花园将会被转换为dmhy作为参数source,攻壳机动队会被作为参数keyword。
- 必须在用户明确要求搜索时才去搜索。


### 调用工具

两个参数 `source` 和 `keyword`
- source: 一个枚举值 现在有 ['dmhy','mikan','bangumi','nyaasi']
    - dmhy 的别称有: '动漫花园','DMHY','冻鳗花园','dmhy.org'等
    - mikan 的别称有: '蜜柑计划','Mikan Project','mikanani.me'等
    - bangumi 的别称有： 'Bangumi Moe','bangumi.moe','BANGUMI','番组'等
    - nyaasi 的别称有: 'nyaa.si'等

- keyword: 完全由用户提供的番剧名称关键词
    - 不要添油加醋，用户说什么传什么

***返回结果***
```json
{
    "url": "xxxxx",
    "animes": [
        {
            "id": "id",
            "title": "[ANi]  小市民系列 第二季 - 14 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
            "pubTime": "2025-01-01 00:00:00"
        }
    ]
}
```

### 展示结果
为方便后续用户指定相应的剧集标题以使用其他工具，只需要将animes数组里面的内容简单整理包含id一行一行展示即可，不能省略、归纳、总结、分类。

比如可以这样展示结果:

```
搜索结果网址：https://bangumi.moe/rss/search/小市民
当前在 bangumi.moe 网站使用关键词“小市民”的搜索结果如下（共3条）：
| ID | 标题                                                                                | 发布时间                |
|:---|:----------------------------------------------------------------------------------|:--------------------|
| a  | [黒ネズミたち] 小市民系列 第二季 / Shoushimin Series 2nd Season - 14 (CR 1920x1080 AVC AAC MKV) | 2025-01-01 00:00:01 |
| b  | [ANi] 小市民系列 第二季 - 14 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]                     | 2025-01-01 00:00:01 |
| c  | [北宇治字幕组] 小市民系列 / Shoushimin Series [13][WebRip][HEVC_AAC][简日内嵌]                   | 2025-01-01 00:00:01 |
```
