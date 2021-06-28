CREATE TABLE Watchlist.Cablecard(id BIGINT NOT NULL primary key auto_increment,
`Userid`	BIGINT,
`ChannelNumber`	INTEGER NOT NULL,
`ChannelName`	VARCHAR(12) COMMENT 'len=12' NOT NULL,
`Receiving`	VARCHAR(3) COMMENT 'len=3',
`LangField`	VARCHAR(2) COMMENT 'len=2',
`Hd`	INTEGER,
`Dt`	INTEGER,
`ShortField`	VARCHAR(12) COMMENT 'len=12',
`InNpl`	INTEGER,
`Net`	VARCHAR(12) COMMENT 'len=12',
CONSTRAINT FK_Cablecard_Userid FOREIGN KEY (Userid)    REFERENCES Watchlist.Account(Id));
