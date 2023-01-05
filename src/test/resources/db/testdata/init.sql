insert into Customer (id, email, userName) select value, 'tesdata@tesdata.de', 'tesdataUser' from ids where id = 'customer_id';  
update ids set value = (select id + 1 from Customer where email = 'tesdata@tesdata.de') where id = 'customer_id';
