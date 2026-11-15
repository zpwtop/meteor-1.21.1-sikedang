# meteor-1.21.4-死氪党-1.3.0 源码

本包只构建 Minecraft 1.21.4，包含十三项独立模块、对应适配代码、资源、Mixin 和测试。
新增“跳跳虎”，所有模块显示名称均无 hachimi/slime 前缀，统一位于“死氪党”分类。

需要 Java 21、Python 3 与网络；不需要 Gradle 或 javac。运行：

```sh
python build.py --test
```

输出：`dist/meteor-1.21.4-死氪党-1.3.0.jar`。
匹配 Meteor 1.21.4-42、Fabric Loader >=0.16.9。依赖会自动下载并校验哈希；不需要原客户端 JAR。
安装时先删除旧插件，保留 `.jar` 后缀；中文不显示时关闭 Meteor Config → Visual → Custom Font。

`release.json` 统一管理本包版本与依赖，`src/platform/1.21.4` 只含本版本适配。
主项目仍同时维护两个游戏版本，这里按用户要求分别导出。
功能及边界见 MODULES.md；验收结果见 VALIDATION.md。
测试辅助代码会改变隔离测试世界，不要放进个人存档；辅助代码不包含在插件中。
