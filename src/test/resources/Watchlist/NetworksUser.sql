CREATE TABLE Watchlist.NetworksUser(id BIGINT NOT NULL primary key auto_increment,
`IUseFReeFreeWithCAblePAyForHUluInstead`	VARCHAR(1) COMMENT 'len=1',
`Userid`	BIGINT,
`NetworksId`	BIGINT,
CONSTRAINT FK_NetworksUser_Userid FOREIGN KEY (Userid)    REFERENCES Watchlist.Account(Id),
CONSTRAINT FK_NetworksUser_NetworksId FOREIGN KEY (NetworksId)    REFERENCES Watchlist.Networks(Id));
