package me.rl24.utility;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClassUtil {

    public interface Type<T> {
        T parse(Object o);
    }

    private static final Map<List<Class>, Function<String, Object>> PARSERS = new HashMap<List<Class>, Function<String, Object>>() {{
        put(Arrays.asList(Boolean.TYPE, Boolean.class), Boolean::parseBoolean);
        put(Arrays.asList(Byte.TYPE, Byte.class), Byte::parseByte);
        put(Arrays.asList(Character.TYPE, Character.class), v -> v.charAt(0));
        put(Arrays.asList(Short.TYPE, Short.class), Short::parseShort);
        put(Arrays.asList(Integer.TYPE, Integer.class), Integer::parseInt);
        put(Arrays.asList(Long.TYPE, Long.class), Long::parseLong);
        put(Arrays.asList(Float.TYPE, Float.class), Float::parseFloat);
        put(Arrays.asList(Double.TYPE, Double.class), Double::parseDouble);
    }};

    public static List<Field> getFieldsRecursiveSuper(List<Field> fields, Class<?> cls) {
        fields.addAll(Arrays.asList(cls.getDeclaredFields()));
        if (cls.getSuperclass() != null)
            fields = getFieldsRecursiveSuper(fields, cls.getSuperclass());
        return fields;
    }

    public static List<Field> getNonTransientFieldsRecursiveSuper(List<Field> fields, Class<?> cls) {
        fields.addAll(Arrays.stream(cls.getDeclaredFields()).filter(f -> !Modifier.isTransient(f.getModifiers())).collect(Collectors.toList()));
        if (cls.getSuperclass() != null)
            fields = getNonTransientFieldsRecursiveSuper(fields, cls.getSuperclass());
        return fields;
    }

    public static List<Field> getFieldsAnnotatedWithRecursiveSuper(List<Field> fields, Class<?> cls, Class<? extends Annotation> annotation) {
        fields.addAll(Arrays.stream(cls.getDeclaredFields()).filter(f -> f.isAnnotationPresent(annotation)).collect(Collectors.toList()));
        if (cls.getSuperclass() != null)
            fields = getFieldsAnnotatedWithRecursiveSuper(fields, cls.getSuperclass(), annotation);
        return fields;
    }

    public static boolean isRecursive(Class<?> cls, boolean or, Class<?>... classes) {
        for (Class<?> c : classes) {
            boolean equal = isRecursive(cls, c);
            if (!equal && !or)
                return false;
            else if (equal && or)
                return true;
        }
        return false;
    }

    public static boolean isRecursive(Class<?> classA, Class<?> classB) {
        return classA == classB || (classA.getSuperclass() != null && isRecursive(classA.getSuperclass(), classB)) || Arrays.asList(classA.getInterfaces()).contains(classB);
    }

    public static Object parseTo(Class cls, Object instance, String value) {
        try {
            for (List<Class> classes : PARSERS.keySet())
                for (Class c : classes)
                    if (isRecursive(cls, c))
                        return PARSERS.get(classes).apply(value);
            if (isRecursive(cls, Enum.class))
                try {
                    return Enum.valueOf(cls, value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            if (isRecursive(cls, Type.class))
                return ((Type) instance).parse(value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
