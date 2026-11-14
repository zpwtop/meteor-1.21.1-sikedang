# 1.0.0 验证记录

生产文件：`meteor-1.21.1-死氪党-1.0.0.jar`

SHA-256：`6e2ebe2364626ecef8dd77f0019692e5cdccac02adad3bc3a29b4843f3cea497`

- 编译：Java 21、Minecraft 1.21.1 + Yarn build.3、用户上传的 Meteor Client 0.5.8。
- 159 项逻辑检查通过：伤害、效果、槽位、方向转换、挖掘进度/确认等待/重试上限，以及快捷栏/背包/跨 tick 胸甲交换和恢复。
- 366 处生产 Minecraft/Meteor 字段和方法引用的链接检查通过。
- 运行依赖仅有 Java、Minecraft、Fabric Loader、Meteor；无 OpenHachimi 或 SlimefunHelper 引用、捆绑代码或运行依赖。
- 真实游戏环境：隔离的 Fabric Loader 0.16.10 + Minecraft 1.21.1 + Meteor 0.5.8，本地单人集成服务器，软件 OpenGL 渲染。
- 十个不同模块均已注册。
- 生存模式下，发包挖掘挖掉石头并收到服务器方块移除确认；快捷栏钻石镐和原手持槽位恢复。
- 甲飞实际在 49 个玩家 tick 中临时装备主背包第 24 槽的鞘翅；每个 tick 后检查胸甲为下界合金胸甲，鞘翅仍在原背包槽。
- 简易控制与甲飞同时运行，停用后原重力状态恢复。
- 着色器实际编译并显示彩虹/点阵/描边/发光效果；停用后原 Meteor 后处理对象恢复，再启用仍正常。实测图：`hachimi-shaders-preview.png`。
- 测试辅助源码在 `tests/smoke`，不打包进生产 JAR。辅助代码会改变隔离测试世界、设置测试装备并关闭自定义字体，请勿放入个人游戏存档运行。
- 原六模块的 1.0.0 空背包开关测试记录仍适用；它们的动作逻辑未作修改。
- 尚未验证多人服务器、LAZY/TICK_LEGACY、原版所有高级设置或与其他着色器/飞行插件同时运行。
