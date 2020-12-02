CREATE TABLE Sheet1User(id INTEGER NOT NULL primary key autoincrement,
UserYN	VARCHAR(1),
Sheet1Id	INTEGER,
Userid	INTEGER,    CONSTRAINT FK_Sheet1User_Sheet1Id FOREIGN KEY (Sheet1Id)    REFERENCES Sheet1(id),    CONSTRAINT FK_Sheet1User_Userid FOREIGN KEY (Userid)    REFERENCES Account(id));
