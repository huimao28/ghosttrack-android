# 用户名站点判定策略

原版 GhostTrack 用"HTTP 200 = 存在"一刀切，但实测发现很多站点对不存在的
用户名并不返回 404。下面是 2026-10-12 逐站点实测后确定的判定策略
（测试用户名均为随机不存在的 `nosuchuserxyz12345abc`）。

判定结果分三种：`FOUND`（存在）、`NOT_FOUND`（不存在）、`UNKNOWN`（无法判断）。

| 站点 | 请求 URL | 实测不存在时 | 实测存在时 | 采用策略 |
|---|---|---|---|---|
| GitHub | `https://api.github.com/users/{u}` | 404 | 200 | 状态码 |
| X | `https://x.com/{u}` | 404 | 200 | 状态码 |
| Instagram | `https://www.instagram.com/{u}/` | 302 → `accounts/login/` | 200 | 跳转到登录页=不存在 |
| TikTok | `https://www.tiktok.com/@{u}` | 200，正文含 `"statusCode":10221` | 200，无该标记 | 正文标记 |
| YouTube | `https://www.youtube.com/@{u}` | 404 | 200 | 状态码 |
| Facebook | `https://www.facebook.com/{u}` | 302 → `m.facebook.com/login/` | 未登录时行为不稳定 | 跳转到登录页=不存在，其它=未知 |
| Snapchat | `https://www.snapchat.com/@{u}` | 404（经 308 跳转） | 200 | 状态码（跟随跳转） |
| Telegram | `https://t.me/{u}` | 200，`og:title` 为 `Telegram: Contact @{u}` | 200，`og:title` 为真实昵称 | 正文标记 |
| Pinterest | `https://www.pinterest.com/{u}/` | 200，正文含 `User not found` | 200，无该标记 | 正文标记 |
| Tumblr | `https://www.tumblr.com/{u}` | 404 | 200 | 状态码 |
| Dribbble | `https://www.dribbble.com/{u}` | 404 | 200 | 状态码 |
| SoundCloud | `https://soundcloud.com/{u}` | 404 | 200 | 状态码 |
| Quora | `https://www.quora.com/profile/{u}` | 404 | 200 | 状态码 |
| Behance | `https://www.behance.net/{u}` | 404 | 可能 403（反爬） | 404=不存在，其它非 200=未知 |
| LinkedIn | `https://www.linkedin.com/in/{u}` | 999（反爬） | 200 | 200=存在，404=不存在，其它=未知 |

## 主动剔除的站点

- **Twitch**：存在与不存在都返回 200，且页面是 JS 壳，HTML 里无稳定区分标记；桌面/移动 UA 行为不一致。
- **Reddit**：`www.reddit.com` 与 `old.reddit.com` 对爬虫均返回 200/403，无稳定区分。
- **Medium**：存在与不存在都返回 403（反爬）。
- **Vimeo**：已知存在的页面也返回 410/404，无稳定区分。
- **GitLab**：`gitlab.com/{u}` 存在与不存在都返回 200；`/api/v4/users?username=` 对匿名请求返回空数组。
- **Flickr**：`flickr.com/people/{u}` 需要数字 ID，用户名形式不可靠。

## 维护建议

这些 heuristic 会随站点改版失效。如果某站点开始大量误报，
优先用 `curl` 复测"存在 vs 不存在"的行为差异，再更新
`UsernameLookup.kt` 中的 `USERNAME_SITES` 表。
