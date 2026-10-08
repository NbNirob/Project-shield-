package com.example.reward

/**
 * Clearly separated TEST/DEVELOPMENT verification mechanism.
 * Does not pretend to be a real AdMob SSV callback, but verifies
 * structured test proofs (nonces, simulated watch durations, and test signatures).
 */
class DevelopmentRewardVerifier : RewardVerifier {

    override suspend fun verifyReward(
        eventId: String,
        userId: String,
        clientTimestamp: Long,
        verificationToken: String,
        extraMetadata: Map<String, String>
    ): VerificationResult {
        // Validate token format: "DEV_PROOF_<eventId>_<hash>"
        if (!verificationToken.startsWith("DEV_PROOF_") || verificationToken.length < 12) {
            return VerificationResult.Failure(
                reason = "Invalid dev verification token structure",
                isFraudSignal = true
            )
        }

        val watchDurationSeconds = extraMetadata["watchDurationSec"]?.toLongOrNull()
        if (watchDurationSeconds != null && watchDurationSeconds < 5L) {
            return VerificationResult.Failure(
                reason = "Rewarded ad watch duration insufficient ($watchDurationSeconds s < 5s minimum)",
                isFraudSignal = true
            )
        }

        return VerificationResult.Success(
            verificationStatus = "DEV_VERIFIED",
            providerTxId = "DEV_TX_${System.currentTimeMillis()}"
        )
    }
}
