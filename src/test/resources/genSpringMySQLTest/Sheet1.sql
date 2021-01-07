CREATE TABLE genSpringMySQLTest.Sheet1(id BIGINT NOT NULL primary key auto_increment,
Created TIMESTAMP NOT NULL,
Lastmod TIMESTAMP NOT NULL,
`IntField`	INTEGER NOT NULL,
`DateField`	DATETIME,
`Text`	VARCHAR(7) COMMENT 'len=7',
`DecimalField`	REAL);
