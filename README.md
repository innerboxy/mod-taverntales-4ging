# taverntales_4ging

Minecraft **1.21.1** / **NeoForge** 模组,为 TavernTales 提供一个数据驱动的装备锻造台。

---

## 1. 简介

本模组添加**装备锻造台**(`taverntales_4ging:equipment_forge`)——一个以"选中配方即可制作"为核心的合成方块,用来取代原版工作台里繁琐的装备摆放。

### 核心特点

- **材料直接取自背包**:锻造台没有合成格子,材料从玩家主背包 + 副手中扣除,不需要来回搬运物品。
- **列表式界面**:左侧是分类标签 + 搜索框 + 物品网格,右侧显示所选配方的所需材料与制作按钮。
  - 参照原版配方书:可合成的物品显示为亮色槽位,材料不足显示为暗色槽位。
  - 搜索框支持**拼音检索**(全拼与首字母,如 `zuanshijian` / `zsj` 均可搜到钻石剑)。
  - 可切换「显示全部 / 仅显示可合成」。
  - 物品排序:先按**稀有度**,再按**创造模式物品栏顺序**;右侧材料列表同理。
- **数据驱动的分类**:左侧分类标签完全由数据包定义,整合包可自由增删改(见 [3.2](#32-分类定义))。
- **可选地移除原版配方**:被锻造台取代的原版工作台/锻造台配方可由配置统一移除,默认开启(见 [2](#2-配置))。
- **背包满不丢物品**:制作时背包放不下的产物会掉落在玩家脚下。

### 战利品袋

模组另外提供一个独立物品——**战利品袋**(`taverntales_4ging:loot_bag`),右键即开,内容完全由数据包的战利品表定义(见 [4](#4-战利品袋))。

- **一袋一表**:袋子的种类写在物品组件里,对应一张同名的战利品表,可以给每个 Boss 配一个专属袋子。
- **掉落物形态受保护**:免疫火焰/岩浆/爆炸,永不消失,并常亮发光轮廓(隔墙可见),适合直接扔在战场上。
- **内容不经网络同步**:战利品表是纯服务端的,游戏内没有任何界面能查到袋子会掉什么(JEI 里也查不到)。但这**不等于保密**——表若随 mod jar 或整合包发到了玩家机器上,解压翻文件即可看到;真想藏,得把表放在服务端独有的数据包里(如服务器的 `world/datapacks/`)。
- 没有合成配方,也不在创造物品栏里,只能通过 `/give`、命令或战利品表给予。

### 获取方式

装备锻造台可在**原版工作台**中合成:

```
铁 铁 铁          铁 = 铁锭
金 台 金          金 = 金锭
金    金          台 = 工作台
```

### 依赖

| 模组 | 类型 | 说明 |
| --- | --- | --- |
| NeoForge `21.1.234+` | **必需** | — |
| Minecraft `1.21.1` | **必需** | — |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) `19+` | 可选(客户端) | 提供「装备锻造」配方查询分类;在锻造台界面点击 JEI 的「+」可直接跳转选中该配方 |
| [Beyond Dimensions](https://modrinth.com/mod/beyonddimensions) `0.7+`(超越维度) | 可选 | 锻造时可直接消耗玩家**主网络**中存储的物品,界面的可合成判定与材料计数也会计入 |

> 拼音检索所需的 pinyin4j 已通过 jarJar 打包进模组,无需额外安装。

---

## 2. 配置

配置类型为 **COMMON**,文件位于 `config/taverntales_4ging-common.toml`。

```toml
#移除被装备锻造台取代的原版工作台合成配方,使这些物品只能通过装备锻造台制作。
removeVanillaRecipes = true
#加载模组自带的默认分类标签(近战/远程/魔法/工具/盔甲/盾牌/饰品)。
enableDefaultCategories = true
#加载模组自带的默认锻造配方(木/石/铁/金/钻石/下界合金装备、弓弩、盾牌等)。
enableDefaultRecipes = true
```

| 选项 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `removeVanillaRecipes` | boolean | `true` | 是否移除被锻造台取代的原版配方。一个开关统一控制全部。 |
| `enableDefaultCategories` | boolean | `true` | 是否加载模组自带的 7 个默认分类标签。关闭后可由数据包完全自定义分类。 |
| `enableDefaultRecipes` | boolean | `true` | 是否加载模组自带的默认锻造配方。关闭后可由数据包完全自定义配方表。 |

**三者的共同点:**

- 都**只在服务端生效**。它们作用于数据包加载阶段(服务端),结果随后同步给客户端。
  - 单人游戏:本地配置文件即生效(单人客户端自带集成服务端)。
  - 多人游戏:**只有服务器的配置文件生效**,客户端改本地文件无效。
- 修改后需要 `/reload` 或重进世界才会生效。

**`removeVanillaRecipes`**

- 开启时,以下原版配方会被移除:木/石/铁/金/钻石各阶的工具与剑、盾牌、弓、弩、重锤、皮革/铁/金/钻石套装、海龟壳,以及**下界合金装备的锻造台升级(smithing)配方**。
- 关闭时,以上原版配方全部恢复,锻造台配方仍然可用(即两种途径并存)。

**`enableDefaultCategories`**

- 关闭后,`taverntales_4ging:melee` 等 7 个内置分类不再加载,左侧只剩内置的「全部」标签,可由数据包自行定义分类。
- 注意:若同时保留了默认配方,那些配方引用的分类将不存在,于是**只会出现在「全部」标签中**。通常与 `enableDefaultRecipes = false` 搭配使用。

**`enableDefaultRecipes`**

- 关闭后,模组自带的 61 个锻造配方不再加载,可由数据包自行定义整套配方表。
- **不影响装备锻造台方块自身的合成配方**(该方块始终可合成)。
- 也不影响 `removeVanillaRecipes`——如需同时恢复原版配方,请另行关闭它。

---

## 3. 数据格式

模组共涉及三类数据文件,整合包可通过数据包直接覆盖或新增。

### 3.1 锻造配方

- **位置**:`data/<命名空间>/recipe/**/*.json`(可放在任意子目录;本模组放在 `recipe/equipment_forge/` 下)
- **类型**:`taverntales_4ging:equipment_forge`

```json
{
  "type": "taverntales_4ging:equipment_forge",
  "category": "taverntales_4ging:melee",
  "materials": [
    { "tag": "minecraft:planks", "count": 2 },
    { "item": "minecraft:diamond", "count": 3 }
  ],
  "result": { "id": "minecraft:diamond_sword", "count": 1 }
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `category` | 是 | 分类 id,**必须写完整命名空间**(如 `taverntales_4ging:melee`)。引用了未定义的分类时,该配方只会出现在「全部」标签中,并打印一条调试日志。 |
| `materials` | 是 | 材料列表。每项为 `{ "item": <物品id> }` 或 `{ "tag": <标签id> }`,配合 `count`(默认 1)。数量指**所需总数**,与摆放形状无关。 |
| `result` | 是 | 产物,`{ "id": <物品id>, "count": <数量> }`。 |

> 材料按列表顺序无关紧要——界面会自动按稀有度、创造物品栏顺序重新排序显示。
> 标签类材料在界面上会每秒轮换展示其可选物品。

### 3.2 分类定义

左侧分类标签由数据包注册表驱动。

- **注册表**:`minecraft:category`
- **位置**:`data/<命名空间>/category/*.json`
- **分类 id**:即该文件的 ResourceLocation。例如 `data/taverntales_4ging/category/melee.json` → `taverntales_4ging:melee`,配方通过这个 id 归类。

```json
{
  "name": { "translate": "category.taverntales_4ging.melee" },
  "order": 0,
  "icon": "minecraft:iron_sword"
}
```

| 字段 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `name` | 是 | — | 标签显示名,完整的 Component(通常用 `{"translate": "..."}`)。 |
| `order` | 否 | `0` | 从上到下的顺序,越小越靠上。相同则按分类 id 字母序。 |
| `icon` | 否 | `minecraft:book` | 标签图标物品 id。可以指向可选模组的物品——该物品不存在时会回退(见下)。 |

**图标回退**:若 `icon` 指定的物品在注册表中不存在(如未安装对应模组),内置分类会回退到各自的原版图标(魔法武器 → 知识之书,饰品 → 鞘翅,其余 → 与自身图标相同);其它分类一律回退为书。

**「全部」标签是内置的**,永远置顶且不可通过数据包配置。

内置分类:

| id | order | 图标   |
| --- | --- |------|
| `taverntales_4ging:melee` | 0 | 铁剑   |
| `taverntales_4ging:ranged` | 1 | 弓    |
| `taverntales_4ging:magic` | 2 | 知识之书 |
| `taverntales_4ging:tool` | 3 | 钻石镐  |
| `taverntales_4ging:armor` | 4 | 黄金胸甲 |
| `taverntales_4ging:shield` | 5 | 盾牌   |
| `taverntales_4ging:curio` | 6 | 鞘翅   |

### 3.3 数据包条件

模组注册了三个数据包条件,分别对应三个配置项。NeoForge 的规则是「条件为 `true` 时该条数据才加载」,所以条件名描述的都是"**加载**"这一侧的状态:

| 条件 id | 返回 `true` 的时机 | 用在哪 |
| --- | --- | --- |
| `taverntales_4ging:vanilla_recipe_enabled` | `removeVanillaRecipes = false` 时 | 模组对原版配方的覆盖文件 |
| `taverntales_4ging:default_recipes_enabled` | `enableDefaultRecipes = true` 时 | 模组自带的 61 个锻造配方 |
| `taverntales_4ging:default_categories_enabled` | `enableDefaultCategories = true` 时 | 模组自带的 7 个分类定义 |

> 条件不仅能用在配方上——NeoForge 给数据包注册表也接上了条件解析,所以分类定义(3.2)同样支持 `neoforge:conditions`。

#### 用条件移除原版配方

`vanilla_recipe_enabled` 的语义是:当配置 `removeVanillaRecipes` 为 `false`(即**不移除**)时返回 `true`。

由于 NeoForge 的规则是「条件为 true 时配方才加载」,把它挂在原版配方文件上,即可实现"配置开启时原版配方不加载"。做法是**用与原版内容一致的文件覆盖原版配方**,并在顶部加上条件:

```json
{
  "neoforge:conditions": [
    { "type": "taverntales_4ging:vanilla_recipe_enabled" }
  ],
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "key": {
    "#": { "item": "minecraft:stick" },
    "X": { "tag": "minecraft:planks" }
  },
  "pattern": ["X", "X", "#"],
  "result": { "count": 1, "id": "minecraft:wooden_sword" }
}
```

放到 `data/minecraft/recipe/wooden_sword.json` 即可覆盖原版木剑配方。该条件对任意配方类型都适用(本模组也用它覆盖了下界合金的 `smithing_transform` 配方)。

> 新增锻造配方时,若希望同时移除对应的原版配方,照此覆盖一份即可复用同一个配置开关。
> 原版配方的原文可从 `neoforge-<版本>-client-extra-*.jar` 的 `data/minecraft/recipe/` 中提取。

---

## 4. 战利品袋

战利品袋(`taverntales_4ging:loot_bag`)是一个"右键即开"的物品。它自己不含任何掉落内容——**袋子只是个指针**,指向一张原版战利品表,掉什么全由那张表说了算。

### 4.1 三个组成部分

一个能用的袋子由三样东西凑成,缺一不可:

| # | 东西 | 位置 | 例(铁傀儡袋) |
| --- | --- | --- | --- |
| 1 | **物品组件**,决定这是哪种袋子 | 物品自身 | `taverntales_4ging:loot_bag_type` = `"iron_golem"` |
| 2 | **战利品表**,决定掉什么 | 数据包 | `data/taverntales_4ging/loot_table/loot_bag/iron_golem.json` |
| 3 | **译文**,决定 tooltip 显示什么名字、什么颜色 | 资源包/模组语言文件 | `loot_bag.taverntales_4ging.iron_golem` = `"§f铁傀儡"` |

三者靠组件里那个**裸字符串**串起来。组件值 `"iron_golem"` 会被**固定**拼成:

- 战利品表 id → `taverntales_4ging:loot_bag/iron_golem`
- 翻译键 → `loot_bag.taverntales_4ging.iron_golem`

> 组件值**不能带命名空间,也不能带目录**(不能写 `mypack:xxx` 或 `boss/xxx`)。所有袋子的表都必须放在 `taverntales_4ging` 命名空间的 `loot_bag/` 目录下——但**任何数据包都可以往这个路径里塞文件**,不需要是本模组的 jar。

### 4.2 手把手:新建一个"末影龙袋"

**第一步**,建表文件 `data/taverntales_4ging/loot_table/loot_bag/ender_dragon.json`:

```json
{
  "type": "taverntales_4ging:loot_bag",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:dragon_egg" }
      ]
    }
  ]
}
```

**第二步**,在语言文件 `assets/<任意命名空间>/lang/zh_cn.json` 里加译文(颜色用 § 格式码写在译文开头):

```json
{
  "loot_bag.taverntales_4ging.ender_dragon": "§5末影龙"
}
```

**第三步**,给予:

```
/give @s taverntales_4ging:loot_bag[taverntales_4ging:loot_bag_type="ender_dragon"] 1
```

改完数据包后 `/reload` 即可生效;新增译文需要重进世界或按 F3+T 重载资源。

### 4.3 组件与显示

| 组件 | 类型 | 说明 |
| --- | --- | --- |
| `taverntales_4ging:loot_bag_type` | 字符串 | 袋子种类。**这是袋子唯一的组件**。不写、写空串、或没有该组件 → 空袋:不可右键,tooltip 显示"空"。 |

**显示名与颜色一律来自语言文件**,没有任何自动推导:

- 查得到译文 → 原样显示,颜色由译文里的 `§` 格式码决定(不写颜色码就继承默认色)。
- **查不到译文 → tooltip 显示红色的组件原文**(如红色的 `ender_dragon`)。这是故意的报错提示,提醒你漏配了译文或者 type 拼错了。

> 因为颜色走 `§` 码,**只能用原版的 16 种颜色**,不支持 `#RRGGBB`。且每种语言的译文都要各自带上颜色码。
>
> "空"这个提示用的键是 `loot_bag.taverntales_4ging.taverntales_4ging_null`——之所以不叫 `.empty`,是为了给一个名字真叫 `empty` 的袋子让路。

### 4.4 掉落物形态的特性

袋子作为掉落物(ItemEntity)躺在地上时:

| 特性 | 效果 |
| --- | --- |
| 防火 | 火焰、岩浆烧不掉 |
| 免伤 | 爆炸、仙人掌等一切伤害都无效 |
| 不消失 | 永远不会因为超时而消失 |
| 发光 | 常亮发光轮廓,隔墙可见 |

> **唯一的例外是虚空**:掉出世界底部的物品会被直接删除,这条路径绕过了所有伤害判定,拦不住。

---

## 5. 战利品表详解

本章是袋子里 `pools` 的手把手用法。这就是**原版战利品表**,和箱子、怪物掉落用的是同一套格式——已经熟悉原版战利品表的话,只需直接跳到 [5.7 上下文参数与限制](#57-上下文参数与限制),那里是唯一的不同之处。

### 5.1 表的骨架

```json
{
  "type": "taverntales_4ging:loot_bag",
  "pools": [ ... ],
  "functions": [ ... ],
  "random_sequence": "taverntales_4ging:loot_bag/iron_golem"
}
```

| 字段 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `type` | 否 | `minecraft:generic` | 参数集。**袋子的表必须写 `taverntales_4ging:loot_bag`**,原因见 [5.7](#57-上下文参数与限制)。 |
| `pools` | 否 | `[]` | 池列表。不写就是个空表(袋子能开,但什么都不给)。 |
| `functions` | 否 | `[]` | **表级函数**,作用于本表掉出的**每一件**物品。 |
| `random_sequence` | 否 | — | 随机序列 id,用于让掉落结果在同一存档内可复现。一般填表自己的 id 即可,不填也无妨。 |

> `type` 写错了**不会报错**,详见 [5.7](#57-上下文参数与限制) 末尾的警告。

### 5.2 核心概念:池 = 一次独立的抽奖

这是整个战利品表最容易误解的地方,先记住三句话:

1. **每个 pool 相互独立**,依次结算,结果**累加**。
2. **一个 pool 的一次 roll,只产出一个条目**——`entries` 是"候选池",不是"清单"。
3. 想让袋子"必给 A,再随机给 B",就写**两个 pool**,而不是把 A、B 塞进同一个 pool 的 entries。

铁傀儡袋正是这么写的:第一个 pool 只有铁锭一个候选(所以必给),第二个 pool 在虞美人和铁块之间按权重二选一。

```
pool[0]: rolls=1, entries=[铁锭]              → 必定给 3-5 个铁锭
pool[1]: rolls=1, entries=[虞美人(8), 铁块(1)] → 8/9 概率给虞美人, 1/9 概率给铁块
                                                 合计:每次开袋恰好 2 件物品
```

### 5.3 pool 的字段

| 字段 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `entries` | **是** | — | 候选条目列表。 |
| `rolls` | **是** | — | 抽取次数。可以是数字,也可以是数值提供器(见 [5.4](#54-rolls-抽几次))。 |
| `bonus_rolls` | 否 | `0` | 幸运加成的额外抽取次数,会乘以玩家幸运值。 |
| `conditions` | 否 | `[]` | **整个池**的开关。条件不通过则本池完全跳过。 |
| `functions` | 否 | `[]` | 作用于**本池**掉出的每件物品。 |

实际抽取次数的公式是:

```
次数 = rolls + floor(bonus_rolls × 玩家幸运值)
```

幸运值来自玩家的 luck 属性(幸运药水等)。普通情况下为 0,于是 `bonus_rolls` 不起作用。

### 5.4 rolls 抽几次

直接写数字就是固定次数。要随机次数则用**数值提供器**:

```json
"rolls": 3                                                  // 固定 3 次
"rolls": { "type": "minecraft:uniform", "min": 1, "max": 3 } // 1~3 次,闭区间,均匀
"rolls": { "type": "minecraft:binomial", "n": 3, "p": 0.5 }  // 抛 3 次硬币,每次 50% 命中,结果 0~3
```

| 提供器 | 说明 |
| --- | --- |
| `minecraft:constant` | `{"type":"minecraft:constant","value":2}`,等价于直接写 `2` |
| `minecraft:uniform` | `min`~`max` 闭区间均匀随机 |
| `minecraft:binomial` | 二项分布,`n` 次尝试每次 `p` 概率。适合"大概率少、小概率多"的钟形分布 |
| `minecraft:score` | 读记分板。**本模组只能用 `"target": "this"`**(开袋玩家) |
| `minecraft:storage` | 读命令存储 |
| `minecraft:enchantment_level` | **本模组不可用**(需要附魔等级参数) |

> 同样的写法也适用于 `set_count` 等函数里的 `count` 字段。

### 5.5 entries 抽什么

| `type` | 说明 |
| --- | --- |
| `minecraft:item` | 给一件物品。`name` 填物品 id。**最常用**。 |
| `minecraft:tag` | 物品标签。`name` 填标签 id,`expand` 决定行为(见下)。 |
| `minecraft:loot_table` | 嵌套另一张表。`value` 填表 id,或直接内联一张表。适合复用公共掉落。 |
| `minecraft:empty` | 空条目,什么都不给。用来给池子掺"空奖"。 |
| `minecraft:group` | 子条目**全部**执行。 |
| `minecraft:alternatives` | 按顺序取**第一个**条件通过的子条目(类似 if / else if)。 |
| `minecraft:sequence` | 按顺序执行,直到某个子条目条件失败为止。 |
| `minecraft:dynamic` | 方块实体专用,**本模组用不了**。 |

**每个条目的通用字段:**

| 字段 | 默认 | 说明 |
| --- | --- | --- |
| `weight` | `1` | 权重。同池内被抽中的概率 = 自身 weight ÷ 池内所有条目 weight 之和。 |
| `quality` | `0` | 幸运修正。实际权重 = `max(floor(weight + quality × 幸运值), 0)`。正数表示"越幸运越容易出"。 |
| `conditions` | `[]` | 本条目的开关。不通过则该条目**不参与本次抽取**。 |
| `functions` | `[]` | 只作用于本条目产出的物品。 |

**`minecraft:tag` 的 `expand`** 值得单独说明:

- `"expand": true` → 标签里的**每个物品各自成为一个条目**,各自参与权重竞争(即"从标签里随机挑一个")。
- `"expand": false` → **一次性给出标签里的全部物品**(即"标签里的都给你")。

**用 `empty` 掺空奖**是很常用的技巧:

```json
{
  "rolls": 1,
  "entries": [
    { "type": "minecraft:item", "name": "minecraft:diamond", "weight": 1 },
    { "type": "minecraft:empty", "weight": 9 }
  ]
}
```

这个池有 10% 概率给一颗钻石,90% 什么都不给。

> 想要"90% 概率给钻石",除了用 `empty` 配权重,也可以给条目挂 `minecraft:random_chance` 条件——但两者语义不同:权重是**互相竞争**,条件是**各自独立判定**。

### 5.6 functions 给掉出来的东西加工

函数可以挂在三个层级:**表级**(每件物品)、**池级**(本池的每件物品)、**条目级**(仅该条目)。以下在本模组中都可用:

| 函数 | 用途 |
| --- | --- |
| `minecraft:set_count` | 设置数量。`count` 支持数值提供器;`"add": true` 表示在原数量上累加而非覆盖 |
| `minecraft:limit_count` | 把数量夹在 `{"min":x,"max":y}` 区间内 |
| `minecraft:set_components` | 设置任意物品组件(1.20.5+ 取代了旧的 `set_nbt`) |
| `minecraft:set_damage` | 设置耐久。注意 `damage` 是**剩余耐久比例**而非损耗比例——`0.9` 表示九成新,`0.1` 表示快断了。这是原版的反直觉设计 |
| `minecraft:enchant_randomly` | 随机附魔。可用 `options` 限定附魔池 |
| `minecraft:enchant_with_levels` | 按等级附魔,等同附魔台的效果 |
| `minecraft:set_enchantments` | 精确指定附魔与等级 |
| `minecraft:set_attributes` | 添加属性修饰符 |
| `minecraft:set_name` / `set_lore` | 设置名字/描述。**带 `entity` 字段时只能填 `"this"`** |
| `minecraft:set_potion` | 设置药水类型 |
| `minecraft:set_custom_data` | 设置自定义 NBT 数据 |
| `minecraft:furnace_smelt` | 把产物换成其熔炼结果 |
| `minecraft:exploration_map` | 生成探险地图(只需开袋位置,可用) |
| `minecraft:filtered` / `sequence` / `reference` | 组合与复用函数 |

一个"随机数量 + 随机附魔"的条目长这样:

```json
{
  "type": "minecraft:item",
  "name": "minecraft:diamond_sword",
  "functions": [
    {
      "function": "minecraft:enchant_with_levels",
      "levels": { "type": "minecraft:uniform", "min": 20, "max": 30 }
    },
    {
      "function": "minecraft:set_components",
      "components": { "minecraft:rarity": "epic" }
    }
  ]
}
```

> 注意函数字段名是 `"function"`,条件字段名是 `"condition"`,而条目字段名是 `"type"`——三者不一样,这是最常见的手滑点。

### 5.7 上下文参数与限制

这是袋子的表**唯一**区别于普通战利品表的地方,也是最需要注意的一节。

战利品表运行时能读到的信息由**参数集**(即表的 `type` 字段)决定。箱子表能读到箱子位置,怪物掉落表能读到凶手是谁、用的什么武器。袋子是玩家右键打开的,所以模组注册了一个专属参数集 `taverntales_4ging:loot_bag`,它只提供两个参数:

| 参数 | 内容 |
| --- | --- |
| `minecraft:origin` | **开袋位置**(玩家当时的坐标) |
| `minecraft:this_entity` | **开袋的玩家** |

其它参数(凶手、武器、方块、伤害来源、爆炸半径……)**一概没有**,因为开袋这个场景里根本不存在这些东西。

**用了读不到的参数会怎样?** 不会崩,但会**静默失效**——这正是它危险的地方:

| 情形 | 后果 |
| --- | --- |
| 条件读不到参数 | 条件恒为 **false** → 该条目**永远不掉**,或该池永远跳过 |
| 函数读不到参数 | 函数变成**空操作** → 悄悄地什么都没做 |
| 加载时 | 日志里有一条 **WARN**:`Parameters [...] are not provided in this context`。**表照常加载**,不会作废 |

> 只有 `minecraft:enchantment_active_check` 是例外,它会真的抛异常。
>
> **所以:袋子不掉东西、或者函数不生效时,第一件事是去服务端启动日志里搜 `are not provided in this context`。**

**不可用清单**——以下条件/函数依赖袋子拿不到的参数,写了等于没写:

| 条件 | 它需要的参数 |
| --- | --- |
| `minecraft:killed_by_player` | 最后伤害玩家 |
| `minecraft:damage_source_properties` | 伤害来源 |
| `minecraft:match_tool` | 工具 |
| `minecraft:table_bonus` | 工具 |
| `minecraft:block_state_property` | 方块状态 |
| `minecraft:survives_explosion` | 爆炸半径 |
| `minecraft:random_chance_with_enchanted_bonus` | 攻击者 |
| `minecraft:enchantment_active_check` | 附魔激活状态(**这个会抛异常**) |

| 函数 | 它需要的参数 |
| --- | --- |
| `minecraft:apply_bonus` | 工具(时运加成,袋子里没意义) |
| `minecraft:enchanted_count_increase` | 攻击者(抢夺加成,袋子里没意义) |
| `minecraft:copy_state` | 方块状态 |
| `minecraft:copy_components` | 方块实体 |
| `minecraft:explosion_decay` | 爆炸半径 |

**半可用**——凡是带实体指向的地方,**只能指向 `this`(开袋玩家)**,填 `attacker` / `attacking_player` / `direct_attacker` 都会静默失效:

| 条件/函数 | 可用写法 |
| --- | --- |
| `minecraft:entity_properties` | `"entity": "this"` |
| `minecraft:entity_scores` | `"entity": "this"` |
| `minecraft:copy_name` | `"source": "this"` |
| `minecraft:set_name` / `set_lore` | `"entity": "this"`,或干脆不写 `entity` |
| `minecraft:fill_player_head` | `"entity": "this"` |
| `minecraft:score`(数值提供器) | `"target": "this"` |

**放心用的**(不依赖任何参数,或只需开袋位置/开袋玩家):

`minecraft:random_chance`、`minecraft:value_check`、`minecraft:time_check`、`minecraft:weather_check`、`minecraft:location_check`(检查开袋位置)、`minecraft:inverted`、`minecraft:any_of`、`minecraft:all_of`、`minecraft:reference`,以及 [5.6](#56-functions-给掉出来的东西加工) 表格里列出的全部函数。

> ⚠️ **`type` 写错不会报错。** 如果把 `"type"` 拼成了 `taverntales_4ging:lootbag` 之类,原版**不会**报任何错,而是静默退回 `minecraft:generic` 参数集——那个集合声称"什么参数都有",于是上面的 WARN 也不会出现,袋子却可能在开的瞬间抛异常。**表不掉东西时,先检查 `type` 有没有拼对。**

### 5.8 完整示例:铁傀儡袋

把前面几节拼起来,一个"铁傀儡袋"的完整表 `data/taverntales_4ging/loot_table/loot_bag/iron_golem.json` 长这样:

```json
{
  "type": "taverntales_4ging:loot_bag",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:iron_ingot",
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "type": "minecraft:uniform", "min": 3, "max": 5 }
            }
          ]
        }
      ]
    },
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:poppy",
          "weight": 8,
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "type": "minecraft:uniform", "min": 1, "max": 2 }
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:iron_block",
          "weight": 1
        }
      ]
    }
  ]
}
```

逐段拆解:

- **第一个池**:`rolls: 1`、只有一个候选 → **必定**掉落,`set_count` 把数量随机成 3~5 个铁锭。
- **第二个池**:`rolls: 1`、两个候选,权重 8 : 1 → 8/9 概率掉 1~2 朵虞美人,1/9 概率掉 1 个铁块。
- **合计**:每次开袋**恰好 2 件**物品(每个池各出 1 件)。

配套的译文(`§f` = 白色):

```json
"loot_bag.taverntales_4ging.iron_golem": "§f铁傀儡"
```

给予指令:

```
/give @s taverntales_4ging:loot_bag[taverntales_4ging:loot_bag_type="iron_golem"] 1
```

> 想在正式加袋子前先验证一张表掉什么,不必反复开袋——用原版命令直接掷它:
> ```
> /loot spawn ~ ~ ~ loot taverntales_4ging:loot_bag/iron_golem
> ```

### 5.9 自带的测试袋

上面的铁傀儡袋只是文档示例,**模组本体不带任何成品袋子**——袋子该有哪些、掉什么,完全交给整合包决定。

模组只自带一个**测试袋**,用来验证功能是否正常,同时也是一份"活的示例":

```
/give @s taverntales_4ging:loot_bag[taverntales_4ging:loot_bag_type="taverntales_4ging_test"] 1
```

它的表 `data/taverntales_4ging/loot_table/loot_bag/taverntales_4ging_test.json` 刻意把本章讲到的东西几乎全用了一遍,每个池对应一个知识点:

| 池 | 演示的东西 |
| --- | --- |
| 1 | 单候选必掉 + `set_count` 用 `uniform` |
| 2 | `weight` 权重竞争 + `quality` 幸运修正 |
| 3 | `empty` 掺空奖(10% 出钻石) |
| 4 | `tag` 条目 + `expand: true` |
| 5 | `alternatives` + `time_check`(夜里给火把,白天给向日葵) |
| 6 | `group` 一次给多件 |
| 7 | `sequence` 顺序执行至条件失败 |
| 8 | `binomial` 抽取次数 + `bonus_rolls` |
| 9 | `loot_table` 条目内联嵌套表 |
| 10 | `enchant_with_levels` + `set_damage` + `set_attributes` + `set_name` |
| 11 | `enchant_randomly` 限定附魔池 + `set_components` |
| 12 | 池级 `conditions` + `entity_properties`(开袋玩家) |
| 13 | `any_of` / `inverted` 组合条件 + `weather_check` |
| 14 | `location_check` + `furnace_smelt` + `limit_count` |
| 15 | `set_potion` + 条目级 `set_lore` |

另外它在**表级**挂了一个 `set_lore`,给掉出的每一件物品都追加一行灰色的"测试袋产出"——这既演示了表级函数的作用范围,也方便你一眼认出哪些东西是这个袋子给的。

正常情况下开一次约掉 12~13 件物品。如果数量明显不对,或者启动日志里出现了 `are not provided in this context`,说明有东西坏了。
