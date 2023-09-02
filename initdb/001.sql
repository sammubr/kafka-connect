CREATE TABLE state (
id bigint NOT NULL AUTO_INCREMENT,
created_by varchar(50) NOT NULL,
created_date datetime NOT NULL,
last_modified_by varchar(50) NULL,
last_modified_date datetime NULL,
description varchar(100) NOT NULL,
PRIMARY KEY (id),
UNIQUE KEY uk_state_description (description)
);
create schema test2;