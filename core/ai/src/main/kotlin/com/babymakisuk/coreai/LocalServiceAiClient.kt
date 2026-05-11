package com.babymakisuk.coreai

import javax.inject.Inject

/**
 * Stub implementation 窶・replace with actual on-device / local ServiceAI SDK call.
 */
class LocalServiceAiClient @Inject constructor() : ServiceAiClient {
    override suspend fun complete(prompt: String): String {
        // TODO: integrate real ServiceAI local SDK
        return """
            {
              "diagnosis_summary": "(stub) 隲区紛蜷・ServiceAI SDK",
              "prescriptions": "",
              "care_instructions": ""
            }
        """.trimIndent()
    }
}
