# GhostTrack 1.0.0

首个正式版：基于 Material You 的 OSINT 小工具（HunxByts/GhostTrack 的非官方安卓移植）。

## 功能

- **IP 追踪**：查询任意 IP 的归属地、运营商、ASN、时区（ipwho.is）
- **IP 质量检测**：机房 / 代理 / 移动网络标记、原生 IP 与纯净度估算（ip-api.com）
- **本机 IP**：一键查看当前网络出口 IP 并解析归属（ipify）
- **号码解析**：手机号归属地、运营商、有效性与格式，离线解析（Google libphonenumber）
- **用户名搜索**：在 15 个站点并发检查用户名是否存在（逐站实测判定策略）
- **关于页**：中英双语切换、开源库鸣谢、检查更新 / 仓库 / 原项目跳转

## 说明

- 最低支持 Android 8.0（API 26），Android 12+ 支持 Material You 动态取色
- 本机 IP 与 IP 质量检测需联网；号码解析为纯离线
- 纯净度 / 原生 IP 为基于公开标记的估算，仅供参考

## 文件

- `GhostTrack_1.0.0.apk`：直接安装到手机
- `GhostTrack_1.0.0.aab`：应用包（备用）
