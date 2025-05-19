-- Create trigger to automatically assign default USER role to new users
DELIMITER //

CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- Insert default USER role
    INSERT INTO user_roles (user_id, role, is_active, assigned_at)
    VALUES (NEW.id, 'USER', 1, NOW());
END //

DELIMITER ; 