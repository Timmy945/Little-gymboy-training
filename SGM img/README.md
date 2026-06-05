# 正面 2D 人偶狀態顯示系統

這是期末專題展示用的簡化版本。

## 功能

- 只顯示正面 2D 人偶
- 不使用背部系統
- 各部位等級彼此獨立
- 下拉選單可以直接切換部位等級
- 按下「升級」後，只切換該部位圖片
- 到最高等級後再次升級會維持最高等級
- 「全部重設」會讓所有部位回到 lv1
- 「全部最高」會讓所有部位切到最高等級

## 右側控制區

每個部位都有等級選單和升級按鈕：

```text
身體：[等級選單] [升級]
胸部：[等級選單] [升級]
腹肌：[等級選單] [升級]
手臂：[等級選單] [升級]
腿部：[等級選單] [升級]

[全部重設]
[全部最高]
```

## 顯示方式

程式會依照目前等級讀取對應 PNG：

```text
body/lv1_body.png
chest/lv1_chest.png
abdomen/lv1_abdomen.png
hand/lv1_leftHand.png
hand/lv1_rightHand.png
leg/lv1_leftLeg.png
leg/lv1_rightLeg.png
```

圖片不會被個別放大、縮小，也不會使用複雜自動對齊。手臂會整體上移，並依手臂等級與胸部等級做簡單外移；腿部會依腿部等級稍微外移與下移，降低高等級重疊。

## 座標調整

之後如果要手動微調人偶外觀，主要改 `src/AvatarDemo.java` 裡的兩個區塊：

```text
正面人偶各部位座標設定區
等級間距調整區
```

整個人偶在左側預覽區的大小，改 `AvatarPanel` 裡的 `PANEL_PADDING`。

## 執行

```powershell
javac -encoding UTF-8 src\AvatarDemo.java
java -cp src AvatarDemo
```
