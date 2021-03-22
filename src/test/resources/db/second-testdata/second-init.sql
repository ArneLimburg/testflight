insert into Customer (id, email, userName) select value, 'second-tesdata@tesdata.de', 'second-tesdataUser' from ids where id = 'customer_id';  
update ids set value = (select value + 1 from ids where id = 'customer_id') where id = 'customer_id';

