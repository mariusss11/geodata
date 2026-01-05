
SELECT * FROM auth.users;

SELECT * FROM auth.authorities;

SELECT * FROM borrow.borrows

SELECT * FROM borrow.borrows_action

SELECT * FROM client.client

SELECT * FROM item.library_items

SELECT * FROM item.categories

SELECT * FROM item.item_category

SELECT * FROM review.reviews


-- get item and client and comment 
SELECT 
	i.item_type,
	i.title,
	c.email,
	r.comment,
	r.rating
FROM item.library_items i 
JOIN review.reviews r ON r.item_id = i.item_id 
JOIN client.client c ON c.client_id = r.client_id;


-- See all the items that are currently borrowed and the client that is borrowing it
SELECT 
	i.title,
	i.author,
	i.item_type,
	c.name,
	b.status
FROM item.library_items i
JOIN borrow.borrows b ON i.item_id = b.item_id
JOIN client.client c ON b.client_id = c.client_id
WHERE b.status = 'borrowed';


-- get the items that have any category specified
SELECT 
	items.title,
	items.item_type,
	c.category_name
FROM item.library_items items
JOIN item.item_category ic ON items.item_id = ic.item_id
JOIN item.categories c ON ic.category_id = c.category_id;

-- Function to get the client and the items
SELECT 
	c.name,
	i.title,
	i.author,
	i.item_type,
	b.status
FROM client.client c
JOIN borrow.borrows b ON b.client_id = c.client_id
JOIN item.library_items i ON i.item_id = b.item_id
WHERE c.client_id = 4
;



