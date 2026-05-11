package com.babymakisuk.coreai

/**
 * Abstraction for ServiceAI calls.
 * Concrete implementation will be wired via Hilt; swap for Cloud LLM via feature flag.
 */
interface ServiceAiClient {
    /**
     * Send a prompt and receive a plain-text response.
     * Throws [ServiceAiException] on network / model error.
     */
    suspend fun complete(prompt: String): String
}

class ServiceAiException(message: String, cause: Throwable? = null) : Exception(message, cause)
