import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
interface ThreeArgsFunction<A, B, C, R> {

    R apply(A a, B b, C c) throws InvocationTargetException, IllegalAccessException;

    default <V> ThreeArgsFunction<A, B, C, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a, b, c));
    }
}
