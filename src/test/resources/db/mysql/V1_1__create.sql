create table Customer (id bigint not null, email varchar(255), userName varchar(255), primary key (id)) engine=InnoDB;
create table ids (id varchar(255) not null, value bigint, primary key (id)) engine=InnoDB;
insert into Customer (id, email, userName) values (1, 'admin@mail.de', 'Admin');
insert into ids(id, value) values ('customer_id',1);