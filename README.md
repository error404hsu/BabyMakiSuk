# BabyMakiSuk 幼兒管理 App

> Kotlin + Jetpack Compose + Clean Architecture + Room

## 模組結構

```
app/
coremodel/   - 資料模型 (純 Kotlin)
coredata/    - Room DB + Repository
coreui/      - 共用 UI 元件 / Theme
coreai/      - ServiceAI Client 封裝
featurehome/       - 首頁 (Child 列表)
featuregrowth/     - 成長紀錄 + 圖表
featuremedical/    - 醫療紀錄
featurevaccine/    - 疫苗紀錄
featurelog/        - 每日日誌
featuresettings/   - 設定 / Sync
```

## Roadmap
- **Phase A** - 專案骨架 & 資料模型 ✅
- **Phase B** - 成長紀錄 & 圖表
- **Phase C** - 醫療紀錄 & ServiceAI 整合
- **Phase D** - 每日日誌 & AI 每週總結
- **Phase E** - Sync / Backup / Reminders
