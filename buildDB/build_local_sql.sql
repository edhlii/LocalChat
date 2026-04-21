CREATE DATABASE localchat;
USE localchat;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50), NOT NULL, UNIQUE
    password VARCHAR(50), NOT NULL,
    nickname VARCHAR(50), NOT NULL,
    role ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER'
);

INSERT INTO users (username, password, nickname, role) 
VALUES ('ducanh', '123', 'Duc Anh', 'MANAGER'),
		('vanquang', '123', 'Quang', 'MEMBER'),
		('dinhhieu', '123', 'Hieu', 'MEMBER'),
		('vantoan', '123', 'Van Toan', 'MANAGER');



----------------------
--Mở DB Bôi đen Đoạn này và chạy
USE localchat;
DROP TABLE users;
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    `role` ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER'
);
INSERT INTO users (username, password, nickname, role)
VALUES ('ducanh', '$2a$12$AwqraqEAznNZdB7lVomfCOJ8i7Y5XRRuNTFFWLIYTby2tDL50FkDS', 'Duc Anh', 'MANAGER'),
		('vanquang', '$2a$12$ZgdLhWDxfvT09/UjWG/wlubDnO5rVXTh0pou.XAB43BNMUvB2CIqa', 'Quang', 'MEMBER'),
		('dinhhieu', '$2a$12$xUVOxsIFoDBh6lWRAgL10uaCvUo1Zw1DJ7XU3xsOFhOV08SyhAPWO', 'Hieu', 'MEMBER'),
		('vantoan', '$2a$12$NQmQoGxVptAY0sqUk5t5C.hY2EjKVcQEArWR9LpVT8zpVRrUQeU7G', 'Van Toan', 'MANAGER');

--------
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT DEFAULT NULL,


    message_type ENUM('TEXT', 'IMAGE', 'FILE') DEFAULT 'TEXT',
    content TEXT,


    file_path VARCHAR(512),
    file_name VARCHAR(255),
    file_size BIGINT,

    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,


    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,


    INDEX idx_chat_lookup (sender_id, receiver_id, sent_at)
);