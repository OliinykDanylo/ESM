package Controller;

import java.lang.reflect.Field;

public class ReflectionUtility {
    public static void disableAccessCheck(Field field) {
        try {
            field.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Could not disable access checks on field: " + field.getName(), e);
        }
    }
}