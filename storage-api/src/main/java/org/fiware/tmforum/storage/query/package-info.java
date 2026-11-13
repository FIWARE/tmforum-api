/**
 * Backend-neutral query model: the TMForum filter/sort parser produces it, and each storage backend renders it
 * into its own query language. Plain records without dependencies, so a model value is safe to use as a cache key.
 */
package org.fiware.tmforum.storage.query;
