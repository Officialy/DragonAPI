# Keyed item matching — D39, Minecraft 26.3

Source references:

- DragonAPI `c29c4fcc2d3a65566783a4a163d8e682116e868c`,
  `Instantiable/Data/KeyedItemStack.java`, `Instantiable/Recipe/ItemMatch.java`.
- RotaryCraft `b2288638f7f4703bb4555f72a2b86f72407cc2ae`,
  `Base/ItemBasic.java`, `Base/ItemChargedTool.java`, `Items/ItemCoil.java`.
- ChromatiCraft `1a5e514d61630995b6c0477368adfd7b0f91887c`,
  `TileEntity/Storage/TileEntityToolStorage.java`.
- ReactorCraft pre-package-reorganization source at
  `d788f1bcd33c66758d687faaf692c2668595021b`, `Base/ItemReactorTool.java`,
  `Items/ItemGeigerCounter.java`, `Items/ItemRadiationCleaner.java`,
  `Items/ItemRemoteControl.java`.
- Generated Minecraft 26.3.0.51-beta `DataComponentMap`, `TypedDataComponent`,
  `ItemStack`, `PotionContents` sources and the accepted family component/item classes.

## Source rules and the port correction

The original stack constructor enabled item identity, metadata and NBT matching,
while ignoring count. Its metadata predicate compared actual item subtypes and
ignored ordinary tool wear. The Item/Block constructors selected wildcard metadata
and ignored additional data. Extra NBT was independent of subtype identity.

The port lost the Item constructor's wildcard flag, equated metadata with
`getDamageValue`, and compared the entire component map for NBT. Durability therefore
participated twice, and ignoring NBT also discarded subtype components that replaced
old metadata. The accepted tool crate combined its original two independent policies.

The new matching roles are:

| Role | Default stack key | Native meaning |
|---|---|---|
| Item | Compare | Registered item identity; separately registered material/colour items retain their identity |
| Variant / legacy metadata | Compare | Explicit subtype component projection |
| Size | Ignore | Stack count |
| Additional / legacy NBT | Compare | Remaining components, including transient data |
| Durability | Ignore | Actual tool wear, independently selectable |

`setIgnoreVariantComponents` and `setIgnoreAdditionalComponents` name the modern
roles. The original `setIgnoreMetadata` and `setIgnoreNBT` methods retain their
independent policies. `setIgnoreDamage` selects ordinary wear explicitly.
`setIgnoreComponents` selects all three component roles together, while count stays
independent. Original `hasMeta`, `hasNBT` and `isSized` queries are restored;
`checksDamage` reports the new explicit durability policy. Item/Block keys are
item-only wildcard predicates, including worn tools and subtype components.

## Declared subtype components

`ItemKeyComponents.variant(type, default)` declares a component's subtype role
inside its deferred registration supplier, before stacks containing it are keyed.
RotaryCraft declares extract-family and railgun-tier identity; ReactorCraft declares
fuel burnup, magnet charge and nuclear-waste identity. Their codecs, network formats
and native registry identifiers remain the existing ones. Other item data, such as
canister contents, chest inventories and adjacency-upgrade NBT-derived tiers, retains
its additional-data role. Unknown components default to additional data; optional
ports must explicitly declare their own former subtypes rather than relying on names
or guessing a damage interpretation.

A declared non-null default makes absent and explicitly default values the same
subtype. Railgun/fuel/magnet zero and the original zero waste isotope use their real
source defaults. An absent extract family remains distinct because no family default
is declared. Keys store immutable projections; declarations do not mutate live keys.

Native map identity and block-state component values select variants. Potion base
identity selects the variant role; custom colour, effects and name select additional
data. Splitting the fields inside `POTION_CONTENTS` prevents ignoring either old
criterion from accidentally requiring the other. Component projection and ordering
operate directly on native values and support transient components without codecs.

Some accepted RotaryCraft charge items still carry charge in `DAMAGE`, and their
original classes deliberately set `hasSubtypes = true`. `ItemVariantProvider` lets
an item supply its component-valued subtype identity: `ItemCoil` uses its actual
charge getter, and `ItemChargedTool` supplies its current stored charge. That charge
is compared once as a variant, independently of wear. Ordinary swords and Reactor's
original non-subtype charged tools preserve their ignored-damage default. This batch
does not rename charge storage or change charge consumption, capacities or recipes.

## Equality, matching, persistence and copies

Collection equality compares both selected policies and their matching projections.
The item-only hash remains compatible with the existing simpleHash adapter.
`compareTo` uses the same equality classes, with weakly interned immutable component
projections giving non-serialized/transient values an order for live keys. This order
is process-local, not a persisted identifier or generation seed.

`matches` preserves the original fuzzy predicate: only criteria enabled in both keys
are required. It is deliberately unsuitable as a HashSet equality relation.
`match(ItemStack)` applies the receiver's complete policy, and `contains` retains
the explicit broader-policy test. `exactMatch` retains full native item/count/component
comparison regardless of policy flags. Canonical subtype defaults may match normally
while differing in exact native component representation. Empty/null queries do not
match, and an empty stack cannot become a key.

Keys copy their stack and apply the existing item-owned legacy migration to that
private copy before taking snapshots. Railgun ammo now participates in the existing
LegacyItemData load-boundary migration: typed tier data wins, legacy DAMAGE becomes
the typed tier, and obsolete DAMAGE is removed. Reactor's existing migrations are
reused. Source stacks are untouched. Returned stacks are detached and copies retain
policy/lock state; locked keys reject all policy changes.

The original persistence fields `ignoremeta`, `ignorenbt`, `sized` and `simplehash`
remain. `ignoredamage` stores the explicit wear policy; old records without it default
to ignored wear. Registry-aware codec overloads round-trip typed item components and
potion holders. Load follows the original unlocked-key behavior.

## Accepted caller review

| Caller | Result |
|---|---|
| ItemMatch / FlexibleIngredient | Template additional-data presence is derived after subtype/wear separation. A variant-only patch no longer makes an ingredient accidentally strict about names/custom effects. Explicit custom payloads remain significant. |
| Rotary auto-crafter loop keys | Ignoring additional data still preserves extract family, ammo tier and charged-item subtype identity. |
| Grinder locked seeds | Item-only keys remain wildcard predicates through the existing explicit `match` calls. |
| Rotary item-filter blacklist | Default keys retain original subtype/additional identity while ordinary tool wear is ignored; the separate MatchData filter controls remain intact. |
| Recipe uniqueness and item collation helpers | Default key policy distinguishes subtype/additional data and ignores ordinary wear/count as the original matching rules intended. |
| Inventory helper / Reactor electrolyzer | Existing explicit predicates and detached template stacks use the corrected key policy; no recipe definitions change. |
| Chroma Tool Compression Crate | Its original subtype and NBT settings are restored independently. Swords group across wear/names; potions distinguish base subtype while ignoring source-ignored custom fields. |

Excluded Chroma storage/crafter/AE/Thaumcraft consumers remain pristine and must be
reviewed with their full subsystem ports. D40's typed-map accumulation migration and
D41's heterogeneous fuzzy-set lookups remain separate backlog work.

## Evidence

Five pure component-projection regressions run without unbound ItemStacks. Three
native fixtures verify policies/order/locks, actual family subtypes and serialization,
and the real loaded Chroma tool crate. They cover ordinary wear, wildcard constructors,
independent additional/variant selection, exact/count contracts, fuzzy containment,
transient component behavior through the existing extended audit, railgun legacy
normalization, Reactor burnup/charge/isotopes, custom families and provider-held charge.

Final family validation passes 156 units (108 DA, 34 RC, 8 ReC, 6 Chroma), all 74
required native DragonAPI tests, eight accepted-class guards covering 3,274 primary
outputs, and DA/RC/ReC/Chroma production jars. All 60 required tests in the existing
railgun suite also pass, covering the shared load migration interface. Normal Play's
argument hashes remain unchanged. Detailed logs and reproduction are recorded in
`TestInstance/docs/keyed-stack-matching-2026-10-11.md`.

These checks establish headless native behavior and accepted family source parity;
interactive visuals, live remote clients and absent optional mods remain separate
acceptance work. Pending rift/worldgen/compatibility/launcher/IDE edits stay outside
the local D39 checkpoint.
