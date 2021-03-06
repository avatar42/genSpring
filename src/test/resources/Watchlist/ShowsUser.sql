CREATE TABLE Watchlist.ShowsUser(id BIGINT NOT NULL primary key auto_increment,
`LastWatched`	VARCHAR(10) COMMENT 'len=10' NOT NULL,
`BestExperience`	VARCHAR(17) COMMENT 'len=17',
`Comment`	VARCHAR(187) COMMENT 'len=187',
`Imdb`	VARCHAR(2) COMMENT 'len=2',
`Ota`	VARCHAR(1) COMMENT 'len=1',
`Userid`	BIGINT,
`ShowsId`	BIGINT,
`N67`	VARCHAR(3) COMMENT 'len=3',
`InShowRssAs`	VARCHAR(47) COMMENT 'len=47',
`TabloLink`	VARCHAR(6) COMMENT 'len=6',
`InRokuFeed`	VARCHAR(1) COMMENT 'len=1',
`JustWatch`	VARCHAR(2) COMMENT 'len=2',
CONSTRAINT FK_ShowsUser_ShowsId FOREIGN KEY (ShowsId)    REFERENCES Watchlist.Shows(Id),
CONSTRAINT FK_ShowsUser_Userid FOREIGN KEY (Userid)    REFERENCES Watchlist.Account(Id));
