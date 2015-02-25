-- name: find-stats-for-quote
-- Finds stats for quote
SELECT *
FROM stats_by_date
WHERE
quote_id = :quote_id AND
display_date = :display_date


-- name: insert-daily-stats!
-- Create a new stat line
INSERT INTO stats_by_date
(quote_id, display_date, skip_count, display_count, share_count, like_count, report_count)
VALUES
(:quote_id, :display_date, :skip_count, :display_count, :share_count, :like_count, :report_count)


-- name: update-daily-stats!
-- Updates a stat line
UPDATE stats_by_date
SET
skip_count = :skip_count,
display_count = :display_count,
share_count = :share_count,
like_count = :like_count,
report_count = :report_count
WHERE
quote_id = :quote_id AND
display_date = :display_date
