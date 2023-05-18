package ooo.sansk.vaccine.circularcomplex;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.exception.CircularDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircularComplexTest {
    private static final Properties PROPERTIES = new Properties();

    private Vaccine vaccine;

    @BeforeEach
    public void setUp() {
        vaccine = new Vaccine();
    }

    //Dependency circle A->B->C->E->D->A
    @Test
    void testPreventCircularDependenciesComplex() {
        final var exception = assertThrows(CircularDependencyException.class, () -> vaccine.inject(PROPERTIES, "ooo.sansk.vaccine.circularcomplex"));

        assertEquals("Circular dependency detected while injecting Components: (ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartA -> ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartB -> ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartC -> ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartE -> ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartD -> ooo.sansk.vaccine.circularcomplex.CircularComplexTest$CircularPartA)", exception.getMessage());
    }

    @Component
    public static class CircularPartA {
        public CircularPartA(CircularPartB part) {
        }
    }

    @Component
    private static class CircularPartB {
        public CircularPartB(CircularPartC part) {
        }
    }

    @Component
    private static class CircularPartC {
        public CircularPartC(CircularPartE part) {
        }
    }

    @Component
    private static class CircularPartD {
        public CircularPartD(CircularPartA part) {
        }
    }

    @Component
    private static class CircularPartE {
        public CircularPartE(CircularPartD part) {
        }
    }
}
