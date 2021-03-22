insert into Customer (id, email, userName) select value, 'admin3@mail.de', 'Admin3' from ids where id = 'customer_id';  
update ids set value = (select value + 1 from ids where id = 'customer_id') where id = 'customer_id';
