package Main;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface FourArgsFunction<A, B, C, D, R> {

    R apply(A a, B b, C c, D d) throws InvocationTargetException, IllegalAccessException;

    default <V> FourArgsFunction<A, B, C, D, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
    }
}
