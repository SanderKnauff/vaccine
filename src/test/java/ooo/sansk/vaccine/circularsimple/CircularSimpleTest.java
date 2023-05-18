package ooo.sansk.vaccine.circularsimple;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.exception.CircularDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircularSimpleTest {
    private static final Properties PROPERTIES = new Properties();

    private Vaccine vaccine;

    @BeforeEach
    void setUp() {
        vaccine = new Vaccine();
    }

    @Test
    void testPreventCircularDependenciesSimple() {
        final var exception = assertThrows(CircularDependencyException.class, () -> vaccine.inject(PROPERTIES, "ooo.sansk.vaccine.circularsimple"));

        assertEquals("Circular dependency detected while injecting Components: (ooo.sansk.vaccine.circularsimple.CircularSimpleTest$CircularHalf -> ooo.sansk.vaccine.circularsimple.CircularSimpleTest$CircularOtherHalf -> ooo.sansk.vaccine.circularsimple.CircularSimpleTest$CircularHalf)", exception.getMessage());
    }

    @Component
    public static class CircularHalf {
        public CircularHalf(CircularOtherHalf otherHalf) {
        }
    }

    @Component
    private static class CircularOtherHalf {
        public CircularOtherHalf(CircularHalf half) {
        }
    }
}
