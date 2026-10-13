# Port update checking stays inactive — D50, 2026-10-13

The owner explicitly instructed that the updater must stay inactive because they
do not yet have Reika's permission for a release. No maintained port update feed
is configured, no port update scheduler/login hook was added, and the inherited
1.7.10 endpoint remains excluded by DragonAPIMod.basicSetup. Existing explicit
external-mod endpoint behavior is preserved.

D50 remains a release-permission/feed prerequisite, not completed runtime update
acceptance. Do not enable the port updater or publish feed/version data without
the owner's later authorization after that prerequisite is satisfied.

The root and DragonAPI GitHub release lists were inspected read-only on this date
and were empty. They were not used to fabricate version data. The source reference
for the original checker remains DragonAPI c29c4fcc2d3a65566783a4a163d8e682116e868c,
Auxiliary/Trackers/CommandableUpdateChecker.java.

UpdateFeedClient is a standalone, fully implemented transport/parser with no runtime
callers. It preserves the original name:version text format and supports existing
legacy and artifact versions. Its local-only unit regressions cover valid/absent
Last-Modified, missing entries, invalid versions/sentinels, duplicate entries,
HTML/invalid UTF-8, redirects, HTTP failures, timeouts and oversized responses.
Failures are explicit results and cannot become comparison versions. It is future
port infrastructure; it does not activate the old checker or establish a production
feed. Native notifications/transitions remain deferred with D50.
