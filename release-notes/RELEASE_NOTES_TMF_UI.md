# Release Notes: tmf-ui, a read-only browser for the all-in-one

## Overview

A new module, `ui`, adds a web page to the all-in-one for looking at what its TMForum APIs
contain: list the entities of all 20 APIs, open one, and follow the references between
entities as hyperlinks — including the reverse direction ("which offerings name this
organization"), which TMForum does not store and the page turns into the matching query.

It only ever issues `GET`.

## Enabling it

Off by default. Start the all-in-one with

```
TMF_UI_ENABLED=true
```

and open `http://<all-in-one>:8632/ui/`. It is served on the API's own port and origin, so it
needs no CORS configuration and no proxy. Nothing changes for deployments that do not set the
variable.

## Module

- **Module:** `ui/`
- **Contents:** static files under `classpath:tmf-ui`, no Java code
- **Served by:** `all-in-one`, via `micronaut.router.static-resources.ui`
- **Development:** `ui/dev/server.mjs` (Node ≥ 18, no dependencies) runs the same page against
  any endpoint, or against sample data with `npm run mock`

## Notes

- `ui/src/main/resources/tmf-ui/catalog.js` is generated from the API modules
  (`npm run gen-catalog` in `ui/`). The `UI` workflow fails a pull request whose catalogue is
  stale.
- Only the all-in-one is supported; a deployment with one container per API is not.
- The page sits behind whatever protects the API. It has no authentication of its own.
