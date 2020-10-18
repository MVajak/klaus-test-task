SELECT (CASE
            WHEN rating_categories.weight != 0 THEN ROUND(
                        (((ratings.rating * rating_categories.weight) / 5) * 100) / rating_categories.weight)
            ELSE 0 END)
FROM main.ratings
         JOIN rating_categories ON ratings.rating_category_id = rating_categories.id
WHERE created_at >= :dateFrom
  and created_at <= :dateTo;