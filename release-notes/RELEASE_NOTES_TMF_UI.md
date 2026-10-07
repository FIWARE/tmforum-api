# Release Notes: tmf-ui, a read-only browser for the all-in-one

## Overview

A new module, `ui`, adds a web page for looking at what the all-in-one's TMForum APIs contain:
list the entities of all 20 APIs, open one, and follow the references between entities as
hyperlinks — including the reverse direction ("which offerings name this organization"), which
TMForum does not store and the page turns into the matching query.

It only ever issues `GET`.

## Image

It ships as an image of its own, **`quay.io/fiware/tmforum-ui`**, built and tagged with the rest.
The all-in-one does not contain it and does not change.

Run it as a sidecar of the all-in-one:

```
TMF_ENDPOINT=http://localhost:8632
```

or on its own, to debug an environment through a port-forward:

```
docker run --rm -p 8080:8080 -e TMF_ENDPOINT=http://host.docker.internal:8632 quay.io/fiware/tmforum-ui
```

and open `http://<host>:8080/`. The image's server proxies the page's `GET`s to the API, so the
API needs no CORS configuration. `/healthz` is there for probes.

## Module

- **Module:** `ui/`
- **Contents:** the page under `src/main/resources/tmf-ui` and `server/server.mjs` (Node, no
  dependencies); no Java code
- **Image:** jib, `oci` profile, on `ubi9/nodejs-22-minimal`, runs as uid 1001, port 8080
- **Development:** `npm start` / `npm run mock` in `ui/` run the same server without the image

## Notes

- Set `TMF_ENDPOINT` wherever somebody else can reach the port: without it the proxy accepts
  any URL.
- `ui/src/main/resources/tmf-ui/catalog.js` is generated from the API modules
  (`npm run gen-catalog` in `ui/`). The `UI` workflow fails a pull request whose catalogue is
  stale, and smoke-tests the server.
- Only the all-in-one is supported; a deployment with one container per API is not.
- The page has no authentication of its own, and does not get through a PEP that demands a token.
