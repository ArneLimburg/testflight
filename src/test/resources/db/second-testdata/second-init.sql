insert into Customer (id, email, userName) select value, 'second-tesdata@tesdata.de', 'second-tesdataUser' from ids where id = 'customer_id';  
update ids set value = (select id + 1 from Customer where email = 'second-tesdata@tesdata.de') where id = 'customer_id';

