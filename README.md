# EssentialsCore

EssentialsCore 是為 Paper 26.2 製作的輕量、GUI 優先生存便利插件。

## 需求

- Paper 26.2
- Java 25
- PlaceholderAPI（選裝，只在需要 Rank Placeholder 時安裝）
- TAB（選裝，可搭配 PlaceholderAPI 顯示 Rank 前綴）

## 建置

Windows PowerShell：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
.\gradlew.bat clean build
```

成品位於 `build/libs/EssentialsCore-1.3.0.jar`。將 JAR 放進 Paper
伺服器的 `plugins` 資料夾並重新啟動伺服器。

## 玩家指令

- `/ec`：主選單
- `/home`、`/sethome`：Home GUI 與建立 Home
- `/tpa`、`/tpahere`：玩家選擇 GUI
- `/tpaccept [玩家]`、`/tpdeny [玩家]`：處理傳送請求
- `/pet`：寵物召回 GUI
- `/sit`：開啟／關閉右鍵階梯與單層半磚坐下模式
- `/lay`：在目前位置躺下；再次輸入、按 Shift、移動或受傷時起身
- `/hat`：將主手物品戴到頭部；之後可直接從頭盔欄位取下
- `/back`：返回最近一次死亡的位置（死亡點會持久保存）
- `/warp [名稱]`：開啟公共 Warp GUI 或直接傳送
- `/trash`：開啟安全垃圾桶；關閉會退回物品，只有確認按鈕會永久刪除
- `/ec help`：開啟雙語指令說明 GUI
- `/rank`：查看自己的 Rank

Rank 前綴也會顯示在一般聊天玩家名稱前及玩家頭頂 nametag。若伺服器另有會管理
scoreboard team 的 nametag 插件，該插件可能覆蓋 EssentialsCore 的頭頂前綴。

玩家先輸入 `/sit` 開啟模式，再以主手、非潛行方式右鍵階梯或單層半磚即可坐下，
按 Shift 起身；再次輸入 `/sit` 關閉模式。同一座位一次只能有一位玩家，潛行右鍵
仍可正常放置或操作方塊。寵物 GUI 另提供全部坐下、全部起身及召回全部寵物。

## OP 管理

- `/ec admin`：Home、TPA、Pet Protection、Warp 與趣味實驗功能設定 GUI
- 趣味與實驗頁：切換萬用拴繩及空手 Shift 右鍵寵物的愛心互動
- Warp 管理 GUI：建立、改名、更新位置、修改圖示、確認刪除及傳送參數
- Home 設定頁可即時調整每位玩家的 Home 數量上限（1–100）
- `/rank create <id> <顯示名稱>`
- `/rank edit <id> name|prefix|color <值>`
- `/rank set <玩家> <id>`
- `/rank default <id>`
- `/rank delete <id> confirm`
- `/rank list`
- `/rank info [玩家]`

## PlaceholderAPI

- `%essentialscore_rank%`
- `%essentialscore_rank_prefix%`

TAB 可使用 `%essentialscore_rank_prefix%%player%`。

OP 每次加入伺服器時會看到 EssentialsCore 正式版版本、作者與管理入口資訊；一般玩家
不會看到這則管理提示。

## 資料檔

- `config.yml`：語言、Home、TPA、Pet Protection 與實驗功能設定
- `data.yml`：Home、玩家 Rank、寵物索引
- `ranks.yml`：Rank 定義與預設 Rank
- `warps.yml`：公共 Warp 定義、位置與 GUI 圖示
- `lang/zh_TW.yml`、`lang/en.yml`：雙語訊息

RTP 不在第一版功能範圍內，插件不會註冊 `/rtp`。
