package org.cawcs.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Data source for counsellors, bookings and chat.
 * This demo implementation runs fully offline so the app is usable immediately.
 * In production, back it with the REST + WebSocket API described in the design doc;
 * chat payloads must be end-to-end encrypted (e.g. Signal Protocol / libsignal) so the server only relays ciphertext.
 */
class Repository {

    val counsellors = listOf(
        Counsellor("c1", "Dr. Amara N.", listOf("Anxiety", "Depression", "Trauma"), listOf("English", "Shona"), "Africa/Harare", 4.9, true, true, 4000,
            "Clinical psychologist, 12 years. Trauma-informed, CBT and EMDR."),
        Counsellor("c2", "Counsellor Tendai", listOf("Relationships", "Family", "Grief"), listOf("English", "Ndebele", "Shona"), "Africa/Johannesburg", 4.8, true, true, 2500,
            "Registered counsellor focusing on couples, family conflict and bereavement."),
        Counsellor("c3", "Sofia R.", listOf("Addiction", "Stress", "Burnout"), listOf("Spanish", "English", "Portuguese"), "America/Mexico_City", 4.7, true, false, 3500,
            "Licensed therapist; substance use recovery and workplace burnout."),
        Counsellor("c4", "Dr. Kenji M.", listOf("Anxiety", "LGBTQ+", "Identity"), listOf("Japanese", "English"), "Asia/Tokyo", 4.9, true, true, 5000,
            "Psychiatrist-supervised psychotherapy; affirming and identity-focused care."),
        Counsellor("c5", "Fatima A.", listOf("Gender-based violence", "Trauma", "Women's health"), listOf("Arabic", "French", "English"), "Africa/Cairo", 4.8, true, true, 3000,
            "Survivor-centred counsellor, safety planning and trauma recovery."),
        Counsellor("c6", "James O.", listOf("Youth", "Students", "Depression"), listOf("English", "Swahili"), "Africa/Nairobi", 4.6, true, false, 2000,
            "Youth and student counsellor; exam stress, loneliness and low mood."),
    )

    fun counsellor(id: String) = counsellors.first { it.id == id }

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    fun createBooking(counsellorId: String, mode: SessionMode, length: SessionLength, priceUsdCents: Int): Booking {
        val b = Booking(UUID.randomUUID().toString(), counsellorId, mode, length, null, priceUsdCents, BookingStatus.AWAITING_PAYMENT)
        _bookings.value = _bookings.value + b
        return b
    }

    fun setStatus(bookingId: String, status: BookingStatus) {
        _bookings.value = _bookings.value.map { if (it.id == bookingId) it.copy(status = status) else it }
    }

    private val chats = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    fun chat(bookingId: String): StateFlow<List<ChatMessage>> =
        chats.getOrPut(bookingId) {
            MutableStateFlow(listOf(ChatMessage("w", false,
                "Welcome. This space is private and you don't need to share your name. What would you like to talk about today?",
                System.currentTimeMillis())))
        }

    suspend fun send(bookingId: String, text: String) {
        val flow = chats.getValue(bookingId)
        flow.value = flow.value + ChatMessage(UUID.randomUUID().toString(), true, text, System.currentTimeMillis())
        delay(1500) // demo: simulated counsellor reply
        flow.value = flow.value + ChatMessage(UUID.randomUUID().toString(), false,
            "Thank you for sharing that. Take your time — can you tell me a little more about how that has been affecting you?",
            System.currentTimeMillis())
    }
}
