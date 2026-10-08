package com.example.reward

/**
 * Production AdMob Server-Side Verification (SSV) Architecture.
 *
 * In production:
 * 1. Mobile app plays Google AdMob Rewarded Ad with custom_data set to (userId + ":" + eventId).
 * 2. Google AdMob servers callback our backend endpoint via HTTPS POST/GET with query parameters:
 *    - ad_network, ad_unit, custom_data, key_id, reward_amount, reward_item, timestamp, transaction_id, user_id, signature
 * 3. Backend fetches Google's public ECDSA keys from https://www.gstatic.com/admob/reward/verifier-keys.json
 * 4. Backend verifies SHA256withECDSA signature against the canonical query string.
 * 5. Only if signature is valid does the backend mark the reward event as ADMOB_VERIFIED.
 *
 * NOTE: As required by Project Shield security rules, this class maintains full architectural
 * fidelity and will fail closed unless a valid production AdMob key is configured.
 */
class AdMobRewardVerifier(
    private val expectedAdUnitId: String? = null
) : RewardVerifier {

    override suspend fun verifyReward(
        eventId: String,
        userId: String,
        clientTimestamp: Long,
        verificationToken: String,
        extraMetadata: Map<String, String>
    ): VerificationResult {
        val ssvSignature = extraMetadata["ssvSignature"]
        val keyId = extraMetadata["keyId"]

        if (ssvSignature.isNullOrBlank() || keyId.isNullOrBlank()) {
            return VerificationResult.Failure(
                reason = "Production AdMob SSV requires valid cryptographic signature & key ID from Google servers. Use Development verifier in test mode.",
                isFraudSignal = false
            )
        }

        // Production ECDSA signature verification stub
        // Will check against https://www.gstatic.com/admob/reward/verifier-keys.json
        return VerificationResult.Failure(
            reason = "Production AdMob keys not yet configured in environment secrets",
            isFraudSignal = false
        )
    }
}
