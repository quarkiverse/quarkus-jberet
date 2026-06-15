create table Auctions
(
    id             bigint not null,
    bid            bigint,
    buyout         bigint,
    itemId         integer,
    quantity       integer,
    primary key (id)
);

create table AuctionStatistics
(
    id        bigint not null,
    itemId    integer,
    quantity  bigint,
    bid       bigint,
    minBid    bigint,
    maxBid    bigint,
    buyout    bigint,
    minBuyout bigint,
    maxBuyout bigint,
    avgBid    double precision,
    avgBuyout double precision,
    timestamp bigint,
    primary key (id)
);

create sequence auction_statistics_id start with 1 increment by 1;
