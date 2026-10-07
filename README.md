# Confidential Anonymous Worldwide Counselling Services (CAWCS) — Android app

Kotlin + Jetpack Compose (Material 3). Min Android 8.0 (API 26), target API 34.

## Open and run
1. Install Android Studio (Koala or newer).
2. File → Open → select this `CAWCS` folder. Let Gradle sync (it downloads the Gradle wrapper and dependencies).
3. Press Run on an emulator or a phone with USB debugging on.
4. To produce an installable file: Build → Build App Bundle(s) / APK(s). For Google Play, use Build → Generate Signed App Bundle.

The app runs fully offline in **demo mode** (sample counsellors, simulated replies, simulated payments) so you can try every screen before a backend exists.

## What's in it
| Screen | What it does |
|---|---|
| Welcome | Anonymous profile: random alias + 8-word recovery phrase. No email, phone or name. Age/terms consent. Not-an-emergency notice. |
| Counsellors | Search by topic or language, "available now" filter, verified badges, ratings, prices. |
| Counsellor detail | Choose text / voice / video, 30-50-80 min, and a regional fair-price tier. |
| Checkout | Wallet, voucher, crypto, mobile money, card, or donor-sponsored session. Escrow + refund terms. |
| Chat | Encrypted-session banner, message bubbles, auto-delete notice. |
| Sessions | Bookings and their status. |
| Wallet | Balance, redeem voucher, top up by mobile money / card / crypto. |
| Urgent help (heart icon) | Worldwide and regional crisis lines. |
| Privacy | App lock toggle, view recovery phrase, erase everything. |

## Privacy features already built in
- `FLAG_SECURE`: no screenshots, screen recording or recent-apps preview.
- Quick Exit button closes the app and removes it from recents.
- App lock (fingerprint / device PIN) on every open, re-locks when the app is backgrounded.
- Identity stored in `EncryptedSharedPreferences` (Android Keystore, AES-256-GCM).
- Backups and device-transfer disabled; HTTPS-only network config.
- No location, contacts, phone-state or advertising-ID permissions.

## Going to production — what you still need
1. **Backend** (see the payment design doc for the API): accounts by anonymous ID, counsellor onboarding and licence verification, bookings, escrow ledger, payouts.
2. **Real end-to-end encryption** for chat (libsignal) and calls (WebRTC with SRTP, e.g. via LiveKit or Jitsi self-hosted). Replace `Repository` with API + WebSocket calls.
3. **Payment providers**: swap `DemoPaymentGateway` for a Retrofit implementation hitting your backend, which talks to Stripe/Adyen (cards), Paynow/Flutterwave/DPO (mobile money), and a crypto processor.
4. **Certificate pinning** in `res/xml/network_security_config.xml`.
5. **Verify crisis line numbers** in `data/Models.kt` and serve them from the backend so they can be updated without an app release.
6. **Legal**: privacy policy, counsellor contracts, data-protection compliance (GDPR, Zimbabwe Cyber and Data Protection Act, POPIA, HIPAA if serving US clients), and safeguarding/duty-to-warn policy for counsellors.

## Project layout
```
app/src/main/java/org/cawcs/app/
  MainActivity.kt            secure window, app lock, wiring
  data/Models.kt             counsellors, bookings, messages, crisis lines
  data/Repository.kt         demo data source (replace with API)
  payments/Payments.kt       payment methods, fair pricing, gateway interface + demo gateway
  security/AnonymousIdentity.kt  alias, recovery phrase, encrypted storage, erase
  ui/CawcsApp.kt             navigation, top bar, bottom bar
  ui/Screens.kt              all screens
  ui/Theme.kt                colours, light/dark
```
