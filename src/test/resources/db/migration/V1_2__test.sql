insert into Customer (id, email, userName) select value, 'admin2@mail.de', 'Admin2' from ids where id = 'customer_id';  
update ids set value = (select value + 1 from ids where id = 'customer_id') where id = 'customer_id';
