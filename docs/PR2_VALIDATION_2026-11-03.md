# SmartIsland PR #2: validation audit (2026-11-03)

## Safety and scope

This audit follows the request to improve SmartIsland, report every change, establish what CI actually executed, and preserve main without merges, releases or unsolicited comments.

- Original PR: https://github.com/in3zalo-gk/SmartIsland/pull/2
- Original PR head: `f0c486a1285a2a7dad7c6744c17214fd63ad9ec3` (`improve/dashboard-pet-reliability`).
- Main inspected and left unchanged: `e52e4c65669c6d343721928b8f16051a41611223`.
- Isolated validation branch: `verify/pr2-unit-tests`.
- Executed validation commit: `2ff2940b3078a651204c8e12e16675091c1ea29f`.
- This documentation-only follow-up is intentionally marked [skip ci]; it does not change the tested sources or workflow.
- PR #2 is still OPEN / DRAFT / NOT MERGED. No release, PR comment, PR creation or change to main was performed by this audit.

## What the original PR contains

As recorded in its existing description and code: cancellable reaction expiry using actual durations; restoration of ongoing activity after transient reactions without a Happy loop; observation of settings/visible notifications/selection, including title changes within the same mode; preservation of gestures against ordinary updates while prioritizing calls/low-battery alerts; five pure timing tests; and a safe dashboard percentage helper with tests. Existing GPL notices remain intact.

The original PR does NOT implement the larger dashboard/statistics redesign, renderer/accessibility/preview/localization work. The standalone pet color helper is preparatory, not an integrated preview. These requests remain open, rather than being reported as completed.

## What was actually executed

The original CI run 41549266387 / job 125632437374 was inspected: the real Gradle log includes `testDebugUnitTest` and `BUILD SUCCESSFUL`, not only a reassuring job title. Release was skipped; automatic review success is not a replacement for test results. The old workflow did not preserve a useful JUnit aggregate as an artifact. No precise historical test count is asserted here.

A fresh, independent run on the isolated validation branch executed the exact verification tasks:

```sh
./gradlew --no-daemon --stacktrace lintDebug testDebugUnitTest assembleDebug
```

- Run: https://github.com/in3zalo-gk/SmartIsland/actions/runs/41629163964
- Job: https://github.com/in3zalo-gk/SmartIsland/actions/runs/41629163964/job/125894016087
- Result: SUCCESS.
- Job timestamps (UTC): 2026-11-03 11:38:36 to 11:43:35.
- Combined verification step: SUCCESS (11:38:43 to 11:43:20 UTC).
- Published JUnit aggregate: **164 tests, 0 failures, 0 errors, 0 skipped**, across 23 suites.
- New `IslandPetReactionTest`: **9 tests, 0 failures**.
- Lint and debug assembly passed in the same execution. Debug compilation is proven; installing or exercising the APK on a physical device is NOT proven.

## Changes made in this audit

### 1. Real ViewModel regression tests

Added `app/src/test/java/com/agupta07505/smartisland/ui/IslandPetReactionTest.kt`, using the actual ViewModel collectors, a real in-memory notification repository, mocked settings storage and a virtual coroutine clock. Tests cover:

1. Tap remains active at 1499 ms and expires at its 1500 ms deadline.
2. A newer drag gesture cancels an older reaction deadline.
3. Same mood with a NEW timestamp restarts expiry. This test explicitly changes the timestamp; it does not prove wall-clock identity generation under simultaneous repeated inputs.
4. Music is restored after a tap and does not then expire as a transient mood.
5. Ordinary activity changes do not overwrite an active gesture; charging resumes after it.
6. An incoming call interrupts a tap and survives the obsolete tap deadline.
7. A charging-to-low-battery title change within Battery mode interrupts the gesture.
8. Disabling and re-enabling the pet clears the reaction without a later timer corrupting state.
9. A notification response expires once without a repeated Happy loop.

No application behavior change was made in this audit: the tested regression cases passed, so there was no reproduced failing case justifying a production fix.

### 2. Evidence-preserving isolated workflow

Replaced `.github/workflows/android.yml` ONLY on the validation branch with a push/workflow_dispatch verification workflow. It has read-only repository contents permission and no release or publishing job. The original PR and main workflows are unchanged.

The workflow retains JUnit XML/HTML and lint reports, aggregates actual XML counters into the job summary, and fails aggregation if no reports exist or if failures/errors are present. Report uploads use `always()` so failed runs can still retain evidence.

Artifacts from the successful run (14-day retention, currently expire 2026-11-17):

- Unit test reports: https://github.com/in3zalo-gk/SmartIsland/actions/runs/41629163964/artifacts/13227691515
- Lint reports: https://github.com/in3zalo-gk/SmartIsland/actions/runs/41629163964/artifacts/13227496737

These changes are available for review in this isolated branch; they have not been transferred or merged into PR #2.

### 3. This audit trail

Recorded exact commits, execution, new tests, evidence handles, user-request mapping and uncompleted work in this file, rather than editing the PR description or publishing a comment.

## Limitations and remaining work

The local sandbox stopped accepting execution commands during environment setup; no successful local Android test/build run is claimed. The successful result above is a genuine GitHub-hosted execution, not a local reproduction.

Automated tests exercise JVM/Robolectric/ViewModel behavior. They do not demonstrate physical gesture delivery, animation/rendering, permissions, Android/OEM background behavior or real call/battery delivery.

### Physical-device checklist (still pending)

- [ ] Install an appropriate debug build and confirm overlay/notification permissions. No APK artifact was published by the audit workflow.
- [ ] Tap the mascot while idle, during music playback and while charging; check actual visible reaction duration and restoration.
- [ ] Repeat the same gesture rapidly and drag/release before the previous reaction deadline.
- [ ] Receive a real call while a reaction is active.
- [ ] Change charging/low-battery conditions within Battery mode and check rendered reactions.
- [ ] Disable/re-enable the pet while a reaction is pending.
- [ ] Recheck notification selection, foreground music filtering, input pausing and auto-collapse on the device.
- [ ] Check renderer/accessibility/preview/localization and dashboard redesign separately when implemented; they are not covered by this PR.

### Non-blocking CI maintenance

The run includes infrastructure notices about Node.js 20 action runtime deprecation and a future ubuntu-latest image transition. These are not lint/test/build failures. Action upgrades and runner pinning were not bundled into this behavior-focused audit.

A source review also suggests a future semantic-category test for battery_low independent of display text; this audit did not reproduce or fix a category-only scenario and does not claim coverage for it.
