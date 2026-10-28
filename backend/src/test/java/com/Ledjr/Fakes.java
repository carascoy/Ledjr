package com.Ledjr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * test helpers. fake() makes a stand-in for a repository (or any interface) with plain java,
 * no mocking library: the methods you name in `answers` return what you give them, everything
 * else returns an empty list / Optional.empty() / false / 0 / null.
 * (skipped mockito on purpose: it builds classes with byte buddy, which has to support the exact
 * java version, and java 25 is newer than what spring boot 3.5's version knows)
 */
public final class Fakes {

    private Fakes(){}

    public static <T> T fake(Class<T> type, Map<String, Function<Object[], Object>> answers){
        return fake(type, answers, new ArrayList<>());
    }

    /** same, but every call gets written to `calls` as "Type.method" so a test can check the order */
    public static <T> T fake(Class<T> type, Map<String, Function<Object[], Object>> answers, List<String> calls){
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> self == args[0];
                    case "hashCode" -> System.identityHashCode(self);
                    default -> "Fake" + type.getSimpleName();
                };
            }
            calls.add(type.getSimpleName().replace("Repository", "") + "." + method.getName());
            Function<Object[], Object> answer = answers.get(method.getName());
            if (answer != null) return answer.apply(args == null ? new Object[0] : args);
            return defaultFor(method.getReturnType());
        });
        return type.cast(proxy);
    }

    private static Object defaultFor(Class<?> type){
        if (type == List.class) return List.of();
        if (type == Optional.class) return Optional.empty();
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        return null;
    }

    /** money compared by value, so 150 and 150.00 count as equal */
    public static void assertMoney(String expected, BigDecimal actual){
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected $" + expected + " but was $" + actual);
    }
}
