-- Create trigger to automatically create default profile for new users
DELIMITER //

CREATE TRIGGER after_user_insert_profile
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- Insert default profile
    INSERT INTO user_profiles (user_id, display_name, language, created_at, updated_at)
    VALUES (NEW.id, NEW.username, 'id', NOW(), NOW());
END //

-- Create trigger to automatically create default preferences for new users
CREATE TRIGGER after_user_insert_preferences
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    -- Insert default preferences
    INSERT INTO user_preferences (
        user_id, 
        preferred_language,
        preferred_quality,
        autoplay_enabled,
        subtitles_enabled,
        subtitle_language,
        email_notifications,
        push_notifications,
        created_at,
        updated_at
    )
    VALUES (
        NEW.id,
        'id',
        'AUTO',
        1,
        0,
        'id',
        1,
        1,
        NOW(),
        NOW()
    );
END //

DELIMITER ; 