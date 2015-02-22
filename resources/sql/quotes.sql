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
