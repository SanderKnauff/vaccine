package ooo.sansk.vaccine.exception;

import java.util.List;

public class CircularDependencyException extends RuntimeException {
    private final List<Class<?>> parents;
    private final Class<?> child;

    public CircularDependencyException(List<Class<?>> parents, Class<?> child) {
        this.parents = parents;
        this.child = child;
    }

    @Override
    public String getMessage() {
        final var message = new StringBuilder();
        message.append("Circular dependency detected while injecting Components: (");
        for (final var clazz : parents) {
            message.append(clazz.getName()).append(" -> ");
        }
        message.append(child.getName()).append(")");
        return message.toString();
    }
}
