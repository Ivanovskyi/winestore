create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(100) not null,
    role          varchar(20)  not null,
    birth_date    date         not null
);

create table wines (
    id          bigserial primary key,
    name        varchar(200)   not null,
    type        varchar(20)    not null,
    country     varchar(100)   not null,
    region      varchar(100),
    vintage     int,
    price       numeric(10, 2) not null check (price > 0),
    stock       int            not null check (stock >= 0),
    description text,
    version     bigint         not null default 0
);

create table cart_items (
    id       bigserial primary key,
    user_id  bigint not null references users (id) on delete cascade,
    wine_id  bigint not null references wines (id) on delete cascade,
    quantity int    not null check (quantity > 0),
    unique (user_id, wine_id)
);

create table orders (
    id         bigserial primary key,
    user_id    bigint         not null references users (id),
    status     varchar(20)    not null,
    total      numeric(12, 2) not null,
    created_at timestamptz    not null
);

create table order_items (
    id         bigserial primary key,
    order_id   bigint         not null references orders (id) on delete cascade,
    wine_id    bigint         not null,
    wine_name  varchar(200)   not null,
    unit_price numeric(10, 2) not null,
    quantity   int            not null check (quantity > 0)
);

create index idx_wines_type on wines (type);
create index idx_wines_country on wines (country);
create index idx_orders_user on orders (user_id);
