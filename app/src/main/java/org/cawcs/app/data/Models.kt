package org.cawcs.app.data

/** A counsellor as shown to clients. Real names are optional; credentials are verified server-side. */
data class Counsellor(
    val id: String,
    val displayName: String,
    val specialties: List<String>,
    val languages: List<String>,
    val timezone: String,
    val rating: Double,
    val verified: Boolean,
    val online: Boolean,
    val pricePerSessionUsdCents: Int,
    val bio: String,
)

enum class SessionMode { TEXT_CHAT, VOICE, VIDEO }

enum class SessionLength(val minutes: Int, val multiplier: Double) {
    SHORT(30, 0.6), STANDARD(50, 1.0), EXTENDED(80, 1.5)
}

data class ChatMessage(
    val id: String,
    val fromClient: Boolean,
    val text: String,
    val sentAtMillis: Long,
)

data class Booking(
    val id: String,
    val counsellorId: String,
    val mode: SessionMode,
    val length: SessionLength,
    val startsAtMillis: Long?, // null = start now
    val priceUsdCents: Int,
    val status: BookingStatus,
)

enum class BookingStatus { AWAITING_PAYMENT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, REFUNDED }

/** Region-aware crisis lines. Verify and refresh these from your backend before launch. */
data class CrisisLine(val region: String, val name: String, val contact: String)

object CrisisDirectory {
    val lines = listOf(
        CrisisLine("International", "Find A Helpline (directory of free lines worldwide)", "findahelpline.com"),
        CrisisLine("International", "Befrienders Worldwide", "befrienders.org"),
        CrisisLine("Zimbabwe", "Friendship Bench", "friendshipbenchzimbabwe.org"),
        CrisisLine("South Africa", "SADAG Suicide Crisis Line", "0800 567 567"),
        CrisisLine("United Kingdom", "Samaritans", "116 123"),
        CrisisLine("United States", "988 Suicide & Crisis Lifeline", "Call or text 988"),
    )
}
