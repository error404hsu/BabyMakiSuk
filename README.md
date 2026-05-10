# BabyMakiSuk 幼兒管理 App

> Kotlin + Jetpack Compose + Clean Architecture + Room

## 模組結構

```
app/               - 入口模組 & Navigation
core/
  model/           - 資料模型 (純 Kotlin)
  data/            - Room DB + Repository
  ui/              - 共用 UI 元件 / Theme
  ai/              - ServiceAI Client 封裝
feature/
  home/            - 首頁 (Child 列表)
  growth/          - 成長紀錄 + 圖表
  medical/         - 醫療紀錄
  vaccine/         - 疫苗紀錄
  log/             - 每日日誌
  settings/        - 設定 / Sync
```

## Roadmap
- **Phase A** - 專案骨架 & 資料模型 ✅
- **Phase B** - 成長紀錄 & 圖表
- **Phase C** - 醫療紀錄 & ServiceAI 整合
- **Phase D** - 每日日誌 & AI 每週總結
- **Phase E** - Sync / Backup / Reminders
