create sequence hibernate_sequence start 1 increment 1;

create table Customer
(
    id       int8 not null,
    email    varchar(255),
    userName varchar(255),
    primary key (id)
);


insert into Customer (id, email, userName)
values (nextval('hibernate_sequence'), 'admin@mail.de', 'Admin');

