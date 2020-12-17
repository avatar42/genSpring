CREATE TABLE RoamioTodo(id INTEGER NOT NULL primary key autoincrement,
Date	DATETIME NOT NULL,
ShowTrimmed	VARCHAR(47),
Channel	VARCHAR(15),
ShowName	VARCHAR(48),
Ep	VARCHAR(50),
Userid	INTEGER,
Duration	VARCHAR(4),
EpisodeName	VARCHAR(59),    CONSTRAINT FK_RoamioTodo_Userid FOREIGN KEY (Userid)    REFERENCES Account(id));
