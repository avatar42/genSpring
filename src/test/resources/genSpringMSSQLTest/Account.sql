CREATE TABLE genSpringMSSQLTest.dbo.Account(id BIGINT IDENTITY(1, 1) PRIMARY KEY, Created DATETIME NOT NULL, Lastmod DATETIME NOT NULL,
Password	VARCHAR(254) NOT NULL,
Role	VARCHAR(25) NOT NULL,
Email	VARCHAR(254) NOT NULL, CONSTRAINT UC_email UNIQUE (email));