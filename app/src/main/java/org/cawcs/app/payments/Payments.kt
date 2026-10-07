package org.cawcs.app.payments

import kotlinx.coroutines.delay
import org.cawcs.app.data.Counsellor
import org.cawcs.app.data.SessionLength
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Payment methods, ordered from most to least anonymous.
 * The app never sees raw card numbers: cards go through the provider's hosted checkout.
 */
enum class PaymentMethod(val label: String, val detail: String, val anonymity: String) {
    WALLET("CAWCS Wallet", "Pay from prepaid credit you already topped up", "Highest"),
    VOUCHER("Prepaid voucher code", "Redeem a scratch / e-voucher bought from a reseller", "Highest"),
    CRYPTO("Crypto (USDC / BTC)", "Pay to a one-time address via the crypto processor", "High"),
    MOBILE_MONEY("Mobile money", "EcoCash, OneMoney, M-Pesa, MTN MoMo, Airtel Money", "Medium"),
    CARD("Card / Google Pay", "Visa, Mastercard via hosted checkout", "Standard"),
    SPONSORED("Sponsored session", "Free session funded by donors / NGOs (means-tested)", "Highest"),
}

data class PriceQuote(
    val baseUsdCents: Int,
    val regionalDiscountPct: Int,
    val platformFeeUsdCents: Int,
    val totalUsdCents: Int,
    val localCurrency: String,
    val localAmount: Double,
)

sealed class PaymentResult {
    data class Success(val receiptCode: String) : PaymentResult()
    data class RequiresAction(val redirectUrl: String) : PaymentResult()
    data class Failed(val reason: String) : PaymentResult()
}

object Pricing {
    /**
     * Purchasing-power tiers (World Bank income groups). The tier is chosen by the
     * client in settings or inferred by the server from payment currency — never from GPS.
     */
    enum class Tier(val discountPct: Int) { HIGH(0), UPPER_MIDDLE(30), LOWER_MIDDLE(55), LOW(70) }

    const val PLATFORM_FEE_PCT = 15

    fun quote(c: Counsellor, length: SessionLength, tier: Tier, currency: String, fxRate: Double): PriceQuote {
        val base = (c.pricePerSessionUsdCents * length.multiplier).roundToInt()
        val discounted = base * (100 - tier.discountPct) / 100
        val fee = discounted * PLATFORM_FEE_PCT / 100 // taken from the total, not added on top
        return PriceQuote(base, tier.discountPct, fee, discounted, currency, discounted / 100.0 * fxRate)
    }
}

/** Client-side gateway. Swap [DemoPaymentGateway] for the Retrofit-backed one in production. */
interface PaymentGateway {
    suspend fun walletBalanceUsdCents(accountId: String): Int
    suspend fun topUpWallet(accountId: String, method: PaymentMethod, usdCents: Int, phone: String?): PaymentResult
    suspend fun payForBooking(accountId: String, bookingId: String, method: PaymentMethod, quote: PriceQuote, extra: String?): PaymentResult
    suspend fun redeemVoucher(accountId: String, code: String): PaymentResult
}

class DemoPaymentGateway : PaymentGateway {
    private var balance = 0

    override suspend fun walletBalanceUsdCents(accountId: String) = balance

    override suspend fun topUpWallet(accountId: String, method: PaymentMethod, usdCents: Int, phone: String?): PaymentResult {
        delay(900)
        if (method == PaymentMethod.MOBILE_MONEY && phone.isNullOrBlank()) return PaymentResult.Failed("Enter the mobile money number to send the payment prompt to.")
        balance += usdCents
        return PaymentResult.Success(receipt())
    }

    override suspend fun payForBooking(accountId: String, bookingId: String, method: PaymentMethod, quote: PriceQuote, extra: String?): PaymentResult {
        delay(1200)
        return when (method) {
            PaymentMethod.WALLET ->
                if (balance >= quote.totalUsdCents) { balance -= quote.totalUsdCents; PaymentResult.Success(receipt()) }
                else PaymentResult.Failed("Wallet balance is too low. Top up first.")
            PaymentMethod.VOUCHER -> if (extra.isNullOrBlank()) PaymentResult.Failed("Enter your voucher code.") else PaymentResult.Success(receipt())
            PaymentMethod.MOBILE_MONEY -> if (extra.isNullOrBlank()) PaymentResult.Failed("Enter your mobile money number.") else PaymentResult.Success(receipt())
            PaymentMethod.CARD, PaymentMethod.CRYPTO -> PaymentResult.Success(receipt())
            PaymentMethod.SPONSORED -> PaymentResult.Success(receipt())
        }
    }

    override suspend fun redeemVoucher(accountId: String, code: String): PaymentResult {
        delay(700)
        if (code.length < 8) return PaymentResult.Failed("That voucher code doesn't look right.")
        balance += 2000
        return PaymentResult.Success(receipt())
    }

    /** Receipts carry a random code only — no name, no counsellor, no "counselling" wording. */
    private fun receipt() = "CW-" + UUID.randomUUID().toString().take(8).uppercase()
}
