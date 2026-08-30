# EconomySystem 赚钱途径与动态委托规划

## 目标

为 EconomySystem 增加比“击杀直接发钱”更丰富、更可控的赚钱途径，让不同类型玩家都能通过正常 Minecraft 游戏行为参与经济，同时避免自动化农场无限制造货币、长期通胀和单一最优解。

当前规划优先级：

- **P0 主线：动态委托 / 工作系统**
- **P0 主线：服务器回收站**
- **P2 备选：职业系统**
- **P2 备选：探索奖励**

委托系统采用明确的“题库式”设计：

> **管理员维护个人委托库，系统从合法模板中程序化生成个人委托；公共大型委托不参与随机生成，只能由管理员手动创建。**

同时，所有“奖励型收入”统一通过邮箱发放，具体邮件结算约束参见：

`plans/commission-mail-reward-delivery.md`

---

# 一、总体经济原则

## 1.1 不把普通动作直接等同于生成货币

不建议新增：

```text
挖 1 个方块 -> +X 梦鱼币
收割 1 个作物 -> +X 梦鱼币
钓到 1 条鱼 -> +X 梦鱼币
砍 1 个木头 -> +X 梦鱼币
```

这类机制极易被自动化农场转化为无限货币来源。

新的赚钱系统优先采用：

- 管理员可控的委托库；
- 独立刷新时间；
- 单条任务独立过期；
- 动态价格或冻结报价；
- 玩家 / 全服配额；
- 服务端权威验证；
- 奖励邮件结算。

## 1.2 区分“奖励”与“交易”

### 奖励型收入

例如：

- 个人委托；
- 公共大型委托；
- 未来探索任务；
- 未来职业任务；
- 活动 / 剧情任务。

统一：

```text
完成目标
   ↓
服务端确认
   ↓
生成奖励记录
   ↓
发送奖励邮件
   ↓
玩家在邮箱领取
```

不得完成后直接把梦鱼币加入余额。

### 交易型收入

例如：

- 回收站；
- 市场成交；
- 系统商店；
- 玩家转账。

默认保持即时交易结算，不强制经过邮箱。

## 1.3 优先复用现有系统

实现时优先复用：

- Economy 账户与 Ledger；
- 邮箱与系统奖励邮件；
- 市场 ItemStack / 事务思路；
- 系统商店价格数据；
- 实体死亡等服务端事件；
- UI 2 现有组件；
- `common + target adapter` 多版本架构。

不得另外复制一套余额、物品快照、邮件、事务或平台专属业务模型。

---

# 二、委托系统总模型

委托分成两类：

```text
Commission
│
├── Personal Commission
│   ├── 来自管理员委托库
│   ├── 系统自动生成
│   ├── 每玩家独立刷新
│   ├── 每次默认生成 1~2 条
│   ├── 每条拥有独立 expiresAt
│   ├── 刷新不会删除旧委托
│   └── 奖励发送到邮箱
│   └── 有最大委托数量上限(若差一次刷新就达到则按最少达到数量刷新新委托,若已达到则不刷新)
│
└── Public Large Commission
    ├── 不自动生成
    ├── 不参与刷新
    ├── 只能由管理员创建
    ├── 全服共享进度
    ├── 管理员自定义委托人 / 委托物 / 数量 / 单价 / 总预算 / 过期时间
    └── 奖励发送到邮箱
```

旧设计中的“公共委托自动批次刷新”和“个人批次刷新时整体替换”不再采用。

---

# 三、个人委托库：题库式程序化生成

## 3.1 基本思想

管理员创建的不是每次玩家最终看到的任务，而是用于系统出题的“委托库”。

系统负责：

```text
到达玩家刷新时间
        ↓
选择合法 CommissionTemplate
        ↓
选择合法 CommissionRequester
        ↓
选择合法 CommissionTargetPool 目标
        ↓
生成数量
        ↓
计算并冻结奖励
        ↓
生成独立 CommissionInstance
        ↓
加入玩家当前委托列表
```

系统不得无约束地从所有委托人和所有物品中完全随机组合。

## 3.2 核心对象

个人随机委托核心模型：

```text
CommissionTemplate
CommissionRequester
CommissionTargetPool
CommissionInstance
PersonalCommissionSchedule
```

语义：

- `CommissionTemplate`：委托题型；
- `CommissionRequester`：委托人；
- `CommissionTargetPool`：可抽取目标池；
- `CommissionInstance`：最终生成且冻结的实际任务；
- `PersonalCommissionSchedule`：某玩家的下一次刷新时间及刷新配置状态。

不再强制使用 `PersonalCommissionBatch` 来表示“这一轮任务”。

---

# 四、CommissionTemplate：个人委托模板

模板定义一类个人委托如何生成。

例如：

```text
模板：矿产供应委托
类型：ITEM_DELIVERY
委托人池：mining_requesters
目标池：basic_mining_materials
数量：64 ~ 256
步长：32
奖励：基础价值 × 数量 × 0.9~1.3
权重：100
过期时间：2h ~ 4h
```

建议字段：

```text
id
type
requesterPool
targetPool
quantityMin
quantityMax
quantityStep
rewardMode
rewardMultiplierRange
weight
category
rarity
expirationMin
expirationMax
playerLimit
textTemplate
specialConditions
requiredProfession
requiredProfessionLevel
professionExperienceReward
```

模板只用于生成个人随机委托。

公共大型委托不要求从 `CommissionTemplate` 自动生成。

---

# 五、CommissionRequester：委托人

委托人不仅是显示名称，也可以带生成与经济特征。

例如：

```text
城镇铁匠铺
允许目标：铁 / 煤 / 铜
需求量倍率：0.8
奖励倍率：1.05
稀有度：普通
```

```text
皇家军需处
需求量倍率：2.5
奖励倍率：1.15
出现权重：低
特点：大量资源需求
```

```text
神秘收藏家
允许目标：稀有物品
需求量：低
奖励：高
出现概率：极低
```

模板引用委托人池，而不是绑定全局所有委托人。

例如：

```text
mining_requesters
├── 城镇铁匠铺
├── 北方矿业协会
└── 皇家军需处
```

这样避免系统自然生成：

```text
渔夫要求钻石块
铁匠要求河豚
农夫要求下界合金
```

除非管理员明确设计特殊模板。

---

# 六、CommissionTargetPool：目标池

目标池负责定义可被抽取的目标。

例如：

```text
basic_mining_materials
├── minecraft:iron_ingot
├── minecraft:copper_ingot
├── minecraft:coal
└── minecraft:gold_ingot
```

```text
basic_crops
├── minecraft:wheat
├── minecraft:carrot
├── minecraft:potato
└── minecraft:beetroot
```

后续支持：

- Item ID；
- Minecraft Tag；
- EconomySystem 自定义池；
- 其他模组物品；
- EntityType；
- 未来群系 / 结构 / advancement 目标。

---

# 七、CommissionInstance：独立实际委托

## 7.1 每条委托都是独立实例

个人委托生成后成为独立 `CommissionInstance`。

例如：

```text
委托人：城镇铁匠铺
目标：铁锭
数量：160
最终奖励：1260 梦鱼币
生成时间：14:00
过期时间：17:20
```

其生命周期只由该实例自身决定。

## 7.2 生成后冻结

实例生成时冻结：

- 委托人；
- 目标；
- 数量；
- 单价或总奖励；
- 文本；
- 过期时间；
- 特殊条件。

假设：

```text
12:00 生成
铁锭 ×128
奖励 960
```

12:05 系统商店价格变化后，该任务仍然是：

```text
铁锭 ×128
奖励 960
```

配置 reload 也只影响未来生成任务，不修改已有实例。

## 7.3 建议字段

```text
commissionId
templateId
requesterId
type
targetSnapshot
requiredAmount
progress
rewardSnapshot
generatedAt
expiresAt
status
ownerPlayerUuid
```

公共大型委托可使用独立的 `PublicCommissionInstance` 或在同一实例模型中以 subtype / scope 区分。

---

# 八、个人委托刷新系统

## 8.1 刷新与过期完全独立

这是当前正式设计的关键规则。

### `nextRefreshAt`

只表示：

> 系统什么时候再次为这名玩家生成新的个人委托？

### `expiresAt`

只表示：

> 这一条具体委托什么时候失效？

二者互不限制。

**刷新不会删除旧委托。**

不再使用：

```text
effectiveEndAt = min(expiresAt, nextRefreshAt)
```

## 8.2 示例

14:00 玩家已有：

```text
铁锭委托
expiresAt = 17:30

僵尸委托
expiresAt = 20:00
```

15:00 到达 `nextRefreshAt`，系统新增：

```text
小麦委托
expiresAt = 19:20

铜锭委托
expiresAt = 18:10
```

最终玩家同时拥有：

```text
铁锭     17:30 过期
僵尸     20:00 过期
小麦     19:20 过期
铜锭     18:10 过期
```

旧委托不会因为 15:00 的刷新而消失。

## 8.3 每名玩家独立刷新

每名玩家单独持久化：

```text
PersonalCommissionSchedule
├── playerUuid
├── nextRefreshAt
└── lastRefreshAt
```

玩家 A、B、C 的刷新时间可以不同。

建议：

```text
baseInterval + 小范围 jitter
```

例如：

```text
基础刷新：4h
偏移：±30m
```

玩家可能得到：

```text
A：3h47m
B：4h18m
C：4h03m
```

随机偏移不能过大，避免明显不公平。

## 8.4 每次刷新生成数量

管理员可配置：

```text
minCommissionsPerRefresh
maxCommissionsPerRefresh
```

默认：

```text
minCommissionsPerRefresh = 1
maxCommissionsPerRefresh = 2
```

即每次刷新随机新增 **1~2 条个人委托**。

管理员可以调整为：

```text
1~1
2~3
1~4
```

但不建议默认一次生成大量任务。

## 8.5 最大同时存在数量

由于刷新不会清理旧任务，需要一个可配置的安全上限：

```text
maxActivePersonalCommissions
```

建议默认：

```text
6
```

例如玩家当前已有 5 条未完成且未过期任务，本次计划生成 2 条，则最多只新增 1 条。

该参数主要用于：

- 防止长期未完成任务无限堆积；
- 控制客户端列表规模；
- 控制服务端持久化与进度监听量。

是否将默认值定为 6 可以在实现前继续调整，但字段应保留。

## 8.6 使用绝对服务端时间

刷新和过期都使用服务端绝对时间：

```text
nextRefreshAt
expiresAt
```

不使用“打开 GUI 后开始倒计时”的方式，也不依赖客户端计时。

服务器关服期间现实时间继续推进。

例如：

```text
16:00 关服
nextRefreshAt = 18:00
20:00 再开服
```

服务端发现当前时间已超过 `nextRefreshAt` 时，执行一次刷新逻辑并重新安排下一刷新时间。

旧任务是否存在只看各自 `expiresAt`，与这次刷新无关。

---

# 九、个人委托过期

每条个人 `CommissionInstance` 拥有自己的 `expiresAt`。

例如：

```text
普通委托：2~4h
紧急委托：20~60min
稀有委托：1~3h
长期个人委托：6~12h
```

模板可以固定有效期，也可以配置范围后在生成时冻结实际过期时间。

到达 `expiresAt` 后：

```text
AVAILABLE / ACTIVE
        ↓
EXPIRED
```

未完成进度不继承到新任务。

例如：

```text
击杀僵尸 13 / 20
expiresAt 到达
↓
EXPIRED
```

下一次即使又生成“击杀僵尸”任务，也必须是新的 `commissionId` 和独立进度。

---

# 十、个人委托生成规则

每次刷新不应简单从全部模板无约束随机。

建议使用：

```text
分类配额 / 分类权重
        +
模板权重
        +
合法性过滤
```

由于默认每次只生成 1~2 条，第一版可以使用较简单的分类权重，例如：

```text
物资 60%
猎杀 30%
稀有 10%
```

然后再在该分类内按模板 `weight` 抽取。

未来职业系统加入后，可对个人权重做轻量个性化，但不能完全锁死玩家玩法。

---

# 十一、个人委托结算

## 11.1 物资委托

个人物资任务通常要求完整完成后结算。

例如：

```text
铁锭 64 / 64
```

服务端确认后：

```text
COMPLETED
↓
生成 CommissionRewardRecord
↓
发送奖励邮件
↓
玩家邮箱领取梦鱼币附件
```

不得直接修改玩家余额。

## 11.2 猎杀委托

进度由服务端实体死亡事件记录。

例如：

```text
僵尸 13 / 20
```

达到 20 后完成并发送奖励邮件。

需要处理：

- 玩家击杀归属；
- 非玩家击杀；
- 自动化陷阱；
- 重复事件；
- 任务过期后的事件忽略；
- 重复网络包与重复结算。

---

# 十二、公共大型委托

## 12.1 核心定位

公共大型委托用于：

- 大型服务器建设；
- 临时物资需求；
- 节日活动；
- 剧情事件；
- 管理员主导的全服目标。

**公共大型委托绝不由系统自行随机生成。**

**公共大型委托不参与任何自动刷新周期。**

只能由管理员主动创建、修改、结束或删除。

## 12.2 管理员可设置字段

至少支持：

```text
commissionId
name
requesterName
targetItem
targetAmount
unitReward
totalBudget
generatedAt
expiresAt
description
status
```

其中：

- `requesterName`：管理员自由设置委托人名称；
- `targetItem`：指定委托物；
- `targetAmount`：总需求数量；
- `unitReward`：每单位物品奖励；
- `totalBudget`：总金额 / 总预算；
- `expiresAt`：公共大型委托独立过期时间。

## 12.3 单价和总金额关系

推荐主要输入：

```text
targetAmount + unitReward
```

系统自动计算：

```text
totalBudget = targetAmount × unitReward
```

例如：

```text
委托人：梦屿城市建设局
委托物：minecraft:stone_bricks
需求：10000
单价：3 梦鱼币
总预算：30000 梦鱼币
过期：7 天后
```

如果管理员界面允许直接填写总预算，则应明确校验：

```text
totalBudget == targetAmount × unitReward
```

或由系统根据其中两个值自动计算第三个值，避免三个数相互矛盾。

第一版建议：

> **数量 + 单价为权威输入，总预算只读自动计算。**

## 12.4 全服共享进度

例如：

```text
【城市扩建计划】

委托人：梦屿城市建设局
目标：石砖
需求：10000
已提交：6821
剩余：3179
单价：3 梦鱼币
剩余预算：9537 梦鱼币
剩余时间：4 天 13 小时
```

所有玩家共享：

```text
remainingAmount
```

并由服务端原子更新，防止多人同时提交时超收。

## 12.5 部分提交

公共大型委托必须支持部分提交。

例如：

```text
玩家提交 64 石砖
单价 3
奖励 = 192 梦鱼币
```

处理：

```text
服务端确认委托仍有效
↓
确认 remainingAmount >= 可接受数量
↓
扣除实际接受的物品
↓
更新公共剩余需求
↓
生成 192 梦鱼币奖励记录
↓
发送奖励邮件
```

奖励不直接到账。

一次玩家确认提交对应一封结算邮件，不能按每个物品或每个单位拆邮件。

## 12.6 公共大型委托过期

公共大型委托只有自己的：

```text
expiresAt
```

没有自动 `nextRefreshAt`。

达到过期时间后：

```text
AVAILABLE
↓
EXPIRED
```

管理员也可以提前：

```text
DISABLED / CANCELLED / COMPLETED
```

公共大型委托过期、完成或管理员关闭后不自动生成替代任务。

---

# 十三、公共大型委托管理员工具

建议第一版管理员指令至少支持：

```text
/economy_system commission public create ...
/economy_system commission public list
/economy_system commission public remove <id>
/economy_system commission public end <id>
/economy_system commission public info <id>
```

由于参数较多，最终更推荐做一个管理员配置 GUI；但首版可以先使用配置文件 + 命令完成创建。

公共大型委托管理员应能够设置：

- 委托名称；
- 委托人；
- 委托物；
- 总需求数量；
- 单价；
- 自动计算总金额；
- 过期时间；
- 描述文本。

不允许系统从个人委托库自动“升级”出公共大型委托。

---

# 十四、委托状态

个人委托建议：

```text
AVAILABLE
ACTIVE
COMPLETED
EXPIRED
DISABLED
LOCKED
```

公共大型委托建议：

```text
AVAILABLE
COMPLETED
EXHAUSTED
EXPIRED
CANCELLED
DISABLED
```

其中：

- `COMPLETED / EXHAUSTED`：需求已经完全满足；
- `EXPIRED`：到达过期时间；
- `CANCELLED`：管理员主动终止。

所有状态以服务端为权威。

---

# 十五、委托奖励邮件

所有委托奖励统一走邮箱。

委托完成页面只显示：

```text
委托已完成
奖励已发送至邮箱
```

奖励邮件建议使用实际委托人作为发件人：

```text
发件人：城镇铁匠铺
主题：物资委托结算
```

正文例如：

```text
感谢你按约提交了我们需要的铁锭。
本次报酬已经随信附上。
```

货币通过邮箱货币附件领取。

要求：

- 邮箱满不能吞奖励；
- 奖励记录必须防重复投递；
- 附件领取必须幂等；
- 未领取奖励邮件不能被普通删除；
- 离线玩家正常持久化；
- Economy Ledger 在真正领取货币附件时记录余额变化。

详细规则以 `commission-mail-reward-delivery.md` 为准。

---

# 十六、委托中心 UI

建议一级页面：

```text
委托中心
```

顶部页签：

```text
我的委托 | 公共大型委托
```

## 16.1 我的委托

顶部显示：

```text
下一次新增委托：01:37:24
当前任务：4 / 6
```

列表中每条独立显示自己的过期时间：

```text
城镇铁匠铺
铁锭 ×64
奖励：520 梦鱼币
过期：02:18:41
```

```text
猎人协会
击杀僵尸 13 / 20
奖励：380 梦鱼币
过期：05:02:10
```

刷新发生时，只把新任务加入列表。

UI 不应因为刷新倒计时归零就清空现有卡片。

## 16.2 公共大型委托

例如：

```text
【城市扩建计划】

梦屿城市建设局
石砖

6821 / 10000
单价：3 梦鱼币
剩余预算：9537 梦鱼币
过期：4天13小时

[选择数量] [提交]
```

没有“下一次公共刷新”倒计时。

如果管理员没有发布大型委托，则显示：

```text
当前暂无公共大型委托
```

---

# 十七、管理员配置

建议：

```text
config/
└── economysystem/
    └── commissions/
        ├── requesters/
        ├── target_pools/
        ├── templates/
        └── settings.json
```

`settings.json` 管理个人委托自动生成规则，例如：

```text
personalRefreshBaseInterval
personalRefreshJitter
minCommissionsPerRefresh
maxCommissionsPerRefresh
maxActivePersonalCommissions
categoryWeights
defaultExpirationMin
defaultExpirationMax
rewardMultiplierBounds
```

默认建议：

```text
minCommissionsPerRefresh = 1
maxCommissionsPerRefresh = 2
```

公共大型委托不进入自动生成配置。

---

# 十八、管理员个人委托库指令

至少提供：

```text
/economy_system commission reload
```

后续建议：

```text
/economy_system commission template list
/economy_system commission debug <template>
/economy_system commission refresh player <player>
/economy_system commission generate player <player> <template>
```

强制刷新个人玩家时，应：

```text
新增任务
```

而不是：

```text
删除旧任务 + 整批替换
```

---

# 十九、防滥用与事务要求

所有委托提交和结算必须服务端验证：

- 玩家实际拥有目标物品；
- NBT / Data Component 匹配；
- 数量；
- commissionId 是否有效；
- 当前时间是否超过 `expiresAt`；
- 公共剩余需求；
- 玩家个人提交限制；
- 猎杀进度是否来自服务端事件；
- 重复网络包不得重复结算；
- 奖励邮件不得重复创建。

物品扣除、任务更新、奖励记录创建必须使用事务式思路。

不得出现：

```text
物品未扣但生成奖励
```

或：

```text
物品扣除成功但奖励记录完全丢失
```

---

# 二十、审计与经济流水

委托奖励在“领取邮件货币附件”时进入 Economy Ledger。

建议来源：

```text
economysystem:commission_reward
```

奖励记录和审计信息仍建议保存：

- playerUuid；
- commissionId；
- templateId；
- requester；
- target；
- quantity；
- unitReward；
- totalReward；
- generatedAt；
- completedAt；
- mailId；
- claimedAt。

公共大型委托额外保存：

- totalTargetAmount；
- submittedAmount；
- remainingAmount；
- totalBudget；
- remainingBudget。

---

# 二十一、P0：服务器回收站

## 21.1 定位

回收站是低收益、快速处理过剩资源的最低价值出口。

其目的：

- 减少垃圾资源直接丢弃；
- 给普通生存行为提供少量收入；
- 提供可控的小额注币；
- 使用周期高价回收引导短期资源生产。

## 21.2 基础回收

例如：

```text
腐肉 ×64 -> 6 梦鱼币
圆石 ×64 -> 2 梦鱼币
蜘蛛眼 ×64 -> 8 梦鱼币
海带 ×64 -> 4 梦鱼币
```

基础回收价格必须明显低于正常玩家市场合理交易价格。

## 21.3 周期高价回收

例如：

```text
本期高价回收
骨头 +50%
铜锭 +30%
海带 +80%
```

支持周期需求上限：

```text
海带
剩余高价需求：1536
```

额度耗尽后可配置：

- 恢复普通回收价；
- 或本周期停止收购。

## 21.4 与委托的区别

### 委托

- 奖励更高；
- 有委托人；
- 有任务目标；
- 有刷新 / 过期；
- 奖励通过邮件。

### 回收站

- 收益低；
- 操作快速；
- 不需要接任务；
- 主要处理过剩资源；
- 属于即时交易，不强制走邮箱。

二者可共享报价、需求、配额与事务基础设施，但 UI 与业务语义分离。

---

# 二十二、P2：职业系统

职业系统作为个人委托的成长层，而不是普通动作直接发钱。

例如：

```text
完成矿业委托
↓
获得职业经验
↓
矿工升级
↓
解锁高级矿业委托
```

可预留：

```text
requiredProfession
requiredProfessionLevel
professionExperienceReward
```

未来个人委托可轻量根据职业调整抽取权重，例如：

```text
60~70% 符合当前职业方向
30~40% 其他合法委托
```

避免把玩家完全锁死在一种玩法。

---

# 二十三、P2：探索奖励

探索奖励未来作为：

```text
CommissionType.EXPLORATION
```

接入个人委托系统。

例如：

```text
发现新的蘑菇岛
奖励 1200 梦鱼币
```

```text
寻找林地府邸
奖励 1800 梦鱼币
```

必须尽量通过服务端可验证信息判断：

- 玩家位置；
- 维度切换；
- advancement；
- 服务端结构查询；
- 玩家完成记录。

探索奖励同样通过奖励邮件领取。

---

# 二十四、建议实施顺序

## Phase 1：个人 Commission 核心

建立：

```text
CommissionTemplate
CommissionRequester
CommissionTargetPool
CommissionInstance
PersonalCommissionSchedule
CommissionGenerator
CommissionRepository
CommissionRewardRecord
```

实现：

- JSON / 配置加载；
- reload；
- 合法性过滤；
- 权重抽取；
- 奖励冻结；
- 独立 `expiresAt`；
- 每玩家 `nextRefreshAt`；
- 每次默认新增 1~2 条；
- 刷新不删除旧任务；
- 最大活跃任务限制；
- 服务端持久化。

## Phase 2：个人物资委托

实现 `ITEM_DELIVERY`：

- 物资提交；
- 完成验证；
- 奖励记录；
- 奖励邮件；
- UI。

## Phase 3：公共大型委托

实现管理员手动创建：

- 自定义委托人；
- 自定义物品；
- 自定义数量；
- 自定义单价；
- 自动计算总预算；
- 自定义过期时间；
- 全服共享进度；
- 部分提交；
- 邮件奖励。

## Phase 4：回收站 MVP

实现：

- 回收配置；
- 服务端报价；
- 背包验证；
- 即时结算；
- 周期高价回收；
- 配额；
- UI。

## Phase 5：猎杀委托

实现 `ENTITY_KILL` 并复用现有服务端死亡事件。

## Phase 6：高级系统

稳定后评估：

- 职业；
- 探索；
- 节日 / 剧情任务；
- 管理员 Commission GUI；
- 委托历史；
- 个人委托个性化权重。

---

# 二十五、当前正式设计决策摘要

1. **动态个人委托为 P0 主线。**
2. **服务器回收站为 P0 主线。**
3. 个人委托来自管理员维护的题库，系统负责随机生成最终实例。
4. 核心个人题库对象为 `CommissionTemplate / CommissionRequester / CommissionTargetPool`。
5. 每名玩家拥有独立 `PersonalCommissionSchedule.nextRefreshAt`。
6. 个人委托刷新时间与单条 `expiresAt` 完全独立。
7. **刷新只新增委托，不删除仍未过期的旧委托。**
8. 删除旧规则 `effectiveEndAt = min(expiresAt, nextRefreshAt)`。
9. 每次个人刷新默认新增 **1~2 条**，管理员可配置最小值与最大值。
10. 建议保留 `maxActivePersonalCommissions`，防止独立过期任务无限堆积。
11. 个人刷新与过期均使用服务端绝对时间，不因离线、关服或不开 GUI 而暂停。
12. 每个 `CommissionInstance` 生成后冻结委托人、目标、数量、奖励和过期时间。
13. 配置 reload 只影响未来生成任务。
14. **公共大型委托禁止系统自动生成。**
15. **公共大型委托没有自动刷新周期。**
16. 公共大型委托只能由管理员主动创建。
17. 管理员可以自定义公共大型委托的委托人、委托物、总数量、单价、总金额 / 总预算、描述和过期时间。
18. 第一版建议以“数量 + 单价”为权威输入，系统自动计算总预算。
19. 公共大型委托全服共享剩余需求，并支持部分提交。
20. 公共大型委托过期或完成后不会自动生成替代任务。
21. **所有委托奖励统一发送到邮箱，不直接到账。**
22. 公共大型委托部分提交产生的奖励也通过邮箱发送，一次提交对应一封奖励邮件。
23. 回收站属于交易行为，默认即时到账，不强制走邮箱。
24. 职业系统作为 P2 成长层，不采用普通动作直接发钱。
25. 探索奖励作为 P2，未来接入同一 Commission 框架。
26. 所有委托验证、物品扣除、过期、进度、公共剩余数量、奖励记录和邮件投递必须服务端权威。
