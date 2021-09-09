create table users (
    id  serial PRIMARY KEY,
    name VARCHAR NOT NULL,
    password bytea NOT NULL,
    salt bytea NOT NULL
);