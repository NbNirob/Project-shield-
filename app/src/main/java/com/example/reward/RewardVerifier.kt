package com.example.reward

sealed class VerificationResult {
    data class Success(
        val verificationStatus: String,
        val providerTxId: String
    ) : VerificationResult()

    data class Failure(
        val reason: String,
        val isFraudSignal: Boolean = false
    ) : VerificationResult()
}

interface RewardVerifier {
    suspend fun verifyReward(
        eventId: String,
        userId: String,
        clientTimestamp: Long,
        verificationToken: String,
        extraMetadata: Map<String, String> = emptyMap()
    ): VerificationResult
}
