CREATE TABLE Sheet1User(id INTEGER NOT NULL primary key autoincrement,
Userid	INTEGER,
Sheet1Id	INTEGER,
UserYN	VARCHAR(1),
CONSTRAINT FK_Sheet1User_Sheet1Id FOREIGN KEY (Sheet1Id)    REFERENCES Sheet1(Id),
CONSTRAINT FK_Sheet1User_Userid FOREIGN KEY (Userid)    REFERENCES Account(Id));
