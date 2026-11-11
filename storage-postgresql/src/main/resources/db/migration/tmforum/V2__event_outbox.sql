-- One row per TMForum event of an API write, inserted in the write's transaction and deleted once every hub
-- acknowledged it. event_kind is CREATE, UPDATE or DELETE; new_state is null for DELETE, old_state for CREATE.
-- entity_type (the NGSI-LD type segment) scopes which deployment's dispatcher claims the row.
CREATE TABLE tmforum.event_outbox (
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_kind      text        NOT NULL,
    entity_id       text        NOT NULL,
    entity_type     text        NOT NULL,
    new_state       jsonb,
    old_state       jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    attempts        int         NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now()
);
