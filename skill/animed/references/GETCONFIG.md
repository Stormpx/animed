
使用`get-animed-config`工具获取配置

### 配置说明

- 配置格式：YAML
- 全局存储路径：`data_path: /path`（必须存在目录）
- 功能模块：`监听动画配置(anime)` + `下载器配置(downloader)`

### 动画监听配置规范
**参数层级**: `anime:`
- `id`: 唯一动画标识符（如：`witch-from-mercury`）
- `rss`: RSS源地址（支持域名包括 dmhy.org/nyaa.si/bangumi.moe）
- `immediately`: 是否立即解析（布尔值，默认false）
- `start_episode`: 起始检测集数（>此值时触发下载）
- `final_episode`: 终止检测集数（<=此值时停止检测）
- `refresh_interval`: 检测间隔秒数（建议≥1800秒）
- `titles:`
    - 已成功匹配的标题参考列表
    - 格式示例：`【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][01][1080p][简体][招募翻译校对]`
- `patterns:`
    - 使用占位符`#ep#`标记集数位置
    - 示例：`【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][#ep#][1080p][简体][招募翻译校对]`
- `downloader`: 指定下载器ID（需要与download配置一致）
- `download_path`: 下载路径（支持相对路径）

### 下载器配置规范
**参数层级**: `downloader:`
- `id`: 下载器唯一ID（如：`aria2`）
- `type`: 只支持`aria2`
- `uri`: aria2的JSON-RPC地址
- `token`: RPC密钥（可选）
- `downloadPath`: 下载器的基础路径(当动画监听的下载路径指定了相对路径时，就是基于该路径来进行'相对')

### 配置文件验证规则
-  每个anime配置必须有id/rss/(titles/rules二选一)/refresh_interval/downloader/download_path
-  downloader配置必须包含type/uri基础参数
-  正则表达式元字符需要转义（如`[]`需写成`\[\]`）

### 典型配置示例
```yaml
anime:
  - id: Lycoris-Recoil
    rss: https://dmhy.org/topics/rss/rss.xml?keyword=Lycoris+Recoil
    immediately: true
    start_chapter: 3
    refresh_interval: 1000
    titles:
      - 【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][01][1080p][简体][招募翻译校对]
    rules:
      - 【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][#ep#][1080p][简体][招募翻译校对]
    downloader: aria2
    download_path: /downloads/VIDEO/anima/Lycoris-Recoilssss/Season 1
```
工具返回的配置直接原样展示即可，不能擅自整理、省略、归纳、总结、分类。