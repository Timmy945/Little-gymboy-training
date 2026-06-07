# FitQuest－遊戲化健身養成系統

FitQuest 是使用 Java Swing 製作的桌面健身紀錄與角色養成系統。使用者可以記錄有氧與阻力訓練，系統會依運動內容計算 XP、更新角色等級與肌群成長，並以正面／背面角色、趨勢圖表、日曆及歷史紀錄呈現訓練成果。

核心特色：

- 紀錄有氧與阻力訓練，內建 70 種運動項目。
- 依運動時間、重量、次數、組數與個人資料計算 XP。
- 顯示角色等級、稱號、目前 XP 與升級需求。
- 依胸、背、手臂、腿、腹部的訓練資料更新部位成長。
- 顯示週、月、年訓練趨勢；週模式以每日柱狀呈現。
- 首頁顯示最近 5 筆訓練紀錄，完整歷史資料持續保存在 CSV。
- 提供日曆、運動提醒、個人資料、成就牆與資料清空功能。

## 目前主要功能

- **新增／紀錄運動**：可搜尋及分類選擇運動，輸入重量、次數／分鐘、組數，勾選後一次送出多筆紀錄。
- **自訂訓練菜單**：訓練菜單會保存於 `data/custom_menu.properties`。
- **角色資訊**：首頁顯示玩家名稱、稱號、角色等級及 XP 進度條。
- **正面／背面角色**：依玩家總等級及各肌群等級組合不同部位圖片。
- **部位成長**：阻力訓練會依運動對應肌群增加部位數值；有氧運動目前只增加 XP，不增加部位數值。
- **今日任務**：顯示今日訓練量、完成組數、訓練部位與三項任務完成狀態。
- **訓練趨勢圖**：
  - 週規模：本週星期日至星期六，每根柱代表一天。
  - 月規模：將當月分為五個日期區間。
  - 年規模：每根柱代表一個月。
- **歷史紀錄**：首頁顯示最近 5 筆運動紀錄，完整資料仍保存在 CSV。
- **日曆與提醒**：可查看每日運動、建立提醒，程式啟動後會檢查提醒時間。
- **成就與稱號**：具備成就牆；可從已解鎖成就選擇角色稱號。
- **個人資料與科學數值**：可設定身高、體重、年齡、體脂、目標體重與增肌／減脂目標，並顯示 BMI、BMR、TDEE 等估算值。
- **清空資料**：首頁提供確認視窗，確認後才會重置玩家進度、運動紀錄、成就及行事曆計畫。

> 目前沒有獨立的「日規模」圖表按鈕；每日資料顯示在「週規模」圖表中。今日目標預設為 `1000` 訓練量，目前需從程式常數調整。

## 專案執行方式

### 環境需求

- 建議使用 **JDK 21**。目前原始碼已使用 JDK 21 編譯驗證。
- 不需要 Maven、Gradle 或第三方套件；UI 使用 Java Swing，圖片讀取使用 Java 內建 `ImageIO`。
- 請從專案根目錄執行，因為 `assets` 與 `data` 都使用相對路徑。

確認 Java 版本：

```powershell
java -version
javac -version
```

### 編譯與執行

在專案根目錄使用 PowerShell：

```powershell
javac -encoding UTF-8 -d demo-out (Get-ChildItem -Recurse -Filter *.java).FullName
java -cp demo-out fithero.FitQuestApp
```

`demo-out` 是建議的編譯輸出資料夾。專案根目錄與套件資料夾內可能存在舊 `.class`，但以上指令只會使用 `demo-out` 內的新編譯結果。

首次執行時，程式會要求建立使用者資料，並自動建立 `data` 資料夾。`assets` 資料夾必須保留在專案根目錄，否則角色會退回顯示簡易線條人偶。

## 專案資料夾與檔案結構

```text
java_final_verison/
├─ FitQuestApp.java                    # 主程式入口、首次註冊、系統通知
├─ assets/                             # 角色圖片素材
│  ├─ body/ chest/ abdomen/ back/
│  ├─ hand/ leg/
│  ├─ head/
│  └─ emoji/
├─ data/                               # 執行後產生的使用者資料
├─ infra/
│  └─ Storage.java                     # 玩家資料與歷史紀錄讀寫
├─ logic/
│  ├─ calculator/
│  │  └─ ExpCalculator.java            # 卡路里、強度與 XP 計算
│  └─ manager/
│     ├─ PlayerState.java              # 玩家狀態、升級與部位成長
│     ├─ AchievementManager.java       # 成就檢查與解鎖紀錄
│     └─ FitnessGoal.java              # 增肌／減脂目標
├─ model/
│  ├─ achievement/Achievement.java     # 成就資料模型
│  ├─ exercise/
│  │  ├─ ExerciseInfo.java             # 單一運動屬性
│  │  ├─ ExerciseRegistry.java         # 70 種運動與肌群對應
│  │  ├─ MuscleGroup.java              # 五大肌群與顏色
│  │  └─ WorkoutType.java              # 運動類型相容映射
│  ├─ player/
│  │  ├─ Avatar.java                   # 角色等級、XP、稱號與肌群數值
│  │  ├─ UserProfile.java              # 身高、體重、性別
│  │  └─ Gender.java
│  └─ workout/WorkoutEntry.java        # 單筆運動紀錄與 CSV 轉換
└─ ui/
   ├─ FitQuestFrame.java               # 首頁 UI、頁面切換、歷史表格、清空資料
   ├─ AvatarPanel.java                 # 正反面角色組合、角色狀態與雷達圖
   ├─ WorkoutSessionDialog.java        # 新增運動與自訂訓練菜單
   ├─ WorkoutBarChartPanel.java        # 週／月／年訓練趨勢圖
   ├─ CalendarPage.java                # 日曆、每日紀錄、提醒與偷懶判定
   └─ AchievementWallPage.java         # 成就牆與稱號選擇
```

### `data` 內的重要檔案

| 檔案 | 用途 |
|---|---|
| `player.properties` | 玩家資料、總等級、XP、各肌群原始數值 |
| `workouts.csv` | 所有歷史運動紀錄 |
| `custom_menu.properties` | 使用者自訂訓練菜單 |
| `custom_plans.properties` | 日曆提醒與完成／偷懶狀態，建立提醒後才會出現 |
| `unlocked_achievements.properties` | 已解鎖成就 |

## 重要程式邏輯

### XP 如何計算

主要公式位於 `logic/calculator/ExpCalculator.java`，送出流程位於 `logic/manager/PlayerState.java`。

有氧運動：

```text
消耗熱量 = (分鐘 / 60) × MET × 玩家體重
XP = 消耗熱量 × 0.5
```

阻力訓練：

```text
非自重熱量 = (重量 × 次數 × 組數 × 0.06) × (玩家體重 / 70)
自重熱量   = (玩家體重 × 0.1) × 次數 × 組數 × 0.05
XP         = 熱量 × 0.3 + 訓練強度 × 20
```

阻力訓練強度介於 1 至 10，會參考重量、運動難度、估算肌肉量、性別與年齡。若連續三週都有運動且近 21 天沒有偷懶狀態，當次 XP 會乘上 `1.25`。

### 等級如何提升

- 初始等級為 `1`，初始升級需求為 `100 XP`。
- 當目前 XP 大於或等於升級需求時，扣除該次需求並提升一級。
- 每次升級後，下一級 XP 需求乘上 `1.2`。
- 若一次取得大量 XP，可在同一次檢查中連升多級。

### 各部位等級如何更新

- 運動與肌群的對應定義於 `model/exercise/ExerciseRegistry.java`。
- 阻力訓練完成後，部位原始成長值為：

```text
部位成長值 = 四捨五入(訓練強度 × 組數 × 0.6)
```

- 顯示用部位等級由 `PlayerState.muscleLevel()` 計算：

```text
部位顯示等級 = floor(sqrt(部位原始值 × 0.7)) + 1
```

- 胸、背、手臂、腿、腹部分別保存；肌群顯示等級直接對應四階素材：

| 顯示等級 | 使用素材 |
|---|---|
| 1 | `lv1` |
| 2 | `lv2` |
| 3 | `lv3` |
| 4 以上 | `lv4` |

角色身體 `body` 仍依玩家總等級區間選圖；其他肌肉部位直接使用各自的部位顯示等級選圖。若要求的圖片不存在，程式會優先使用最接近的可用等級圖片；背面專用素材不存在時會退回正面素材，避免角色部位消失或程式錯誤。

### 今日訓練量如何計算

首頁任務與趨勢圖共用 `WorkoutEntry.trainingVolume()`：

```text
阻力訓練量 = 重量 × 次數 × 組數
自重／有氧訓練量 = 完成量 × 組數
```

在週規模圖表中，今日柱狀會找出日期為今天的所有 `WorkoutEntry`，將訓練量依胸、背、腿、手臂、腹部或有氧分類後加總。預設今日目標為 `1000`，每日危險值為 `3000`。達到危險值的柱狀與折線節點會顯示紅色警示；月、年圖會依顯示區間換算目標與危險值。

### 圖表資料從哪裡來

- 資料來源：記憶體中的 `List<WorkoutEntry>`，啟動時由 `data/workouts.csv` 載入。
- 圖表實作：`ui/WorkoutBarChartPanel.java`。
- 每筆紀錄依運動名稱查詢 `ExerciseRegistry`，再依肌群或有氧分類堆疊。
- 柱狀高度使用訓練量，折線表示各時段訓練量總和，虛線表示平均訓練量；綠色目標線與紅色危險值線用來辨識訓練量狀態。

### 歷史紀錄如何儲存與讀取

- `WorkoutEntry.toCsvLine()` 將資料轉為：

```text
時間,運動名稱,重量,數量,組數,XP
```

- `Storage.saveWorkouts()` 將完整紀錄寫入 `data/workouts.csv`。
- `Storage.loadWorkouts()` 啟動時逐行讀取並轉回 `WorkoutEntry`。
- 首頁表格依時間新到舊排序，只顯示最近 5 筆；CSV 仍保存全部紀錄。
- 讀取器仍支援舊版四欄 CSV 格式。

## UI 調整指南

| 想調整的項目 | 修改位置 |
|---|---|
| 角色合成畫布大小與留白 | `ui/AvatarPanel.java` 的 `CANVAS_SIZE`、`PREVIEW_MARGIN`、`COMPOSE_SIZE` |
| 首頁角色正反面顯示大小 | `AvatarPanel.drawDynamicLayeredAvatars()` 的 `availableWidth`、`availableHeight`、`scale`、`startX`、`y` |
| 部位隨等級向外／向下偏移 | `AvatarPanel.java` 的 `HAND_LEVEL_OUT_STEP`、`CHEST_LEVEL_HAND_OUT_STEP`、`LEG_LEVEL_OUT_STEP`、`LEG_LEVEL_DOWN_STEP` |
| 首頁最近紀錄顯示筆數 | `FitQuestFrame.java` 的 `HOME_RECENT_RECORD_LIMIT` |
| 今日目標訓練量 | `FitQuestFrame.java` 的 `DAILY_TRAINING_VOLUME_TARGET` |
| 每日訓練危險值 | `FitQuestFrame.java` 的 `DAILY_TRAINING_DANGER_THRESHOLD` |
| 今日任務組數／部位數 | `FitQuestFrame.java` 的 `TODAY_SET_TARGET`、`TODAY_MUSCLE_TARGET` |
| 圖表顏色與圖例 | `WorkoutBarChartPanel.java` 的 `COLOR_*`、`LINE_COLOR`、`TARGET_COLOR`、`DANGER_COLOR` 與 `createLegendPanel()` |
| 新增訓練頁的暗色表格與捲動區 | `WorkoutSessionDialog.java` 的 `applyDarkTableTheme()`、`applyDarkScrollPane()` |
| 首頁左右區塊版面 | `FitQuestFrame.createHomePage()`、`buildCharacterPanel()`、`buildHomeRightColumn()` |
| 圖表、今日任務及歷史區塊高度 | `buildTrendPanel()`、`buildTodayMissionPanel()`、`buildRecentRecordsPanel()` 的 `setPreferredSize()` |

## 圖片與素材說明

所有角色素材位於 `assets`，目前圖片皆為 `256 × 256 PNG`，分層時會疊在同一畫布座標上。

| 資料夾 | 用途與命名範例 |
|---|---|
| `assets/body/` | 全身基底：`lv1_body.png` 至 `lv4_body.png` |
| `assets/chest/` | 胸部：`lv1_chest.png` 至 `lv4_chest.png` |
| `assets/abdomen/` | 腹部：`lv1_abdomen.png` 至 `lv4_abdomen.png` |
| `assets/back/` | 背部：`lv1_back.png` 至 `lv4_back.png` |
| `assets/hand/` | 正面手部：`lv2_leftHand.png`；專用背面手部：`lv2_back_leftHand.png` |
| `assets/leg/` | 正面腿部：`lv4_rightLeg.png`；專用背面腿部：`lv4_back_rightLeg.png` |
| `assets/head/` | `head.png`、`back_head.png` |
| `assets/emoji/` | 正面表情：`emoji.png` |

目前背面素材共用規則：

- 身體 `body`：所有等級正面與背面共用正面素材。
- 手部 `hand`：`lv1`、`lv3` 背面使用正面素材；`lv2`、`lv4` 使用專用背面素材。
- 腿部 `leg`：`lv1`、`lv3` 背面使用正面素材；若其他等級沒有專用背面素材，也會自動退回正面素材。
- `lv3_back_leftHand.png` 與 `lv3_back_rightHand.png` 目前保留在資料夾，但程式不會使用。

新增素材時請遵守現有大小、透明背景、座標及大小寫命名。程式會嘗試忽略檔名大小寫差異，但不應依賴此容錯行為。

## 給組員的開發注意事項

- 修改 UI 時，盡量只改 `ui` 內的版面與繪製程式，不要順手更動 `PlayerState` 或 `ExpCalculator` 的資料計算邏輯。
- 修改 `player.properties` 或 `workouts.csv` 格式時，要同步更新讀取程式，並保留舊資料相容處理。
- 新增運動項目時，要同步更新：
  - `ExerciseRegistry` 的運動屬性與肌群對應。
  - `WorkoutSessionDialog.updateExerciseList()` 中顯示用的完整清單。
- 清空資料功能必須保留確認視窗，避免誤刪使用者資料。
- 新增圖片前，確認資料夾、檔名、等級、左右側、正背面及透明畫布座標一致。
- 所有 Java 原始碼請使用 UTF-8 儲存，編譯時保留 `-encoding UTF-8`。
- 不要直接依賴原始碼旁的舊 `.class`；Demo 前重新編譯到 `demo-out`。
- `data` 是實際使用者資料。測試刪除或修改前先備份。

## 常見問題

### 圖片沒有顯示

1. 確認 Terminal 目前位置是專案根目錄。
2. 確認 `assets/body/lv1_body.png` 或 `assets/head/head.png` 存在。
3. 確認檔名符合等級、左右側與正背面命名規則。
4. 確認 PNG 透明畫布中確實有可見內容。
5. 若素材無法載入，程式會顯示簡易線條人偶。

### 歷史紀錄沒有出現

1. 確認訓練菜單中的項目有勾選，再按「完成紀錄」。
2. 確認運動名稱存在於 `ExerciseRegistry`。
3. 確認 `data/workouts.csv` 存在且每行可解析。
4. 查看 Terminal 是否出現「歷史紀錄載入失敗」訊息。

### 圖表沒有資料

1. 圖表來源也是 `data/workouts.csv`，先確認歷史紀錄是否成功載入。
2. 確認紀錄日期落在目前選擇的本週、本月或本年。
3. 確認紀錄中的重量、次數／分鐘與組數可計算出大於 `0` 的訓練量。
4. 若運動名稱無法在 `ExerciseRegistry` 找到，該筆會暫時歸類到有氧區。

### 編譯失敗或找不到主類別

1. 確認 `java -version` 與 `javac -version` 均可執行，建議使用 JDK 21。
2. 確認從專案根目錄執行編譯指令。
3. 必須使用 `javac -d demo-out ...`，讓編譯器依 `fithero.*` package 建立正確目錄。
4. 執行時使用 `java -cp demo-out fithero.FitQuestApp`，不要直接使用專案根目錄作為 classpath。

## 待改進與已知限制

- **新增完整歷史紀錄頁**：目前首頁只顯示最近 5 筆，尚無篩選、搜尋及分頁功能。
- **新增設定頁**：目前個人資料頁可調整身體資料，但尚無通知、主題或圖表目標設定。
- **新增可設定的每日訓練目標**：目前已有程式常數與首頁任務 UI，但尚未提供設定頁輸入。
- **新增獨立日規模圖表**：目前每日趨勢包含在週規模圖表內。
- **新增訓練推薦**：可依近期肌群、疲勞與目標推薦下一次課表。
- **新增資料匯出／匯入**：目前資料存在 Properties 與 CSV，尚無 UI 匯出功能。
- **強化角色肌肉變化**：可補齊更多專用背面素材與更平滑的部位等級變化。
- **完善成就條件**：成就牆已存在，但部分隱藏成就描述尚未實作對應判定。
- **保存使用者選擇的稱號**：目前稱號更換後不會寫入 `player.properties`。
- **檢查阻力訓練難度參數**：`ExerciseInfo` 的阻力訓練建構子目前需要確認是否正確保存傳入的難度係數，否則強度計算可能長期落在低值。
