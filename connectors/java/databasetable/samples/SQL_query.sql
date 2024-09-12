SELECT name, count(*) as NumberOfUsers
FROM users
Where test is null and name = 'Marc'
Group by name
Having count(*) > 0