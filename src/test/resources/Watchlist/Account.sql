CREATE TABLE Watchlist.Account(id BIGINT NOT NULL primary key auto_increment,
`Email`	VARCHAR(254) COMMENT 'len=254' NOT NULL,
`Name`	VARCHAR(254) COMMENT 'len=254' NOT NULL,
`Password`	VARCHAR(254) COMMENT 'len=254' NOT NULL,
`Userrole`	VARCHAR(25) COMMENT 'len=25' NOT NULL,
CONSTRAINT UC_Email UNIQUE (Email));
