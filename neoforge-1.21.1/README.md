# Iron Auto Spell / 铁魔法法术连击

Minecraft 1.21.1 / NeoForge 21.1.250+ / Iron’s Spells ’n Spellbooks 3.16.3。

只安装在客户端；服务器只需要原有的铁魔法及其前置，不需要本模组。通过原模组 QuickCastPacket 发起正常施法，不能绕过冷却、法力、学习要求或吟唱时间。

## 安装与使用

1. 把交付的 `iron-auto-spell-1.21.1-neoforge-1.0.1.jar` 放进客户端 `mods` 目录并重启游戏。无需额外安装新的前置。
2. 将已有法术书装备到 Curios 的法术书饰品槽。
3. 按 **F8** 打开设置。左侧点击书格可追加步骤，或右侧直接填写 `3, 1, 5` 这样的书内格子顺序。序号从 1 开始，空格也计入编号。
4. 可以新建多套组合、修改名称，以及设置冷却时跳过、等待或停止。保存后，**F9** 开始/停止当前组合。
5. 也可以在设置页面点击“保存并释放”。下一招只在当前招收到服务端正常结束通知后发出。
6. 按键冲突时，在游戏“选项 → 控制 → 按键绑定 → 铁魔法法术连击”修改 F8/F9。

F9 停止的是后续连击队列，已经开始的单个法术会正常结束。没有无限循环和自动刷新 BUFF；一次启动执行一遍顺序。

## 注意事项

- 配置保存在 `config/ironautospell.json`，只保存本地组合，不上传服务器。
- 缺蓝、施法被打断、换书、死亡、离线、打开其他界面、手动攻击/使用物品时停止队列。
- 灰色书格可能为空、锁定，或其中法术被另一件装备的同名法术合并。为了保证按指定书格施法，本版本不悄悄改用其他装备上的版本。遇到这种情况，调整重复法术的装备后再启动。
- 持续引导法术暂不支持加入组合；已有重施法状态的法术会停止队列，避免关闭护盾或撤销召唤。
- 连击结束仅表示法术正常施放完成。需要瞄准目标的技能，仍须自己瞄准；不会自动选目标。
- 启动请求 30 秒未得到服务端确认会停止，并提示检查法术前置条件或网络；不会假装成功，也不会不断重发请求。
- 更新铁魔法到不同大版本后需重新验证接口。本次开发针对原整合包的 3.16.3。

## 开发与测试

需要 Java 21。`gradlew.bat build` 编译并运行状态机测试；产物位于 `build/libs`。编译依赖使用 `libs/` 中从本地整合包复制的五个原模组/前置 JAR；它们不嵌入交付的 JAR，也不随源代码包分发。请自行从各模组官方页面获取相同版本。

`src/smoke` 是独立测试夹具，不编入正式 JAR。`runServer` 仅加载原模组和测试夹具，明确检查服务端没有 IronAutoSpell；`runSmoke` 加载客户端模组并验证原生施法请求、空格映射、BUFF 和界面。测试仅使用独立的 `run-server`、`run-smoke` 目录，不访问用户的真实服务器和存档。

开发时调研的相似项目：Spell Combo System 当时仅 1.20.1 Forge 且需要服务端；Incantation 支持 1.21.1 但同样要求服务端配合，因此本项目按纯客户端需求独立实现，没有复制它们的代码。

- https://www.curseforge.com/minecraft/mc-mods/scs-spell-combo-system-irons-spells-addon
- https://github.com/Hoggormino/incantation


## 发布和许可

本项目的原创代码和翻译采用 MIT 许可。分发时保留 LICENSE 中的版权与许可文本；含 MDK 模板的源码分发还须保留 TEMPLATE_LICENSE.txt，Gradle Wrapper 采用 Apache-2.0。第三方依赖保留各自许可，不包含在本模组的 MIT 授权中，详情见 THIRD_PARTY_NOTICES.md。

只发布正式 JAR，以及已经排除第三方依赖的 source.zip。不要上传整个开发文件夹，特别是 libs、build、run-server、run-smoke、downloads 和 docs/reference。

本项目是非官方附属，依赖 Iron’s Spells ’n Spellbooks；请通过原作者页面安装本体和前置。不要复用本体的 Logo、贴图、模型或声音作为本项目素材。

开发披露：本项目代码、测试、翻译和文档大量由 AI 编程助手根据用户需求生成并迭代。模组运行时不调用 AI 服务。发布平台的 AI 内容规则和自动施法规则需单独遵守；截至 2026-10-16，Modrinth 内容规则第 6.2 条不允许公开发布主要由 AI 输出构成的项目，因此不能承诺或建议将当前版本直接公开上架 Modrinth。免费分发的许可不等于具体平台会接受上架。

## 构建依赖版本

将这些版本的 NeoForge JAR 放入本目录 `libs/` 后构建，文件名可以保留下载时的名称：

- Iron’s Spells ’n Spellbooks 3.16.3（Minecraft 1.21.1）
- Curios 9.5.1
- GeckoLib 4.9.3
- Iron’s Lib 2.2.0
- Player Animator 2.0.4

需 JDK 21。运行 `./gradlew build`（Windows 使用 `gradlew.bat build`）。
