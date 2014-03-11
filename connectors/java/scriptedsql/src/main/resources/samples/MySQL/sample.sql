CREATE DATABASE IF NOT EXISTS SAMPLE CHARACTER SET utf8 COLLATE utf8_bin;
USE SAMPLE;

DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Groups;
DROP TABLE IF EXISTS Organizations;

CREATE TABLE Users(
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	uid char(32) NOT NULL,
	firstname varchar(32) NOT NULL default '',
	lastname varchar(32) NOT NULL default '',
	displayName varchar(64),
	email varchar(64),
	employeeNumber varchar(32),
	employeeType varchar(32),
	description varchar(64),
	mobilePhone varchar(64),
	timestamp TIMESTAMP
);

CREATE TABLE Groups(
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	gid char(32) NOT NULL,
	name varchar(32) NOT NULL default '',
	description varchar(32),
	timestamp TIMESTAMP
);

CREATE TABLE Organizations(
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name varchar(32) NOT NULL default '',
	description varchar(32),
	timestamp TIMESTAMP
);
INSERT INTO Users VALUES ("","bob","Bob", "Fleming","Bob Fleming","Bob.Fleming@example.com","100","employee","Employee Bob Fleming","123-456-789",CURRENT_TIMESTAMP);
INSERT INTO Users VALUES ("","rowley","Rowley","Birkin","Rowley Birkin","Rowley.Birkin@example.com","101","employee","Employee Rowley Birkin","345-234-543",CURRENT_TIMESTAMP);
INSERT INTO Users VALUES ("","louis","Louis", "Balfour","Louis Balfour","Louis.Balfour@example.com","102","employee","Employee Louis Balfour","876-678-098",CURRENT_TIMESTAMP);
INSERT INTO Users VALUES ("","john","John", "Smith","John Smith","John.Smith@example.com","103","employee","Employee John Smith","634-123-456",CURRENT_TIMESTAMP);
INSERT INTO Users VALUES ("","jdoe","John", "Doe","John Doe","John.Doe@example.com","104","employee","Employee John Doe","125-785-645",CURRENT_TIMESTAMP);
INSERT INTO Users VALUES ("","chris","Chris", "Crafty","Chris Crafty","Chris.Crafty@example.com","105","contractor","Employee Chris Crafty","888-666-555",CURRENT_TIMESTAMP);

INSERT INTO Groups VALUES ("","100","admin","Admin group",CURRENT_TIMESTAMP);
INSERT INTO Groups VALUES ("","101","users","Users group",CURRENT_TIMESTAMP);
INSERT INTO Groups VALUES ("","102","security","Security group",CURRENT_TIMESTAMP);

INSERT INTO Organizations VALUES ("","HR","HR organization",CURRENT_TIMESTAMP);
INSERT INTO Organizations VALUES ("","SALES","Sales organization",CURRENT_TIMESTAMP);
INSERT INTO Organizations VALUES ("","SUPPORT","Support organization",CURRENT_TIMESTAMP);
INSERT INTO Organizations VALUES ("","ENG","Engineering organization",CURRENT_TIMESTAMP);
INSERT INTO Organizations VALUES ("","OPERATIONS","Operations organization",CURRENT_TIMESTAMP);

grant all on *.* to root@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES on openidm.* TO openidm IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@'%' IDENTIFIED BY 'openidm';
GRANT ALL PRIVILEGES on openidm.* TO openidm@localhost IDENTIFIED BY 'openidm';

