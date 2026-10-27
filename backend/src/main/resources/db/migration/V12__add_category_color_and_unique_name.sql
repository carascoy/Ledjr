-- Each category gets a fixed color slot (0-7) from the categorical palette, assigned once
-- when it's created, so its color never changes when other categories are added or removed
-- (color follows the category, never its position in a list).
ALTER TABLE category
    ADD COLUMN color_slot INTEGER NOT NULL DEFAULT 0;

-- Give any existing categories distinct slots per user, in the order they were created.
UPDATE category c
SET color_slot = sub.rn % 8
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY id) - 1 AS rn
    FROM category
) sub
WHERE c.id = sub.id;

ALTER TABLE category
    ADD CONSTRAINT category_color_slot_range CHECK (color_slot BETWEEN 0 AND 7);

-- One user can't have two categories with the same name ("Groceries" and "groceries" count as the same).
CREATE UNIQUE INDEX category_user_name_unique ON category (user_id, LOWER(name));
