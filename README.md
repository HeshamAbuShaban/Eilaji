# 🩺 **Eilaji** (علاجي)  
### A Pharmacy Companion Built for Arabic-Speaking Communities

> **"Eilaji" = "My Cure"** ,  a name rooted in personal meaning and regional relevance.

Eilaji is a **production-grade Android application** that digitizes the patient–pharmacy relationship in underserved markets. It enables users to **share prescriptions**, **chat securely with pharmacists**, **discover nearby pharmacies**, and **never miss a dose**, all in a **bilingual (Arabic/English), RTL-first interface** designed with cultural sensitivity and technical precision.

---

## ✨ Core Capabilities

- **📸 Prescription Sharing**  
  Upload or capture a prescription image, add notes, and send it directly to pharmacies.

- **💬 Real-Time Pharmacist Chat**  
  Firebase-powered messaging with push notifications (FCM) and persistent history.

- **🗺️ Pharmacy Discovery**  
  Browse and navigate to nearby pharmacies using Google Maps integration.

- **⏰ Smart Medication Reminders**  
  Create customizable local reminders (sound, label, time) using **Room + WorkManager**, fully offline-capable.

- **❤️ Personalization**  
  Save favorite medicines & pharmacies, leave ratings, and switch seamlessly between Arabic and English.

- **🌙 Full Dark Mode & RTL Support**  
  Native night theme and right-to-left layout optimized for Arabic typography (Cairo font).

---

## 🛠️ Technical Foundation

| Layer | Technology |
|------|------------|
| **Language** | Kotlin (primary), Java (legacy adapters) |
| **Architecture** | MVVM + Repository Pattern |
| **UI** | XML + ViewBinding, Material Design, SDP/SSP for responsive sizing |
| **Navigation** | Jetpack Navigation Component |
| **State** | ViewModel + LiveData |
| **Local DB** | Room (for reminders, favorites) |
| **Remote** | Firebase (Auth, Firestore, Realtime DB, Cloud Messaging) |
| **Image Loading** | Glide |
| **Utilities** | Shimmer (loading states), MagicIndicator, RoundedImageView |
| **Permissions** | Structured runtime permission handling |
| **Build** | Gradle, ProGuard-ready |

> 🔒 **No analytics, no ads, no unnecessary permissions.** Privacy by design.

---

## 🎨 Design Philosophy

- **Minimalist aesthetic** restrained color palette, functional typography.
- Primary color: `#BA324F` (deep rose) ,  used sparingly for emphasis.
- **Arabic-first UX**: all strings localized, layouts fully RTL-compliant, culturally appropriate icons and interactions.
- **Accessibility-conscious**: semantic structure, scalable text, high-contrast modes.

---

## 📦 Project Structure Highlights

```
/app
├── ui/                # Activities, Fragments, ViewModels
├── adapters/          # RecyclerView logic (migrating Java → Kotlin)
├── firebase/          # Modular Firebase services (chat, auth, FCM)
├── storage/           # SharedPreferences abstraction (AppSharedPreferences)
├── reminder_system/   # Full local reminder engine (Room + WorkManager)
├── res/
│   ├── values-ar/     # Complete Arabic localization
│   └── values-night/  # Dark theme support
└── models/            # Data classes & mappers
```

---

## 🚀 Vision & Roadmap

Eilaji is more than an app, it’s a **foundation for digital health access** in regions where pharmacy interaction remains analog. Future iterations will explore:

- Barcode-based medicine scanning  
- Drug interaction warnings (via public APIs)  
- Offline-first data sync  
- End-to-end encrypted chat (optional)  
- Dynamic feature modules (to reduce APK size)

---

## 📄 License

**Private project** ,  shared for **portfolio, mentorship, and learning purposes only**.  
© Hesham AbuShaban, Omar, Faras 2025. All rights reserved.

---

## 💬 About the Creator

> “I built Eilaji not just to solve a technical problem, but to honor a personal one, making medicine access feel familiar, trustworthy, and human for people like my grandmother.”  
> ,  **Hesham Abu Shaban**, Android Engineer
