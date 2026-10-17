# Spell Relay · 法术接力 (Forge 1.20.1)

Client-side spell sequences for Iron's Spells 'n Spellbooks. Configure physical spellbook slot numbers, press one key, and wait for each native server-confirmed cast to finish before continuing. No server installation of Spell Relay is required.

This is the Forge 1.20.1 port. The original NeoForge 1.21.1 project is maintained separately.

## 安装与使用

测试版本：Minecraft 1.20.1、Forge 47.4.13、Java 17，以及用户提供的 Iron's Spells 'n Spellbooks **1.20.1-3.15.4**。

1. 将 `spell-relay-1.20.1-forge-1.0.1.jar` 放入客户端实例的 `mods` 文件夹。
2. 客户端和服务器原本都应装好对应版本铁魔法及其必需前置。服务器无需安装法术接力。
3. 将法术书装备到铁魔法的法术书饰品槽。
4. 按 **F8** 打开本地设置，点击书格或输入释放顺序，例如 `3, 1, 5`，保存。
5. 按 **F9** 开始或停止当前组合，也可以使用界面的“保存并释放”。按键可在 Minecraft 控制设置中修改。

编号从 **1** 开始，指法术书内实际格子位置，包含空格。它不是轮盘中的第几个可用法术。界面会显示对应法术名称与等级。

支持多个命名组合，最多 32 个组合、每组最多 64 步。配置文件为 `config/ironautospell.json`；保持原项目的模组 ID 和配置格式。

## 释放规则

- 通过铁魔法自己的 `QuickCastPacket` 和 `setup.PacketDistributor.sendToServer` 请求施法；施法条件、法力和冷却均由原模组服务器判断。
- 每次只请求一招。收到本人该法术的开始确认与结束确认后，才继续下一招。
- 冷却中的法术可选择跳过、等待或停止。缺蓝、无效槽位、吟唱中断、书本改变、打开其他界面、死亡或离线会停止组合。
- 按 F9 停止后，已经开始的施法会自然结束。手动使用或攻击输入会停止队列。
- 当前不支持持续引导法术。处于可重施法状态的法术也会停止，避免自动撤回召唤物或关闭状态。
- 若同名法术被其他装备提供的版本合并，无法可靠对应法术书原槽位时会拒绝释放。
- 不提供自动瞄准、无限循环或自动刷新 BUFF；它执行一次用户配置的序列。

尚未覆盖所有整合包、第三方扩展法术和所有铁魔法历史版本。**3.4.x 使用旧接口，本构建不适用。**

## 从源码构建

需要 JDK 17。工程基于用户提供的 Forge 1.20.1-47.4.13 MDK，Gradle Wrapper 为 8.8，ForgeGradle 固定为 6.0.54。

自行从各模组作者处获取下列依赖，将文件放入工程 `libs/`。源码压缩包不附带第三方模组。

| 文件名 | 用途 |
| --- | --- |
| `irons_spellbooks-1.20.1-3.15.4.jar` | 编译、运行 |
| `curios-forge-5.14.1+1.20.1.jar` | 编译、运行 |
| `geckolib-forge-1.20.1-4.8.4.jar` | 运行 |
| `player-animation-lib-forge-1.0.2-rc1+1.20.jar` | 运行 |

用户提供的铁魔法文件名带有中文前缀，放入 `libs/` 时重命名为上表名称即可。

```powershell
$env:JAVA_HOME = '你的 JDK 17 安装路径'
.\gradlew.bat build
```

首次构建需要联网下载 Forge 与 Minecraft 的开发依赖。正式 JAR 位于 `build/libs/`，必须使用执行过 `reobfJar` 的构建结果。`test-fixture.jar` 仅用于测试，不是交付给玩家的模组。

开发运行需要先执行 `createSrgToMcp`。`runClient` 为开发客户端；`runServer` 和 `runSmoke` 为 `src/smoke` 中的测试辅助环境。测试服务端在发现安装法术接力时会主动报错，以保证验证的是客户端单独安装的情形。

## 许可与致谢

自身代码采用 MIT 许可，见 `LICENSE`。第三方代码与素材未打包，相关说明见 `THIRD_PARTY_NOTICES.md`。源码附带原模板、Forge MDK 和 Gradle Wrapper 的许可文本。

项目主要由 AI 编写，经自动测试和游戏实机联机验证。AI 开发方式应在公开发布时如实说明。不同分发平台的接收规则另行适用。

本项目是非官方铁魔法附属模组。感谢 Iron431、Lab3 及 Forge、Curios、GeckoLib、Player Animator 的作者。
