# GhostTrack for Android

基于 Material You（Material 3 动态取色）设计的安卓版 GhostTrack。
灵感与功能来自 [HunxByts/GhostTrack](https://github.com/HunxByts/GhostTrack)，
为非官方移植，全部代码重写（Kotlin + Jetpack Compose）。

## 功能

底部导航四个入口，对应原版四个菜单：

| 功能 | 说明 | 数据源 |
|---|---|---|
| IP 追踪 | 查询任意 IP 的归属地、运营商、ASN、时区 | [ipwho.is](https://ipwho.is)（免费，无需 key） |
| 本机 IP | 一键查看当前出口 IP 并解析归属 | [ipify](https://www.ipify.org) + ipwho.is |
| 号码解析 | 号码有效性、类型、运营商、归属地、各种格式 | Google libphonenumber（纯离线） |
| 用户名搜索 | 在 15 个站点并发搜索用户名是否存在 | 各站点公开个人主页 |

用户名搜索的判定策略是逐站点实测得出的（很多站点对不存在的用户名不返回 404，
而是返回 200 或跳转到登录页），详见 [docs/CHECK_STRATEGIES.md](docs/CHECK_STRATEGIES.md)。
结果分三种：存在 / 不存在 / 未知（被站点反爬拦截时显示"未知"，可点进链接手动确认）。

## 构建

GitHub Actions 自动跑单元测试并打出 debug APK（见 `.github/workflows/android.yml`，
Artifacts 里下载 `app-debug`）。

本地构建需要 Android SDK：

```bash
./gradlew :app:assembleDebug
```

## 技术栈

- Kotlin + Jetpack Compose（Material 3，Android 12+ 动态取色）
- OkHttp（网络）、kotlinx.serialization（JSON）、libphonenumber（号码解析）
- 单 `:app` 模块；ViewModel 自带协程作用域，无额外架构依赖

## 注意事项

- ipwho.is 免费额度为每 IP 每月 1 万次请求，超限会返回错误。
- 用户名搜索直接请求各站点页面，部分站点有反爬，结果仅供参考。
- 本工具用于 OSINT 学习与合法的信息收集，请勿用于骚扰、盗号等非法用途。
