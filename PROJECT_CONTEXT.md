# Project Context & AI Assistant Onboarding Guide: EdgeLLM Offline

This document defines the working preferences, architectural foundation, and development lifecycle for this application codebase. Any AI assistant or developer working on this project must read and strictly adhere to the guidelines below.

---

## 1. Core Principles & User Preferences

1. **Strictly Zero Emojis**:
   - Never use emojis anywhere: code, commit messages, comments, logs, documentation, or chat responses.
   - Use clean, professional language and native vector iconography only.
2. **Concise & Direct Responses**:
   - Provide direct summaries, actionable implementation details, and verification steps. Avoid excessive conversational filler.
3. **Clickable File Links**:
   - Always link referenced files, directories, classes, and methods using the `file://` URL scheme with forward slashes (e.g. `[app/build.gradle.kts](file:///D:/My%20Projects%2014%20aug/antigravity%20cli/Andriod/ai%20application%20local%20llm/app/build.gradle.kts)`).
4. **Preserve Working Architecture**:
   - Do not perform destructive refactors without careful incremental verification. Keep working features intact while extending the application.

---

## 2. Environment & Build Strategy

- **Operating System**: Windows (PowerShell shell).
- **No Local Compilation / No Local Toolchain Installations**:
  - The local Windows environment does not have Java, Gradle, Cargo, Rust, or Node installed.
  - Never execute `./gradlew`, `gradle build`, `npm install`, or package managers on the host machine.
  - Never attempt to download or install JDK, Android SDK, or build tools locally.
- **Cloud CI with GitHub Actions**:
  - All builds and packaging (Android APK) are handled in the cloud using GitHub Actions runners.
  - Workflows reside in `.github/workflows/`:
    - `build-android.yml`: Compiles the Android debug APK and uploads the artifact `offline-llm-debug-apk`.
  - **Standard Cloud Build Lifecycle**:
    1. Make required edits to code and resources locally.
    2. Commit and push changes:
       ```powershell
       git add .
       git commit -m "Your descriptive commit message"
       git push origin main
       ```
    3. Monitor workflow status via GitHub CLI:
       ```powershell
       gh run list --limit 3
       gh run watch <run-id>
       ```
    4. Download compiled artifacts:
       ```powershell
       gh run download <run-id> -n offline-llm-debug-apk --dir output_android
       ```

---

## 3. Engineering Architecture & Key Mechanisms

### Memory Architecture & OOM Prevention
- **POSIX `mmap`**: Zero-copy model loading directly from UFS storage. Model weights are mapped into virtual address space instead of allocated onto the JVM/native heap, preventing process termination by Android's Low Memory Killer (LMK).
- **Quantized KV Cache**: Supports `Q8_0` and `Q4_0` KV cache quantization, reducing context memory overhead by 50% to 75% compared to FP16.

### Thermal & Core Affinity Governor
- **Core Affinity Detection**: `CpuTopologyDetector` checks CPU frequency topology to isolate Performance/Prime cores and restrict inference threads to fast cores, avoiding efficiency-core lockstep bottlenecks.
- **Dynamic Thermal Throttling**: `ThermalController` registers `PowerManager.OnThermalStatusChangedListener` to scale down thread counts and introduce pacing micro-delays when the device reaches `THERMAL_STATUS_MODERATE` or `THERMAL_STATUS_SEVERE`.

### Decoupled UI Token Streaming
- Token generation is decoupled from Jetpack UI rendering via a 35-50 ms batching channel in `NativeLlmBridge` and `ChatViewModel`, preventing 60-120 Hz recomposition overhead.

### Data Layer
- **Room Database**: `AppDatabase` stores `ChatSession` and `ChatMessage` records with timestamps, tokens/sec, and TTFT metrics.
- **Model Registry**: `ModelRepository` scans `context.filesDir/models` for `.gguf` and `.bin` model files and streams imports from SAF file pickers.

---

## 4. Project Directory Map

- `app/src/main/java/com/offline/llm/`
  - `engine/`: `LlmEngine`, `NativeLlmBridge`, `CpuTopologyDetector`, `ThermalController`, `PromptFormatter`, `ModelConfig`, `TokenChunk`
  - `data/`:
    - `model/`: `ChatSession`, `ChatMessage`, `ModelInfo`
    - `db/`: `AppDatabase`, `ChatDao`
    - `repository/`: `ChatRepository`, `ModelRepository`
  - `ui/`:
    - `MainActivity.kt`, `MainPagerAdapter.kt`
    - `chat/`: `ChatFragment`, `ChatViewModel`, `ChatAdapter`
    - `models/`: `ModelsFragment`, `ModelsViewModel`, `ModelAdapter`
    - `diagnostics/`: `DiagnosticsFragment`, `DiagnosticsViewModel`
- `.github/workflows/`:
  - `build-android.yml`: GitHub Actions compilation workflow

---

## 5. Quick Primer Prompt for New AI Chat Sessions

When opening a new chat for this project, you can start with:

```text
Please read GEMINI.md and PROJECT_CONTEXT.md in the project root to understand the environment constraints, zero-emoji policy, cloud CI build process, and app architecture before starting.
```
