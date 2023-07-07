-- Add Users

-- !Ups

create table users
(
    id            UUID PRIMARY KEY,
    username      varchar not null UNIQUE,
    first_name    varchar not null,
    last_name     varchar not null,
    mail          varchar not null,
    employee_type varchar not null
);



-- !Downs

drop table "users" if exists;
