package com.babymakisuk.coremodel

/**
 * App 操作角色定義。
 *
 * DATA_MANAGER  ── 低階機：輸入資料、僅限 Cloud API 提問，禁用本機 LLM
 * AI_OPERATOR   ── 高階機：執行本機 AI 分析、產生週報
 * ADMIN         ── 全功能（開發 / 主要管理者）
 * NONE          ── 未設定（首次啟動）
 */
enum class UserRole(val label: String, val description: String) {
    DATA_MANAGER(
        label = "資料管理員",
        description = "負責輸入孩子所有資訊，AI 功能限雲端 API 提問"
    ),
    AI_OPERATOR(
        label = "AI 操作員",
        description = "執行本機 AI 分析、產生週報（高階機適用）"
    ),
    ADMIN(
        label = "管理員",
        description = "全功能存取，包含所有輸入與 AI 功能"
    ),
    NONE(
        label = "未設定",
        description = "尚未選擇角色"
    );

    /** DATA_MANAGER 禁用本機 LLM，只能用 Cloud API */
    val canUseLocalAi: Boolean
        get() = this == AI_OPERATOR || this == ADMIN

    /** 是否可編輯輸入表單（新增就診 / 生長 / 日誌） */
    val canEditData: Boolean
        get() = this == DATA_MANAGER || this == ADMIN

    /** 是否可執行 AI 週報產生 */
    val canGenerateReport: Boolean
        get() = this == AI_OPERATOR || this == ADMIN
}
