-- One row per entity. version, revision and is_current are inert until versioning is implemented:
-- every row is written with their defaults, so is_current marks the only row of an id.
-- version is '' for unversioned types; revision is monotonic per id.
CREATE TABLE tmforum.entity (
    id         text        NOT NULL,
    version    text        NOT NULL DEFAULT '',
    revision   bigint      NOT NULL DEFAULT 1,
    is_current boolean     NOT NULL DEFAULT true,
    type       text        NOT NULL,
    data       jsonb       NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (id, version)
);

CREATE UNIQUE INDEX entity_current_idx ON tmforum.entity (id) WHERE is_current;
