-- name: find-category-by-name
-- Finds category by name
SELECT *
FROM categories
WHERE category_name = :category_name


-- name: save-category<!
-- Save the category and return the id
INSERT INTO categories
(category_name)
VALUES
(:category_name)

-- name: new-quote-category!
-- Assign the quote and the category together
INSERT INTO quote_categories
(quote_id, category_id)
VALUES
(:quote_id, :category_id)


-- name: find-quote-category-pair
-- Checks to see if there is a quote category pair
SELECT *
FROM quote_categories
WHERE
quote_id = :quote_id AND
category_id = :category_id
