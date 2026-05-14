<p align="center">
  <img src="docs/images/economysystem-cover.png" alt="EconomySystem 封面图" width="100%" />
</p>

<h1 align="center">EconomySystem · 经济系统</h1>

<p align="center">
  为 Minecraft / 梦鱼服打造的服务器经济、商店、玩家市场与交易基础模组。
</p>

<p align="center">
  <img src="https://img.shields.io/github/v/release/QingMo-A/EconomySystem?style=flat-square&label=release" alt="Release" />
  <img src="https://img.shields.io/github/license/QingMo-A/EconomySystem?style=flat-square" alt="License" />
  <img src="https://img.shields.io/github/stars/QingMo-A/EconomySystem?style=flat-square" alt="Stars" />
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square" alt="Minecraft 1.21.1" />
  <img src="https://img.shields.io/badge/NeoForge-21.1-blue?style=flat-square" alt="NeoForge" />
</p>

---

## 项目介绍

**EconomySystem（经济系统）** 是一款面向 Minecraft 多人服务器的经济系统模组，围绕服务器内的统一货币、玩家账户、系统商店、玩家市场、交易记录、领地与传送玩法进行设计。

在梦鱼服中，经济系统只使用一种官方货币：**梦鱼币**。所有玩家余额、商品价格、交易金额、领地花费与经济玩法，均围绕梦鱼币展开，避免多货币体系带来的理解成本和数值混乱。

<p align="center">
  <img src="docs/images/mengyu-coin.png" alt="梦鱼币：服务器唯一货币" width="100%" />
</p>

---

## 功能总览

<p align="center">
  <img src="docs/images/feature-overview.png" alt="EconomySystem 功能总览" width="100%" />
</p>

| 功能 | 说明 |
| --- | --- |
| **梦鱼币** | 服务器唯一官方货币，用于商店、市场、交易、领地等经济玩法。 |
| **玩家账户** | 记录并管理玩家的梦鱼币余额。 |
| **系统商店** | 由服务器提供商品，支持价格配置与价格浮动。 |
| **玩家市场** | 玩家可以上架物品，由其他玩家使用梦鱼币购买。 |
| **玩家交易** | 围绕梦鱼币构建玩家间交易与资源流通。 |
| **交易记录** | 保存并展示交易行为，强调可靠、安全、公平。 |
| **领地系统** | 使用梦鱼币创建 2D 领地，支持管理与传送。 |
| **传送药水** | 虫洞药水与回忆药水提供更丰富的服务器玩法。 |

---

## 核心玩法展示

### 1. 服务器商店

系统商店由服务器统一配置商品，玩家可以使用梦鱼币购买建筑材料、工具、装备、药水等物品。商店价格可配置，也可以根据服务器玩法进行动态调整。

<p align="center">
  <img src="docs/images/server-shop.png" alt="服务器商店系统" width="100%" />
</p>

### 2. 玩家市场上架

玩家可以把自己的物品上架到市场中出售。适合服务器内的自由交易、资源流通、装备流转与玩家经营玩法。

<p align="center">
  <img src="docs/images/player-market-listing.png" alt="玩家市场上架物品" width="100%" />
</p>

### 3. 购买与交易

玩家之间的交易围绕梦鱼币展开，统一货币可以让交易价格更直观，也方便服务器管理者观察经济流向。

<p align="center">
  <img src="docs/images/player-market-trade.png" alt="玩家购买与交易" width="100%" />
</p>

### 4. 交易记录

交易记录用于体现服务器经济系统的可靠性。后续可以继续扩展为订单查询、交易审计、异常交易追踪等功能。

<p align="center">
  <img src="docs/images/transaction-records.png" alt="交易记录：可靠、安全、公平" width="100%" />
</p>

---

## 系统架构

EconomySystem 的整体逻辑可以理解为：玩家或管理员通过 **命令 / GUI** 进行操作，经济系统核心负责账户、商店、交易、日志等业务处理，并通过配置文件和数据存储保持服务器经济数据稳定。

```mermaid
flowchart LR
    Player[玩家] --> GUI[命令 / GUI]
    Admin[管理员] --> GUI
    GUI --> Core[EconomySystem Core\n核心业务逻辑]

    Core --> Account[账户管理]
    Core --> Shop[系统商店]
    Core --> Market[玩家市场]
    Core --> Trade[交易逻辑]
    Core --> Log[交易记录]

    Core --> Config[配置文件]
    Core --> Storage[数据存储]

    Account --> Gameplay[服务器经济玩法]
    Shop --> Gameplay
    Market --> Gameplay
    Trade --> Gameplay

    Currency[单一货币：梦鱼币] --> Core
```

---

## 使用教程

### 1. 打开菜单

- 默认按键为 **I**。
- 按下 **I** 可以打开经济系统菜单。
- 如有需要，可以在按键设置中搜索“打开菜单”并自行修改快捷键。

### 2. 系统商店

- 系统商店会出售一些服务器内物品。
- 商品价格会根据游戏时间刷新，例如正午或午夜。
- 商品价格存在浮动，可根据服务器配置调整。
- 将鼠标移动到物品图标上，可以查看物品详细信息。

### 3. 玩家市场

- 玩家可以在市场中出售物品。
- 玩家购买物品时，可以查看卖家等相关信息。
- 将鼠标移动到物品图标上，可以查看物品详情。

### 4. 我的领地（2D 领地）

领地系统基于 2D 区域选择，适合服务器中轻量化管理玩家空间。

#### 圈地方式

- 使用 **圈地杖** 进行圈地。
- 圈地杖可以通过一根木棍合成。
- 手持圈地杖时：
  1. 第一次右键方块，选择第一个点。
  2. 第二次右键方块，选择第二个点。
  3. 第三次右键方块，取消当前选择。

#### 领地花费与管理

- 每个格子需要 **20 枚梦鱼币**。
- 创建领地后，领地所有者可以管理或传送到领地。
- 成员仅能传送到领地，不能进行所有者管理操作。
- 所有者可以在任一领地输入：

```mcfunction
/setbackpoint
```

用于设定回城点。默认回城点为第一个圈地点。

### 5. 领地图标

所有者会根据领地所在维度显示不同图标：

| 维度 | 所有者图标 |
| --- | --- |
| 主世界 | 草方块 |
| 下界 | 地狱岩 |
| 末地 | 末地石 |

成员显示为：**木门**。

### 6. 虫洞 / 回忆药水

- **虫洞药水**：可在系统商店购买。使用后，可以使用 `tpa` 指令。
- **回忆药水**：可直接饮用，传送到出生点，也可以用于传送到领地。
- 目前已知问题：回忆药水存在不可堆叠 bug。

---

## 版本分支

请根据你的 Minecraft 版本选择对应分支：

- [1.20.1 分支](https://github.com/QingMo-A/EconomySystem/tree/1.20.1)
- [1.21.1 分支](https://github.com/QingMo-A/EconomySystem/tree/1.21.1)

如果你只是想体验最新构建，可以优先查看仓库的 [Releases](https://github.com/QingMo-A/EconomySystem/releases)。

---

## ToDo / 计划实现

功能不分先后，以下功能后续都有可能继续完善：

- UI 底层重做
- 订单查询
- 订单逻辑
- 领地权限
- 交易记录与订单审计增强
- 管理员商店配置工具
- ~~拍卖订单~~
- 赞助者玩偶 / 雕塑 / 名单

---

## 赞助者名单

排名不分前后，也与赞助金额无关。

| 姓名 | 赞助金额 |
| --- | ---: |
| poxiaojin | 20.00¥ |
| YHS116284 | 12.01¥ |
| 351987654321 | 35.10¥ |
| bugong | 1.50¥ |

感谢以上赞助者对项目的支持！你的支持使得项目可以持续进行和优化。

---

## 作者

- [QingMo](https://github.com/QingMo-A)
- [HanHanYu](https://github.com/HanHanYu)

---

## 许可证

本项目基于 **GPL-3.0 License** 开源，详细内容请查看 [LICENSE](LICENSE) 文件。
