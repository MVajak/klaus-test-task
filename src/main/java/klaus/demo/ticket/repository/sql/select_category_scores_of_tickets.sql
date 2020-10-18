SELECT ticket_id,
       rating_categories.name as category_name,
       (CASE
            WHEN rating_categories.weight != 0 THEN ROUND(
                        (((ratings.rating * rating_categories.weight) / 5) * 100) / rating_categories.weight)
            ELSE 0 END)       as score
FROM main.ratings
         LEFT JOIN rating_categories ON ratings.rating_category_id = rating_categories.id
WHERE created_at >= :dateFrom
  and created_at <= :dateTo;