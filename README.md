# 这是作弊

适用于 Minecraft 1.21.1 的 Fabric 模组，加入最高 6 级的“区块挖掘”附魔。

## 运行要求

- Minecraft 1.21.1
- Fabric Loader 0.15.11 或更高版本
- Fabric API 0.102.1+1.21.1
- Java 21

手持带有“区块挖掘”的镐子，潜行挖掘适合镐子的方块即可触发。等级 1–6 对应 1×1 至 11×11 区块范围。创造模式默认不能触发。

## 规则命令

```text
/zuobi
/zuobi creativeChunkMining true|false
/zuobi setDefault creativeChunkMining true|false
/zuobi removeDefault creativeChunkMining
```

查询命令对所有玩家开放，修改命令要求 OP 权限等级 2。

## 构建

```powershell
.\gradlew.bat build
```

已验证的可安装 JAR 和 SHA-256 构建清单位于 `mod-builds/`。

