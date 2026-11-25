# Implementation Plan: TMForum - Access Policies - 03 - A FORBIDDEN exception reason

## Overview
Add a `FORBIDDEN` reason to `TmForumExceptionReason` and render it as `403` with the standard
`ErrorDetails` body in `TmForumExceptionHandler`, so a denied order (AP-5, correction D3 in
`doc/access-policies/TICKETS.md`) no longer falls into the `default ->` `500` branch.

## Steps

### Step 1: FORBIDDEN reason, its 403 branch and a parameterized handler test
**Files** (all in `common`, package `org.fiware.tmforum.common.exception`):
- `src/main/java/.../TmForumExceptionReason.java`: add `FORBIDDEN` to the enum. Keep the existing
  five constants and keep `UNKNOWN` last.
- `src/main/java/.../TmForumExceptionHandler.java`: add a `case FORBIDDEN ->` branch before
  `default ->`, built like the `NOT_FOUND` branch: `HttpResponse.status(HttpStatus.FORBIDDEN)` with
  `new ErrorDetails(HttpStatus.FORBIDDEN.toString(), HttpStatus.FORBIDDEN.getReason(),
  String.format("The requested operation is not permitted - %s", exception.getMessage()),
  HttpStatus.FORBIDDEN.toString(), null)`. Keep the file's 4-space indentation. Do not touch the
  other branches or `withReferenceCause`.
- `src/test/java/.../TmForumExceptionHandlerTest.java`: add a parameterized test
  (`@MethodSource`) with one row per `TmForumExceptionReason`: the reason, the expected
  `HttpStatus` and the expected message prefix. Expected rows: `CONFLICT` -> 409
  "At least one of the entities already exists - ", `INVALID_DATA` -> 400
  "The request contained invalid data - ", `INVALID_RELATIONSHIP` -> 400
  "At least one of the references does not exist - ", `NOT_FOUND` -> 404
  "The requested object could not be found - ", `FORBIDDEN` -> 403
  "The requested operation is not permitted - ", `UNKNOWN` -> 500
  "An unexpected error happened - ". For each row, assert the response status, and in the body
  `code()` and `status()` equal `status.toString()`, `reason()` equals `status.getReason()`, and
  `message()` equals prefix + the thrown message. Put the prefixes in named constants (reuse
  `INVALID_RELATIONSHIP_PREFIX`). Add a guard that the provider covers every
  `TmForumExceptionReason.values()` entry, so a future constant without a row fails the test.
  Keep the existing `invalidRelationshipReportsTheReferenceCause` test unchanged. The file uses tabs.

**Already checked (no change needed):** `TmForumExceptionHandler.handle` is the only `switch` over
`TmForumExceptionReason`, and it keeps its `default ->`. The switches in
`AbstractApiController.isRejectedWrite`/`rejectedWrite` are over `StorageErrorReason`.
`BrokerFailures` switches over `HttpStatus`, and `DeletionExceptionHandler` over the deletion reason.
`StorageExceptionHandler` only creates `UNKNOWN`/`INVALID_DATA` exceptions. None of them is affected.
The step agent should re-run `grep -rn "TmForumExceptionReason\|getExceptionReason" --include=*.java .`
to confirm that nothing new switches over the enum.

**No doc comments are added.** The enum and handler have none, and the reason name says enough.

**Verify:** `mvn test -pl common -am -Dtest=TmForumExceptionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false`
passes. Then run `mvn test -pl common` for the whole common unit suite, which includes the ArchUnit
and boundary tests.

**Acceptance criteria:**
- `TmForumException(..., FORBIDDEN)` renders `403` with the standard `ErrorDetails` body.
- The other five reasons give the same status and body as before, including the
  `INVALID_RELATIONSHIP` reference-cause formatting. `UNKNOWN` still gives `500`.
