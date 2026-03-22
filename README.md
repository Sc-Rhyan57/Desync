# Desync

**Fake Lag engine for Android — injects artificial network delays via WiFi using OkHttp interceptors.**

## How it works

Desync implements the same logic as the classic Roblox fake-lag script:

1. **Anchor phase** — All outgoing packets are held (Thread.sleep) for a random duration between `minLag` and `maxLag` ms, mirroring `root.Anchored = true + task.wait(random)`
2. **Release phase** — Packets are released all at once, creating burst behavior
3. **Spike mode** — Random probability of 1.5–3× multiplied delays
4. **Packet loss** — Random drops with configurable percentage

## Features

- Real-time ping graph
- Configurable min/max lag, packet loss, spike probability
- Quick presets: Light / Medium / Heavy / Extreme
- Full log console
- Material 3 animations (Revanced-style progress bar)
- Crash reporter

## Building

```bash
./gradlew :app:assembleDebug
```
