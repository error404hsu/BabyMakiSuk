package com.error404hsu.babymakisuk.coreai

import javax.inject.Inject

/**
 * Stub implementation — replace with actual on-device / local ServiceAI SDK call.
 */
class LocalServiceAiClient @Inject constructor() : ServiceAiClient {
    override suspend fun complete(prompt: String): String {
        // TODO: integrate real ServiceAI local SDK
        return """
            {
              "diagnosis_summary": "(stub) 請整合 ServiceAI SDK",
              "prescriptions": "",
              "care_instructions": ""
            }
        """.trimIndent()
    }
}
