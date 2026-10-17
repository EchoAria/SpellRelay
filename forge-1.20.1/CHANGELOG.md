# 1.0.1 — Forge 1.20.1 port (2026-10-17)

- Ported Spell Relay's client UI and spell sequencing to Minecraft 1.20.1, Forge 47.4.13 and Java 17.
- Targets the user's Iron's Spells 'n Spellbooks 1.20.1-3.15.4 JAR.
- Uses Iron's native QuickCastPacket and current packet distributor.
- Preserves physical spellbook slot mapping, server-confirmed casting, cooldown policies and manual stop behavior.
- Keeps the `ironautospell` ID and JSON configuration format for continuity with the NeoForge project.
- Packaged separately from the NeoForge 1.21.1 build.
