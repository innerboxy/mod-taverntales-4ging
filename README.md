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
```

| 选项 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `removeVanillaRecipes` | boolean | `true` | 是否移除被锻造台取代的原版配方。一个开关统一控制全部。 |

**注意事项:**

- 该开关**只在服务端生效**。配方的移除发生在数据包加载阶段(服务端),配方表随后同步给客户端。
  - 单人游戏:本地配置文件即生效(单人客户端自带集成服务端)。
  - 多人游戏:**只有服务器的配置文件生效**,客户端改本地文件无效。
- 修改后需要 `/reload` 或重进世界才会生效。
- 开启时,以下原版配方会被移除:木/石/铁/金/钻石各阶的工具与剑、盾牌、弓、弩、重锤、皮革/铁/金/钻石套装、海龟壳,以及**下界合金装备的锻造台升级(smithing)配方**。
- 关闭时,以上原版配方全部恢复,锻造台配方仍然可用(即两种途径并存)。

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
  "name": { "translate": "taverntales_4ging.category.melee" },
  "order": 0,
  "icon": "minecraft:iron_sword"
}
```

| 字段 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `name` | 是 | — | 标签显示名,完整的 Component(通常用 `{"translate": "..."}`)。 |
| `order` | 否 | `0` | 从上到下的顺序,越小越靠上。相同则按分类 id 字母序。 |
| `icon` | 否 | `minecraft:book` | 标签图标物品 id。可以指向可选模组的物品——该物品不存在时会回退(见下)。 |

**图标回退**:若 `icon` 指定的物品在注册表中不存在(如未安装对应模组),内置分类会回退到各自的原版图标(魔法武器 → 书,饰品 → 鞘翅,其余 → 与自身图标相同);其它分类一律回退为书。

**「全部」标签是内置的**,永远置顶且不可通过数据包配置。

内置分类:

| id | order | 图标   |
| --- | --- |------|
| `taverntales_4ging:melee` | 0 | 铁剑   |
| `taverntales_4ging:ranged` | 1 | 弓    |
| `taverntales_4ging:magic` | 2 | 书    |
| `taverntales_4ging:tool` | 3 | 钻石镐  |
| `taverntales_4ging:armor` | 4 | 黄金胸甲 |
| `taverntales_4ging:shield` | 5 | 盾牌   |
| `taverntales_4ging:curio` | 6 | 鞘翅   |

### 3.3 移除原版配方(配方条件)

模组注册了一个数据包配方条件:

- **条件 id**:`taverntales_4ging:vanilla_recipe_enabled`
- **语义**:当配置 `removeVanillaRecipes` 为 `false`(即**不移除**)时返回 `true`。

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
