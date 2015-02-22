-- name: get-users
-- Returns all users
SELECT *
FROM users

-- name: user-count
-- Counts all the users.
SELECT count(*) AS count
FROM users

-- name: save-user<!
-- Creates a new user
INSERT INTO users
(hash_string, visit_count, first_visit_date, last_visit_date, fast_forward_count)
VALUES
(:hash_string, :visit_count, :first_visit_date, :last_visit_date, :fast_forward_count)

-- name: find-user-by-hash
-- Finds user by md5 hash
SELECT *
FROM users
WHERE hash_string = :hash_string

-- name: find-user-by-id
-- Finds user by id
SELECT *
FROM users
WHERE id = :id


-- name: get-quotes-seen
-- Returns a list of quotes user seen
SELECT *
FROM users_quotes
WHERE user_id = :user_id
