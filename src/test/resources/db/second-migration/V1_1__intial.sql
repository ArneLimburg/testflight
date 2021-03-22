create table ids
(
    id       varchar(255) not null,
    value    int8 not null
);

create table Customer
(
    id       int8 not null,
    email    varchar(255),
    userName varchar(255),
    primary key (id)
);

insert into Customer (id, email, userName) values (1, 'second-admin@mail.de', 'Second Admin');
insert into ids (id, value) values ('customer_id', 2);

