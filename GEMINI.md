# AI Assistant Protocol: EdgeLLM Offline

Welcome to the EdgeLLM Offline Android project. Read this file and `PROJECT_CONTEXT.md` before executing any actions.

## 1. Operating Rules
- Strictly Zero Emojis: Never emit any emoji in code, documentation, commits, or responses.
- No Local Compiles: Never run `./gradlew`, `npm`, `cargo`, or build commands on the host machine. The authoritative compiler is GitHub Actions.
- Clickable Links: Format paths with `file://` scheme and forward slashes.

## 2. CI/CD Lifecycle
- To verify changes, commit and push to GitHub.
- Check workflow status with `gh run list --limit 1` and `gh run watch <run-id>`.
- If build fails, inspect logs with `gh run view <run-id> --log-failed`, resolve root causes locally, commit, and push again.

## 3. Engineering Reference
- Memory: POSIX mmap zero-copy loading + Q8_0 / Q4_0 KV cache quantization.
- Compute: CPU thread pinning to Performance cores only; thermal throttling via PowerManager.
- Decoupling: Token stream buffering via Coroutines Channel to prevent UI rendering jank.
- See `docs/ARCHITECTURE_AND_ENGINEERING_PLAN.md` for full system specifications.
