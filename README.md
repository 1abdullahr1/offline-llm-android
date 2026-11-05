# EdgeLLM Offline: 100% On-Device AI for Android

EdgeLLM Offline is an Android application engineered to run Large Language Models (LLMs) locally on-device without internet access, external API keys, or cloud infrastructure.

---

## Engineering Features

- **Zero-Copy Memory-Mapped Files (`mmap`)**: Maps GGUF model files directly into virtual address space, eliminating duplicate heap allocations and preventing Out-Of-Memory (OOM) termination by the Android Low Memory Killer (LMK).
- **Quantized KV Cache (Q8_0 / Q4_0)**: Shrinks Key-Value self-attention cache memory footprint by 50% to 75% compared to standard FP16.
- **Heterogeneous SoC Core Affinity**: Automatically detects Performance vs. Efficiency CPU cores and pins parallel matrix operations strictly to fast cores, avoiding lockstep thread stalls.
- **Dynamic Thermal Throttling Governor**: Integrates with Android `PowerManager.OnThermalStatusChangedListener` to scale inference threads and pace token generation before SoC thermal throttling collapses CPU frequencies.
- **Decoupled 60 FPS Token Stream**: Batches streaming tokens in a 35 ms window to eliminate high-frequency UI recomposition overhead.
- **Local Persistence**: Room SQLite database with full session and message history.

---

## Supported Model Architectures

The engine accepts standard GGUF quantized models:
- **Llama 3.2 (1B & 3B)**: `<|begin_of_text|>` template formatting
- **Qwen 2.5 (0.5B, 1.5B, 3B)**: ChatML template formatting
- **SmolLM2 (135M, 360M, 1.7B)**: ChatML template formatting
- **Gemma 2 (2B)**: `<start_of_turn>` template formatting

---

## Cloud CI Build

This project compiles and produces debug APK artifacts exclusively in GitHub Actions:
- Workflow file: `.github/workflows/build-android.yml`
- JDK: 17 (Temurin)
- Gradle: 8.11.1
- Android SDK: Compile 35 | Target 35 | Min 26
