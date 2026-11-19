-- Rows written before versioning have version '' while data holds the label. Copies it for the versionable types
-- (VersionedEntity.VERSIONED_TYPES, kept equal by VersionedTypesTest); each id has a single row, so no key collides.
UPDATE tmforum.entity
SET version = data->>'version'
WHERE is_current
  AND version = ''
  AND COALESCE(data->>'version', '') <> ''
  AND type IN (
    'agreement',
    'agreementSpecification',
    'api-specification',
    'catalog',
    'category',
    'document-specification',
    'hosting-platform-requirement-specification',
    'logical-resource-specification',
    'physical-resource-specification',
    'product-offering',
    'product-offering-price',
    'product-specification',
    'quote',
    'resource-candidate',
    'resource-catalog',
    'resource-category',
    'resource-specification',
    'service-candidate',
    'service-catalog',
    'service-category',
    'service-specification',
    'software-resource-specification',
    'software-specification',
    'software-support-package-specification',
    'usageSpecification');
