-- Design intent (see Database Schema.md): a transaction can exist before it's
-- categorized. V5 made category_id NOT NULL by mistake, which meant a brand-new
-- user with no categories couldn't log anything. Fixed forward here instead of
-- editing V5, since V5 has already been applied (Flyway checksums applied files).
ALTER TABLE transaction
    ALTER COLUMN category_id DROP NOT NULL;
