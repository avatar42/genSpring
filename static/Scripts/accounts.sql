DROP TABLE IF EXISTS account;
CREATE TABLE account (id bigint not null, created timestamp, email varchar, password varchar, role varchar, primary key (id));

INSERT INTO account (id,created,email,password,"role") VALUES (
1,1592425095850,'user','$2a$10$5twbWyhL0OZnw/PZ43nK.OGMZ7QtALBzPZhowVd39LFuW1NPguN7a','ROLE_USER');
INSERT INTO account (id,created,email,password,"role") VALUES (
2,1592425096102,'admin','$2a$10$fJ.I0N1JX8oFMNmPkLon2uM.XELhVJy6qpkcHwpdcmtzMhIOTNxEm','ROLE_ADMIN');

DROP TABLE IF EXISTS "hibernate_sequence";
CREATE TABLE hibernate_sequence (next_val bigint);
INSERT INTO hibernate_sequence (next_val) VALUES (
5);
INSERT INTO hibernate_sequence (next_val) VALUES (
5);

SELECT * from account;
