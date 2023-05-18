package ooo.sansk.vaccine.unknowndependency;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.exception.UnknownDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnknownDependencyTest {
    private static final Properties PROPERTIES = new Properties();

    private Vaccine vaccine;

    @BeforeEach
    void setUp() {
        vaccine = new Vaccine();
    }

    @Test
    void testUnknownDependencies() {
        final var exception = assertThrows(UnknownDependencyException.class, () -> vaccine.inject(PROPERTIES, "ooo.sansk.vaccine.unknowndependency"));

        assertEquals("No injection candidates of type interface ooo.sansk.vaccine.unknowndependency.UnknownDependencyTest$UnknownComponent found to inject in class ooo.sansk.vaccine.unknowndependency.UnknownDependencyTest$KnownComponent", exception.getMessage());
    }

    @Component
    public static class KnownComponent {
        public KnownComponent(UnknownComponent unknownComponent) {
        }
    }

    private interface UnknownComponent {
    }
}
