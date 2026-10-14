# D55 — packaged transformer and mixin ownership, 2026-10-14

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta, Java 25. Review baseline:
DragonAPI 4ab88718 and RotaryCraft 54cee595. Minecraft API evidence is the
Gradle-generated mergeWithSources_012da5434d06693213a7a9a290d1150db864f83e_output.jar,
not the older Sources/minecraft tree.

## Removed transformer declarations

The baseline DragonAPI and RotaryCraft jars both contained
META-INF/accesstransformer.cfg and registered it in neoforge.mods.toml. Their
contents are preserved here:

DragonAPI:
```cfg
# Makes the blockrenderer public for use in the ReikaRenderDispatcher
public-f net.minecraft.client.Minecraft f_91052_
public-f net.minecraft.client.model.geom.ModelPart f_104212_ # cubes
```

RotaryCraft:
```cfg
public net.minecraft.world.item.crafting.ShapedRecipe m_44210_(Lcom/google/gson/JsonObject;)Ljava/util/Map; # keyFromJson
public net.minecraft.world.item.crafting.ShapedRecipe m_44186_([Ljava/lang/String;)[Ljava/lang/String; # shrink
public net.minecraft.world.item.crafting.ShapedRecipe m_44196_(Lcom/google/gson/JsonArray;)[Ljava/lang/String; # patternFromJson
public net.minecraft.world.item.crafting.ShapedRecipe m_44202_([Ljava/lang/String;Ljava/util/Map;II)Lnet/minecraft/core/NonNullList; # dissolvePattern
```

No current family source calls/reflection lookups consume these targets. In actual
26.3 Minecraft, the previous block-renderer field/accessor has gone; block models
use BlockModelResolver and BlockStateModelSet. The historical audit recommendation
to replace the field with Minecraft.minecraft would target a nonexistent field.
ReikaRenderDispatcher is an existing separate unfinished port boundary; deleting
its obsolete transformer does not complete that renderer. Its source and the
fully commented LODModelPart reference remain preserved for later work.

ModelPart.cubes is private final. Current ChromaModelOutlineRenderer uses native
ModelPart.visit(PoseStack, Visitor), including original cube identities, instead of
private-list access. Other ModelPart consumers use normal constructors/parts.
No visibility change or removal of final is needed for current accepted consumers.

The four old ShapedRecipe methods do not exist on that 26.3 class. ShapedRecipe
uses MAP_CODEC/STREAM_CODEC and a public pattern. ShapedRecipePattern exposes of
and its map codec; shrink now belongs to its internal pattern implementation.
RotaryCraft's RoCRecipeProvider already uses native ShapedRecipePattern.of for
original recipe datagen. No replacement transformer is required.

Both unused transformer resources and their explicit metadata registrations are
removed. No recipe, model, mixin or accepted Java class is excluded or replaced.
A future private access requirement must be justified against its actual native
owner and consumer rather than resurrecting these stale names.

## Removed duplicate mixin config

DragonAPI's baseline jar also shipped dragonapi.mixin.json: required=true,
minVersion=0.8, package=reika.dragonapi.mixins, compatibilityLevel=JAVA_21, empty
mixins/client arrays, defaultRequire=1, refmap=dragonapi.mixin-refmap.json.
No source/build/metadata consumer registers it. The baseline jar manifest has no
MixinConfigs attribute. Its plural package and refmap have no corresponding
packaged implementation. The unreferenced empty resource is removed.

The real metadata entry still names mixins.dragonapi.json and its singular
reika.dragonapi.mixin package. RotaryCraft still registers mixins.rotarycraft.json.
Both active config files and their entries are unchanged. D56's complete active
and optional mixin matrix remains separate and open.

## Validation

Fresh archive inspection passes for both standard/private copied jars:
META-INF/accesstransformer.cfg and its TOML registration are absent; the duplicate
dragonapi.mixin.json is absent. Native metadata still selects the unchanged active
configs (31 DragonAPI entries, one RotaryCraft entry). Neither jar manifest has a
MixinConfigs attribute. Proof: build/d55-packaged-resource-proof.json.

All 196 family units pass (143 DragonAPI, 34 RotaryCraft, 8 ReactorCraft,
11 ChromatiCraft), with no failures/errors/skips. Eight accepted-output guards
verify 3,302 primary classes. All 100 required native DragonAPI GameTests pass;
clean native exit in 1m 12s. Logs: build/d55-family-validation.log and
build/d55-native-validation.log.

Private packaged run 20261014-d55-a uses these SHA-256s:

| Jar | SHA-256 |
| --- | --- |
| Server/matching DragonAPI | 94a7aba1966d9caa99d51e912c5d6f17600ea6eca891f58c35f354983e0946eb |
| Different DragonAPI | 4c0c713b5ed1f5fb03034a7539aa37f4be56619185287426d42aa6f9cfa6d51f |
| Server/matching RotaryCraft | 9c05e41669806e72e67f72f5baf63c1010778c611707568ecf68c75993330815 |

Every original entry in both DragonAPI variants has identical bytes. The differing
variant adds only the existing inert checksum-fixture note. Rerun instructions:
TestInstance/docs/packaged-filehash-2026-10-13.md, with a new run ID.

The restarted packaged sequence passed matching acceptance, enabled mismatch
rejection with the exact hash reason, disabled acceptance and original cancellation.
The server passed its required packaged GameTest and exited cleanly in 3m 16s;
matching client exited cleanly in 59s, different client in 1m 2s. Final state is
ALL_PASS with client acknowledgment PASS. Logs: build/d55-packaged-server.log,
d55-packaged-match.log and d55-packaged-different.log. Each required marker is
present exactly once. No assertion or production code was relaxed for the rerun.

The first attempt's native matching client
passed, and the server enforced the correct mismatch, but the different client
received a Windows socket-abort message rather than the required hash disconnect
reason. Its strict assertion failed. Logs are preserved as
build/d55-packaged-server-attempt-a.log and d55-packaged-different-attempt-a.log;
that failed attempt is not final acceptance evidence.


Normal Play's identity/saves and disabled audit flag are unchanged. No new tests
mirror these resource deletions; existing units, native fixtures and the actual
packaged connection harness provide acceptance. This confirms the removed
resources have no current accepted consumers. It does not complete the unfinished
render dispatcher or D56's optional-client/dedicated-server mixin matrix. Port
update checking remains owner-deferred and inactive. Checkpointed locally.
