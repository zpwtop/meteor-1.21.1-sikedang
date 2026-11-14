# meteor-1.21.4-死氪党-1.0.0 源码

这个源码包**仅构建 Minecraft 1.21.4 版本**，包含十个模块、对应版本适配代码、资源、Mixin、测试和构建脚本，不包含其他 Minecraft 版本的适配代码。
需要 Java 21、Python 3 和网络连接。匹配的依赖会自动下载并校验哈希，无需安装 Gradle 或 javac。

```sh
python build.py --test
```

输出：`dist/meteor-1.21.4-死氪党-1.0.0.jar`。
运行环境：Minecraft 1.21.4、Meteor 1.21.4-42、Fabric Loader >=0.16.9，Java 21。
Meteor 官方下载：https://maven.meteordev.org/snapshots/meteordevelopment/meteor-client/1.21.4-SNAPSHOT/meteor-client-1.21.4-20250408.145004-42.jar

插件独立运行，不依赖 OpenHachimi、SlimefunHelper 或 Fabric API。安装时先移除旧插件，并保留 `.jar` 后缀。
中文名称不显示时，关闭 Meteor 的 Config → Visual → Custom Font。

对应 API 适配代码在 `src/platform/1.21.4`，模块主体代码在 `src/main/java`。
`release.json` 是当前包的版本和依赖信息来源。两个游戏版本在主项目中继续同步维护，这里按用户要求单独导出。
新增功能递增中间版本号，bug 修复递增末位；当前基准版本为 1.0.0。

功能与差异详见 MODULES.md，验证结果见 VALIDATION.md。
`tests/smoke` 是隔离单人测试辅助代码，不包含在插件中；它会改变测试世界和测试装备，不要放进个人游戏目录。
