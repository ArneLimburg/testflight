insert into Customer (id, email, userName) select value, 'tesdata2@tesdata2.de', 'tesdataUser2' from ids where id = 'customer_id';  
update ids set value = (select id + 1 from Customer where email = 'tesdata2@tesdata2.de') where id = 'customer_id';
