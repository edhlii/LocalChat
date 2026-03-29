CREATE DATABASE localchat;
USE localchat;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50),
    password VARCHAR(50),
    nickname VARCHAR(50),
    role ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER'
);

INSERT INTO users (username, password, nickname, role) 
VALUES ('ducanh', '123', 'Duc Anh', 'MANAGER'),
		('vanquang', '123', 'Quang', 'MEMBER'),
		('dinhhieu', '123', 'Hieu', 'MEMBER');