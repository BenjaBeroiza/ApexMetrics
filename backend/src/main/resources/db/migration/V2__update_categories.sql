-- Actualiza categorías al alcance del proyecto: F1, WEC, GT3
DELETE FROM categories WHERE name IN ('GT4', 'TCR');

UPDATE categories SET name = 'F1',  regulation = NULL WHERE name = 'Formula 2';
UPDATE categories SET name = 'WEC', regulation = NULL WHERE name = 'Hypercar';
UPDATE categories SET regulation = NULL WHERE name = 'GT3';
