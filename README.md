# RikkaHub (RikkaHub)

> 一个运行在 Android 上的 AI 大模型聊天客户端，支持多家供应商切换、MCP 工具调用、
> 语音、Markdown 渲染等。本项目在 [RikkaHub](https://github.com/rikkahub/rikkahub) 基础上二次开发，
> 增加了逆向工作台、内置技能、悬浮球、消息保活等功能。

---

## ⚠️ 项目来源与开源声明（重要）

**本项目是基于 [RikkaHub](https://github.com/rikkahub/rikkahub) 二次开发的衍生作品。**

RikkaHub 使用 **AGPL-3.0** 许可证发布，因此本项目（RikkaHub）同样遵循 **AGPL-3.0** 开源，
完整源码在本仓库/网盘公开，任何人可自由查看、修改、再分发。

- 原项目：RikkaHub —— https://github.com/rikkahub/rikkahub
- 原项目许可证：[AGPL-3.0](LICENSE)

**特别感谢 RikkaHub 原作者**打下的完整 AI 聊天客户端基础。RikkaHub在其之上做了功能扩展与界面调整，
但核心框架、绝大部分聊天/供应商/MCP/TTS 代码源自 RikkaHub，版权与署名归原作者所有。

> 若原作者对本二开有任何异议，请联系我，我会积极配合处理。

---

## 二次开发新增/改动（相对 RikkaHub）

- **逆向工作台**：预置连接本机逆向 MCP 后端（MT 管理器 / SOMCP / ProxyPin），
  一键探测端口，配套逆向技能与 system prompt。
- **内置技能库**：打包了一批安卓逆向/安全分析技能文档。
- **高级功能**：AI 生成悬浮球、消息保活前台服务、自动压缩会话、持续工作等。
- **界面与品牌**：主题紫色主题、首页仪表盘、底部导航等 UI 调整。
- **自定义更新源**：更新检测指向本仓库 GitHub Releases（`JcEvoX/RikkaHuB`），不再监听官方
  `updates.rikka-ai.com`，避免拉到官方包（签名不同）导致覆盖安装失败。
- 品牌信息（应用名、图标、包名 applicationId）改为RikkaHub。

其余的核心能力（多供应商、模型管理、MCP 客户端、TTS/ASR、Markdown、备份同步等）
均来自 RikkaHub，遵循其原有实现与许可证。

---

## 构建

```bash
# 需要 JDK 21 + Android SDK
./gradlew assembleRelease
```

web-ui 前端模块需先安装依赖（pnpm，见 [web-ui/README](web-ui/README.md) 与 [AGENTS.md](AGENTS.md)）。

### 更新源

应用内更新检查读取本仓库 **GitHub Releases**（`https://api.github.com/repos/JcEvoX/RikkaHuB/releases/latest`）。
发布新版本时，在仓库 Releases 页面创建一个 Release：`Tag` 用 `v{版本号}`（如 `v2.4.13`），并在附件中上传
release APK（`app-universal-release.apk` 或 `app-arm64-v8a-release.apk` 等，以 `.apk` 结尾即会被识别）。
App 检测到该 Release 后即会提示更新并提供下载。

### 固定签名

为使每次 CI 打出的 release APK 用同一密钥签名（这样才能覆盖安装、无需卸载重装），请在仓库
**Settings → Secrets and variables → Actions** 配置固定密钥：

1. 生成本地 keystore（仅需一次，妥善保管）：

   ```bash
   keytool -genkeypair -v \
     -keystore release.jks \
     -alias rikkahub -keyalg RSA -keysize 2048 -validity 10000 \
     -storepass 你的口令 -keypass 你的口令 \
     -dname "CN=RikkaHub, OU=CI, O=rikkahub, L=CN, C=CN"
   ```

2. 用下面的命令生成 4 个 Secret 的值（口令换成你自己的）：

   ```bash
   base64 -w0 release.jks                        # RELEASE_KEYSTORE_BASE64
   echo -n 你的store口令                            # RELEASE_STORE_PASSWORD
   echo -n rikkahub                                # RELEASE_KEY_ALIAS
   echo -n 你的key口令                              # RELEASE_KEY_PASSWORD
   ```

3. 在仓库 Secrets 里分别创建上述 4 个项目，值粘贴对应输出。

配置好后，master 分支的 CI 会用该固定密钥签名 release 包；未配置时则回退到临时密钥（签名不固定，
只能新装、无法覆盖安装正式版）。

> 本地开发也可把 `storeFile/storePassword/keyAlias/keyPassword` 写进 `local.properties` 直接复用同一密钥，无需上传到 GitHub。

---

## 许可证

本项目依据 [GNU Affero General Public License v3.0](LICENSE) 发布（继承自 RikkaHub）。
你可以自由使用、修改、再分发本项目，但衍生作品必须同样以 AGPL-3.0 开源并保留署名。

第三方依赖、submodule 等仍分别遵循各自上游许可证，AGPL-3.0 声明不替代其原有条款。
