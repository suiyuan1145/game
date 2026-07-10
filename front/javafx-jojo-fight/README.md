# JOJO Star Battle Arena

这是以 `fight/fight` 的平台近战玩法为核心，并接入现有 JavaFX
JOJO/MUGEN 角色资源的独立项目。

## 已整合内容

- 三条命、平台跳跃、近战命中框和固定 60 FPS 战斗循环
- 单人对 AI 与双人本地对战，可在主菜单选择模式
- Jotaro、Dio、Giorno、Pucci、Polnareff 等角色资源
- 角色待机、攻击、受击动画帧
- JOJO 场景背景
- 鼠标或键盘选择 P1/P2 角色
- 液氮、灼烧、爆炸、烟雾、治疗、无敌、时间停止七类技能
- 第一阶段为 150 生命；存活 20 秒进入二阶段，生命上限提升为 1500 并回满

## 角色技能

- Jotaro：白金之星范围连打
- Dio：第一阶段时停 5 秒，第二阶段时停 9 秒
- Giorno：黄金体验生命治愈
- Pucci：白蛇迷雾，降低伤害并封锁集气
- Polnareff：银色战车烈焰剑舞
- Dojo Jim：寒冰封锁
- Diavolo：绯红之王时间删除，双方都无法互相命中，地图进入星空领域
- Kamen Rider Ex-Aid：三阶段角色，Lv1 胖子形态，Lv2 正常形态，Lv99 无敌玩家永久免伤

## 运行

要求 JDK 21 和 Maven：

```powershell
cd C:\Users\36451\Desktop\vue\front\javafx-jojo-fight
mvn javafx:run
```

也可以双击 `run.bat`。

## 操作

- P1：`A/D` 移动，`W` 跳跃，`J` 攻击，按住 `K` 集气，`L` 技能
- P2：方向键移动，`↑` 跳跃，数字 `1` 攻击，按住数字 `2` 集气，数字 `3` 技能
- `Esc` 返回菜单

单人模式下 P2 由 AI 控制，会主动移动、跳跃、近战、集气并释放角色技能。只有 DIO 使用独立飞刀远程攻击，其他角色使用各自配置的 MUGEN `punch`/`kick` 动画。

## MUGEN 运行时解析

启动时会读取 `C:\Users\36451\Downloads\JOJO精致整合V6（主程序）`：

- `data/select.def`：人物与 `[ExtraStages]` 场景清单
- `chars/<人物>/*.def`：名称、作者以及 SFF/AIR/SND/CMD/CNS 文件引用
- `stages/*.def`：场景名称、镜头、舞台参数、SFF 背景与 BGM 引用

可通过 JVM 参数 `-Dmugen.home=目录` 或环境变量 `MUGEN_HOME` 覆盖默认目录。JavaFX 会播放可读取的场景 MP3，并把已有的 PNG 场景预览与解析结果匹配；原生二进制 `.sff/.snd` 当前建立索引但不直接解码。

选角卡会显示匹配到的 MUGEN 名称、作者和解析状态。确认双方角色后，游戏进入资源加载画面，逐项显示角色 DEF/SFF/AIR/CMD/CNS/SND 与本局场景 DEF/SFF/BGM 的可用状态；加载画面和实际战斗使用同一个已选场景。

选角页直接使用 `select.def` 解析出的 94 人运行时列表，每页最多 50 人，可用方向键移动并用 `Q/E` 翻页。角色姓名、作者、文件夹、加载画面和战斗 HUD 都保留实际选中的 MUGEN 人物。尚未提取 PNG 动作帧的人物会暂时采用近战战斗原型，但不会被错误识别为 DIO；只有文件夹或显示名精确为 `Dio` 的人物拥有飞刀。

角色定义位于 `src/main/resources/assets/characters/<角色>/character.properties`。
新增角色时只需增加角色目录和动画帧配置，再在 `Fighter.CharType` 注册。

新增图片来源记录见 `src/main/resources/assets/characters/SOURCES.md`。
