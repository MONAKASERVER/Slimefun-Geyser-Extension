# Slimefun Geyser Extension

Slimefunのlegacy `CustomModelData` アイテムを、Geyser経由のBedrock Editionクライアントへ専用アイテムとして登録するGeyser Extensionです。

このforkはGeyser 2.11.3のCustom Item API v2へ移行しており、Paper/Bukkit APIやバックエンド側のSlimefunプラグインへ直接依存しません。

## 対応環境

- Geyser-BungeeCord `2.11.3-SNAPSHOT`（2.11.3-b1245でAPI確認）
- Floodgate 2.2.5系
- BungeeCord
- Paper 1.21.11バックエンド
- Java 21
- [Slimefun-Geyser-RP](https://github.com/SofiaRedmond/Slimefun-Geyser-RP)

Geyser API dependencyと`extension.yml`の要求APIは、どちらも`2.11.3`へ統一されています。

## 変更点

- deprecatedな`CustomItemData` / `CustomItemOptions` / `register(String, CustomItemData)`を廃止
- `CustomItemDefinition`、`Identifier`、`ItemRangeDispatchPredicate.legacyCustomModelData(...)`へ移行
- 全561定義を型付きRegistryから一括検証して登録
- 通常アイテムのBedrock identifierを`slimefun:<item_name>`へ移行
- 既存RPのattachableが直接参照する防具16件は`geyser_custom:<item_name>`を維持
- RPの既存texture keyを`icon`として明示し、テクスチャ名との互換性を維持
- 旧format-v1 JSONとExtensionの二重登録を防止
- 重複・欠落を検出するJUnitテストを追加

## インストール

1. BungeeCordを停止します。
2. 古いSlimefun Geyser Extension JARを削除します。
3. `plugins/Geyser-BungeeCord/custom_mappings/` に旧Slimefun用JSONがある場合は削除します。
4. Extension JARとResource Packを次の位置へ配置します。

```text
plugins/
└─ Geyser-BungeeCord/
   ├─ extensions/
   │  └─ Slimefun-Geyser-Extension-1.4.0-geyser-2.11.3.jar
   └─ packs/
      └─ Slimefun-Geyser-RP.mcpack
```

5. Geyserの`config.yml`でcustom contentを有効にします。

```yaml
gameplay:
  enable-custom-content: true
```

6. BungeeCordを起動します。`/geyser reload`ではなく完全再起動を推奨します。

`add-non-bedrock-items`は旧設定名です。Geyser 2.11.3では`gameplay.enable-custom-content`を使用します。

## Resource Pack

対応RP:

https://github.com/SofiaRedmond/Slimefun-Geyser-RP

RPはGeyserが読み込める`.zip`または`.mcpack`形式で`packs`へ配置してください。GeyserはJava版RPを自動変換しません。

ExtensionのBedrock identifierとtexture iconは分離されています。例えば`carbon`は次のように登録されます。

```text
Java item:          minecraft:player_head
CustomModelData:    2200113
Bedrock identifier: slimefun:carbon
Texture icon:       carbon
```

通常545件は`slimefun:` namespaceを使用します。既存RPのattachableとrender controllerがidentifierを直接参照する防具16件のみ、表示互換性のため`geyser_custom:`を維持します。identifierはRegistry内で明示管理され、全561件で一意性を検査します。

旧`.textureSize(32)` APIは使用しません。RP内の16x16/32x32 PNG自体は変更・縮小せず、そのままBedrockのitem textureとして使用します。装備用のサイズ・表示調整はRP側の`attachables/`を維持します。

RP監査結果:

- Extension定義: 561
- RP custom texture key: 559
- `wiki`は既存の`slimefun_guide`へ割り当て
- `ui_background_2`は既存の`background`へ割り当て
- `copper_ingot`はBedrock標準atlas keyを使用
- 既存attachable: 16

## 起動ログ

正常時は実際の件数から次のようなログが出ます。

```text
[SlimefunGeyser] Geyser API: 2.11.3
[SlimefunGeyser] Preparing 561 custom Slimefun items...
[SlimefunGeyser] Duplicate definitions: 0
[SlimefunGeyser] Registered 561/561 custom Slimefun items.
[SlimefunGeyser] Failed definitions: 0
```

## トラブルシューティング

### `conflicts with another custom item definition`

同じSlimefun定義が`custom_mappings`とExtensionの両方から読み込まれている可能性があります。`plugins/Geyser-BungeeCord/custom_mappings/`から旧Slimefun JSONを削除し、Extension JARだけを使用してください。

Extensionは同じJava item + CustomModelData + iconの既存定義を検出した場合、二重登録せず既存定義を再利用します。異なる内容の衝突は部分登録せず、対象名・Java item・CustomModelData・Bedrock identifierをログへ出して停止します。

### テクスチャが紫黒またはバニラ表示になる

- RPが`plugins/Geyser-BungeeCord/packs/`へ配置されているか確認
- `gameplay.enable-custom-content: true`を確認
- Bedrockクライアント側でサーバーRPを再ダウンロード
- 起動ログの`Failed definitions`と競合ログを確認

### Base API 1.0.0の警告

古いJARが読み込まれています。このforkのJARへ置き換え、`extensions/`に同名・旧版JARが残っていないか確認してください。

## ビルド

Java 21で実行します。

```bash
./gradlew clean build
```

成果物:

```text
build/libs/Slimefun-Geyser-Extension-1.4.0-geyser-2.11.3.jar
```

push / pull request時にもGitHub Actionsが同じビルドとテストを実行し、JARをartifactとして保存します。

## データ保全

旧188KBの単一Javaファイルに存在した561件を機械抽出し、`src/main/resources/slimefun-items.tsv`へ移行しています。テストでは次を固定検証します。

- 定義数: 561
- 16pxメタデータ: 438
- 32pxメタデータ: 123
- item nameの一意性
- `slimefun:<item_name>`の一意性
- Java item + CustomModelDataの一意性
- 全定義のcanonical SHA-256

## Upstream

- Extension: https://github.com/SofiaRedmond/Slimefun-Geyser-Extension
- Resource Pack: https://github.com/SofiaRedmond/Slimefun-Geyser-RP
- Geyser: https://github.com/GeyserMC/Geyser
- Geyser custom items documentation: https://geysermc.org/wiki/geyser/custom-items/
