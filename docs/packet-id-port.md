# Explicit packet IDs — D46, 2026-10-12

Minecraft 26.3 / NeoForge 26.3.0.51-beta. Compatibility reference: published root
checkpoint `2073045` and its pinned module commits, captured before this migration.

## Stable identities without a wire change

Seven existing enum tables now implement PacketId and declare their wire IDs
explicitly. Each assigned integer is the exact value the old ordinal encoder used.
Decoding uses an immutable ID index, not values()[index], so declaration/array
order cannot choose an operation. Negative/duplicate ID tables fail initialization;
unknown incoming IDs fail lookup. PacketTypes additionally enforces its one-byte
range. IDs are never to be recycled when an operation is retired.

| Table | Entries | Existing numeric range |
| --- | ---: | --- |
| PacketTypes | 16 | 0–15 |
| DragonAPI PacketIDs | 47 | 0–46 |
| Private field-sync PacketableData tags | 5 | 0–4 |
| RotaryCraft PacketRegistry | 67 | 0–66 |
| ReactorCraft ReactorPackets | 4 | 0–3 |
| ElectriCraft ElectriPackets | 2 | 0–1 |
| ChromatiCraft ChromaPackets | 212 | 0–211 |

The 353 mappings and independent SHA-256 fingerprints are preserved in
packet-ids-protocol-2.1.json. They are a historical compatibility fixture/document,
not an alternate runtime packet registration or a data-driven gameplay registry.
The migration also checks that every old payload-constructor argument is unchanged,
including data counts, variable-data markers, types and the private primitive tags.

PacketObj encodes the explicit category ID; field sync writes/reads explicit
primitive tags. All accepted callers use wireId(), including the blower GUI's
indexed control and the TestInstance audit. Accepted source discovery comes from
Gradle's actual source set after the allowlists are applied. The original scan found
101 direct packet-ordinal lines and the indexed blower use. Gameplay payload enums
(particle choices, note lengths, crafting modes, etc.) retain their own behavior.

ChromatiCraft's getPacket no longer clamps an invalid integer into ENCHANTER or
the last valid operation. Reactor/Rotary/Electri/DragonAPI lookups likewise reject
unknown IDs consistently. Existing payload bounds and direction checks remain;
IDs still belong to their authoritative channel/handler namespace.

## Negotiation policy

CustomNetworkBridge exposes PROTOCOL_VERSION, and actual PayloadRegistrar
registration uses it. It remains 2.1.0 because existing numbers, canonical frames,
payload names, handlers and schema arguments are preserved. A version bump merely
for replacing ordinal() would unnecessarily reject byte-compatible peers.

Future incompatible numeric assignments, removed/reused operations or changed
body schemas require an intentional protocol-version migration and compatibility
fixtures. Do not change a golden mapping just to silence a failed compatibility
check. Allocating a new operation also needs a deliberate old-peer/negotiation
decision; explicit IDs by themselves do not prove feature compatibility.

Excluded pristine 1.7.10 consumers remain untouched, in accordance with the full-
file port boundary. Their complete subsystem ports must use the explicit API when
accepted. Chroma's 212-entry enum is already accepted; assigning its IDs does not
activate the excluded ChromatiPackets handler or unported gameplay subsystems.

## Validation and scope

Four new units check reordered/scrambled tables, immutable lookup maps, duplicate/
negative IDs, unknown values and the three core published fingerprints. The existing
pipeline golden now uses a fixed historical byte rather than computing its expected
byte with the same ordinal as its encoder.

Two new native fixtures check all seven published fingerprints, every public enum
lookup, every category's native encode/decode, an independent old RAW frame,
invalid categories/controls and the actual protocol constant. The full-family
fixture uses runtime class lookup to avoid cyclic compile dependencies between
RotaryCraft and the mods which depend on it; it registers when those mods are loaded.

All 91 required DragonAPI native regressions and 182 family units pass. The packet
regression selector exercises other accepted RotaryCraft packet round trips and
handlers. Eight accepted-class guards cover 3,287 primary outputs (DA 671 / RC
1,024); development DA/RC jars build. The final accepted-source audit finds no
remaining packet-discriminator ordinal encoding, and constructor parity checks
all 353 entries. Normal Play argument hashes are unchanged.

Evidence: TestInstance/docs/packet-id-2026-10-12.md and root
build/packet-id-{family-validation,packet-regressions,shape-audit}.log/json.
Actual remote socket/client-render acceptance remains separate. D46 is complete
for the accepted port; D47 dimension-field semantics is next.
