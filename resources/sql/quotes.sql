-- name: new-quote!
-- Saves a new quote
INSERT INTO quotes
(quote_hash, quote, author_id, publish_date, boosted, skip_count,
 display_count, share_count, like_count, report_count)
VALUES
(:quote_hash, :quote, :author_id, :publish_date, :boosted, :skip_count,
 :display_count, :share_count, :like_count, :report_count)

-- name: find-quote-by-hash
-- Finds quote by md5 hash
SELECT *
FROM quotes
WHERE quote_hash = :quote_hash


-- name: find-quote-by-id
-- Finds quote by id
SELECT *
FROM quotes
WHERE id = :id

-- name: get-quotes
-- Returns all quotes in a list
SELECT *
FROM quotes


-- name: update-quote!
-- Saves the quote
UPDATE quotes
SET
quote_hash = :quote_hash,
quote = :quote,
boosted = :boosted,
skip_count = :skip_count,
display_count = :display_count,
share_count = :share_count,
like_count = :like_count,
report_count = :report_count
WHERE
id = :id


-- name: update-quote-display-count!
-- Updates the quote display count
UPDATE quotes
SET
display_count = :display_count
WHERE
id = :id


-- name: update-quote-skip-count!
-- Updates the quote skip count
UPDATE quotes
SET
skip_count = :skip_count
WHERE
id = :id


-- name: update-quote-share-count!
-- Updates the quote share count
UPDATE quotes
SET
share_count = :share_count
WHERE
id = :id


-- name: update-quote-like-count!
-- Updates the quote like count
UPDATE quotes
SET
like_count = :like_count
WHERE
id = :id


-- name: update-quote-report-count!
-- Updates the quote report count
UPDATE quotes
SET
report_count = :report_count
WHERE
id = :id
