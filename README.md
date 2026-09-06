# 知微数学 (Zhiwei Math)

> 🚧 **正在开发中** — 本项目处于积极开发阶段，功能与接口均可能变化。

「知微数学」是一个帮助大学生学习高等数学的 Android App（BYOK：用户填自己的 API Key）。

打开应用先问"你要学什么"，选择科目（V1 仅高等数学）后进入与云端 LLM 的对话式学习：
LLM 扮演"B 站知名高数教师宋浩"的风格讲课，支持 **精讲 / 期末冲刺** 两种模式，
另有练题、例题本、划重点、学习报告等功能。设计参考苹果 iOS（毛玻璃 / 液态玻璃 + 丝滑动画）。

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 工程骨架（Gradle + 源码模块导入 + 全依赖编译验证 + MiSans + 主题 + 导航） | ✅ |
| 2 | LLM 核心（OpenAI/Anthropic 双协议客户端、SSE 流式、提示词分层缓存、打断、API 设置、Onboarding） | ✅ |
| 3 | 聊天主界面（流式 Markdown+LaTeX、消息操作按钮组、选中追问、模式切换、图片/文档输入、OCR 兜底链、对话列表） | ✅ |
| 4 | 玻璃效果系统（毛玻璃默认 / 液态玻璃 API 33+ 参数可调 / 低版本半透明降级、苹果风转场） | ✅ |
| 5 | 扩展功能（对话设置元提示词生成、练题、例题本、划重点、学习报告导出图片/Word） | ✅ |
| 6 | 分类教程 + 搜索、真机验证、release 包 | 🔧 收尾中 |

## 功能一览

- 💬 **对话式学习**：宋浩风格讲课（口头禅、类比库、挖坑示错、判卷给分点意识），token-by-token 流式输出，可随时打断
- 🎯 **两种模式**：精讲（听懂为主）/ 期末冲刺（直奔考点），切换不改 system prompt（前缀缓存友好）
- 🖼 **拍照做题**：支持视觉的模型直传图片；不支持的自动降级本地 OCR（RapidOCR）
- 📄 **文档上传**：txt/md 直传、docx/pdf 提取文本
- ✍️ **练题**：主题+难度 → 出题 → 作答 → AI 批改点评 → 详细解析，记录可回看
- ⭐ **例题本 / 划重点**：例题卡片一键收藏；重点按时间线管理
- 📊 **学习报告**：基于对话上下文生成，可导出长图 / Word（自研极简 docx 生成器）
- 🧊 **玻璃效果**：默认毛玻璃（haze），可开液态玻璃（AndroidLiquidGlass + glasense-ui，Android 13+），四个参数滑杆实时可调
- 🔒 **隐私**：API Key 用 Android Keystore 加密存储；所有数据只在手机本地
- ⚡ **缓存优化**：system prompt 字节级稳定分层（Layer 0 人设 / Layer 1 高数考点大纲 / Layer 2 定制段）+ Anthropic 显式 cache_control 断点 + 消息 append-only，对话详情可见缓存命中率

## 技术栈

- Kotlin 2.4 + Jetpack Compose（BOM 2026.08）+ Material 3，单 Activity + Navigation
- AGP 9.3 / Gradle 9.6 / minSdk 29 / targetSdk 36
- DI：Koin；持久化：Room + DataStore；密钥：EncryptedSharedPreferences
- 网络：OkHttp + 手写 SSE 解析（流式逐 token）
- Markdown：multiplatform-markdown-renderer；LaTeX：RaTeX（Rust 核心，无 WebView）
- OCR：RapidOcrAndroidOnnxCompose 源码模块（Apache-2.0）
- 玻璃：chrisbanes/haze + Kyant0 backdrop/Shapes + glasense-ui 源码模块（Apache-2.0）

## 构建

```bash
# 要求：JDK 17+，Android SDK（platform 37 / build-tools 36+）
git clone https://github.com/fypklanfanqie/college-learn-helper.git
cd college-learn-helper
./gradlew assembleDebug     # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # 产物：app/build/outputs/apk/release/（当前使用 debug 签名便于安装）
```

## 字体与开源组件声明

- Font: MiSans © Xiaomi（免费商用）
- glasense-ui / RapidOcrAndroidOnnxCompose / OpenCV 等组件遵循其原始开源协议（Apache-2.0 等）

## License

TBD（正式发布前确定）
