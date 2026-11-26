-- Indexes for the two statements that run on every request and every poll. Both tables had only their keys, so a
-- list scanned every current row of the table and the outbox poll scanned and sorted the whole outbox once a second.
-- A separate migration rather than an edit of V1 and V2, so a database those two already migrated keeps its
-- checksums.

-- Every list filters by is_current and type and orders by the sort keys, then created_at, id. The partial index
-- covers the filter of any list and the order of an unsorted one, which is the default; a sorted list still orders
-- the rows the index found. count(*) OVER () runs on the same rows, so it is covered as well.
CREATE INDEX IF NOT EXISTS entity_current_type_created_idx
    ON tmforum.entity (type, created_at, id) WHERE is_current;

-- OutboxDispatcher.CLAIM runs every poll-interval (1s by default) and selects the due rows of this deployment's
-- types, ordered by id. DISCARD_EXPIRED and STATISTICS filter on entity_type alone, which the same index serves.
CREATE INDEX IF NOT EXISTS event_outbox_type_due_idx
    ON tmforum.event_outbox (entity_type, next_attempt_at, id);

-- DISCARD_EXPIRED deletes the rows of the owned types older than the retention.
CREATE INDEX IF NOT EXISTS event_outbox_type_created_idx
    ON tmforum.event_outbox (entity_type, created_at);
