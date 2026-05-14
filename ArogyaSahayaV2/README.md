# 🌿 Arogya Sahaya V2 — Advanced AI Health Companion
### MindMatrix VTU Internship Program — Project #78 (Advanced Edition)

---

## 🚀 What's New in V2

| Feature | Description |
|---|---|
| 🤖 **AI Voice Assistant** | Gemini 1.5 Flash powered — ask health questions by voice |
| 🗣️ **Text-to-Speech** | Responses read aloud in natural voice (Hindi/English) |
| 📡 **Offline Fallback** | Smart offline answers when no internet available |
| 💊 **Stock Tracking** | Medicine stock counter with low-stock alerts |
| 📊 **Multi-mode Charts** | Switchable BP / HR / Glucose chart views |
| ✅ **Adherence Score** | Weekly medication adherence % on home dashboard |
| 🎨 **Premium UI** | Gradient headers, card-based layout, smooth animations |
| 🔖 **Event Completion** | Mark ASHA events as done |
| 💬 **Voice History** | Saved AI conversation history |

---

## ⚡ How to Open in Android Studio

### Step 1 — Extract & Open
1. Unzip `ArogyaSahayaV2.zip`
2. Open Android Studio → **File → Open**
3. Select the `ArogyaSahayaV2` folder (the one containing `build.gradle`)
4. Choose **"Open as Project"** if prompted
5. Wait for indexing to finish

### Step 2 — Gradle Sync
- A sync bar appears automatically — click **Sync Now**
- Or go to **File → Sync Project with Gradle Files**
- Wait for the bottom status bar to say **"Gradle sync finished"**
- ✅ JitPack is already included in `settings.gradle` — no manual step needed

### Step 3 — Set Up Emulator
1. **Tools → Device Manager → Create Device**
2. Choose **Pixel 6** → API **33** (Tiramisu) or higher → Finish
3. Start the emulator with the ▶ button

### Step 4 — Run
- Press **Shift + F10** or click the green ▶ Run button
- App launches on the onboarding screen

---

## 🤖 Setting Up the AI Voice Assistant

The app works with **offline fallback** responses out of the box.

For **full AI responses** powered by Google Gemini:

1. Get a free API key from: https://aistudio.google.com/app/apikey
2. Open: `app/src/main/java/com/arogya/sahaya/service/VoiceAssistantService.kt`
3. Find line: `val apiKey = "YOUR_GEMINI_API_KEY_HERE"`
4. Replace with your key: `val apiKey = "AIza...your_key_here"`
5. Rebuild and run

> 💡 The offline fallback covers 10+ common health topics and works without any API key.

---

## 🏗️ Architecture

```
MVVM + Repository Pattern + Hilt DI + Room DB
│
├── data/
│   ├── model/         ← 6 Room entities
│   ├── db/            ← Database + 7 DAOs
│   └── repository/    ← 6 Repositories
│
├── di/                ← Hilt DatabaseModule
│
├── viewmodel/         ← 5 ViewModels with LiveData
│
├── service/
│   └── VoiceAssistantService ← STT + Gemini AI + TTS
│
├── utils/
│   ├── AlarmReceiver  ← BroadcastReceiver (pill alarms + BOOT)
│   └── AlarmScheduler ← Exact alarm scheduling
│
└── ui/
    ├── home/          ← Dashboard with adherence + vitals summary
    ├── pills/         ← Medicines + stock tracking
    ├── vitals/        ← Multi-mode charts (BP/HR/Glucose)
    ├── asha/          ← Health camp calendar
    ├── emergency/     ← SOS with countdown
    ├── voice/         ← AI Voice Assistant (full UI)
    ├── profile/       ← Medical profile card
    └── onboarding/    ← 4-step setup with dots indicator
```

---

## 📋 Success Criteria — All Met ✅

| Criterion | Implementation |
|---|---|
| Notifications in Doze mode | `setExactAndAllowWhileIdle` + `BOOT_COMPLETED` |
| 7-day vital trend graph | MPAndroidChart with BP/HR/Glucose tabs |
| High-contrast UI, large fonts | 17–22sp, WCAG-AA colours |
| Repository Pattern | 6 typed repositories |
| ASHA data simulation | 5 pre-populated health events on first launch |
| Voice AI assistant | Android STT + Gemini API + TTS |

---

## ⚠️ Common Android Studio Issues

| Error | Fix |
|---|---|
| `Could not resolve MPAndroidChart` | JitPack already in `settings.gradle` — just re-sync |
| `Unresolved class ArogyaApplication` | Gradle hasn't synced yet — click Sync Now |
| `JDK version error` | File → Project Structure → set JDK to 17 |
| Manifest errors before sync | Normal — they disappear after sync |
| `RECORD_AUDIO permission denied` | Grant mic permission when prompted on device |
| AI returns fallback response | Add Gemini API key in `VoiceAssistantService.kt` |

---

*MindMatrix VTU Internship Program | Project #78 | Arogya Sahaya V2*
