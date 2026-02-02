# Xueqiu Reader - IntelliJ IDEA 插件

## 功能介绍
这是一个专为开发者设计的雪球网阅读插件，让您在编写代码的间隙，能够快速浏览股市动态。
- **热门内容**：查看雪球最热门的帖子。
- **关注动态**：实时同步您关注的用户动态。
- **7*24 快讯**：获取第一手市场资讯。
- **清晰展示**：显示发言人、正文，支持翻页查看。

![示意图](https://raw.githubusercontent.com/Folgerjun/materials/refs/heads/master/blog/img/xueqiu-plugin.png)

## 安装与使用
1. **下载插件**：获取 `xueqiu-reader.zip` 插件包。
2. **安装**：在 IDEA 中选择 `Settings` -> `Plugins` -> `Install Plugin from Disk...`。
3. **配置 Cookie**：
   - 打开侧边栏的 `Xueqiu` 工具窗口。
   - 点击 `设置Cookie` 按钮。
   - 在浏览器中登录雪球网，按 F12 打开开发者工具，在 `Network` 选项卡中找到任意一个请求，复制其 `Cookie` 字段内容并粘贴到插件中。
4. **浏览**：点击 `热门`、`关注` 或 `7*24` 即可开始阅读。
5. **翻页**：使用底部的翻页按钮查看更多内容。
6. **详情**：左键双击帖子可查看详情。

## 注意事项
- 由于雪球网的安全防护机制，插件目前主要通过 Cookie 进行认证。
- 建议定期更新 Cookie 以保持登录状态。
