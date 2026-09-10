CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    avatar_color VARCHAR(20) DEFAULT '#0d6efd',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    total_amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE expense_payers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount_paid DECIMAL(15, 2) NOT NULL,
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE expense_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

INSERT INTO members (name, avatar_color) VALUES 
('Thành viên 1', '#0d6efd'), ('Thành viên 2', '#6610f2'), 
('Thành viên 3', '#6f42c1'), ('Thành viên 4', '#d63384'), 
('Thành viên 5', '#dc3545'), ('Thành viên 6', '#fd7e14'), 
('Thành viên 7', '#198754');