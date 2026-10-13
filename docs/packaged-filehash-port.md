# Packaged file-hash acceptance — D51, 2026-10-13

Target: Minecraft 26.3 / NeoForge 26.3.0.51-beta. The bridge remains protocol 2.2.0;
FILEMATCH remains control ID 35 with its original mod-id:hash string body. No
control/category/tag number or packet schema changes in this batch.

Source contract: DragonAPI c29c4fcc2d3a65566783a4a163d8e682116e868c,
Auxiliary/Trackers/ModFileVersionChecker.java. Matching registered files pass;
unknown client-only mods are ignored; disabled FILEHASH does not enforce; mismatch
or malformed input fires the original cancellable HashKickEvent and disconnects
unless cancelled. These semantics remain.

DragonAPIMod.basicSetup already computes SHA-256 from actual packaged mod files
and registers it with ModFileVersionChecker. The previous native development tests
used directories, producing Source rather than differing real archive checksums.
Client dispatch also depended on a ClientLoginEvent raised by the old LOGIN packet,
which has no current automatic sender.

ClientSessionEvents now raises the original common login/logout events from native
ClientPlayerNetworkEvent.LoggingIn/LoggingOut. Dedicated-server discovery excludes
this client-only subscriber. Login therefore dispatches real FILEMATCH frames
automatically, including reconnection. The inherited port updater remains inactive:
no update-check fetch/scheduler was connected to this lifecycle.

Checksum registration uses a concurrent map to tolerate parallel mod initialization.
HashCheckEvent is a read-only observation after enforcement: MATCH, CLIENT_ONLY,
DISABLED, KICKED or CANCELLED. It carries the actual reported/server hashes and does
not replace or change the original HashKickEvent cancellation hook. Existing
checks, reasons and client-only behavior are preserved.

The opt-in packaged harness has separate server, matching-client and different-client
runs. Eight accepted development jars are copied into a new private directory.
Only the differing DragonAPI jar adds META-INF/dragonapi-private-audit-variant.txt;
all original class/resource entries retain their exact bytes. These are local
development copies, not releases. PackagedAuditLauncher supplies actual archive
mod coordinates before NeoForge loads the game; hashes must be real SHA-256 values.

The native GameTest server listens only on an ephemeral IPv4 loopback port, with
authentication disabled for its isolated local fixture. Matching/differing clients
run in separate JVMs with their own directories and identities. Both peers register
the same required GameTest-only channels. The fixture changes and saves the actual
typed FILEHASH config once per scenario, so subsequent native configuration sync
uses the correct value. Enabled checks exercise normal login dispatch; the disabled
case additionally permits an explicit mismatching frame to verify the server gate.

Server result events and client connection/disconnect observations prove matching
acceptance, enabled mismatch rejection with the real reason, disabled acceptance,
and original-event cancellation. The server waits for final client acknowledgment;
closing early or opening an integrated world fails the audit. Ordinary Play and the
existing integrated audit never activate these fixtures.

Evidence and rerun instructions: TestInstance/docs/packaged-filehash-2026-10-13.md.
These checks do not establish authenticated remote hosting, handbook/JEI graphical
acceptance, shader/LOD compatibility or mixed-protocol peer negotiation.
