-- name: get-authors
-- Returns all authors
SELECT *
FROM authors

-- name: find-author-by-name
-- Finds author by name
SELECT *
FROM authors
WHERE author_name = :author_name


-- name: save-author<!
-- Save the author and return the id
INSERT INTO authors
(author_name, author_info, wiki_url, photo_data)
VALUES
(:author_name, :author_info, :wiki_url, :photo_data)

