CREATE TABLE NetworksUser(id INTEGER NOT NULL primary key autoincrement,
IUseFReeFreeWithCAblePAyForHUluInstead	VARCHAR(1),
Userid	INTEGER,
NetworksId	INTEGER,
CONSTRAINT FK_NetworksUser_Userid FOREIGN KEY (Userid)    REFERENCES Account(Id),
CONSTRAINT FK_NetworksUser_NetworksId FOREIGN KEY (NetworksId)    REFERENCES Networks(Id));
