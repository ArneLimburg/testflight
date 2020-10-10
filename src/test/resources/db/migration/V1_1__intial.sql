create table Customer
(
    id       int8 not null,
    email    varchar(255),
    userName varchar(255),
    primary key (id)
);


insert into Customer (id, email, userName)
values (1, 'admin@mail.de', 'Admin');
