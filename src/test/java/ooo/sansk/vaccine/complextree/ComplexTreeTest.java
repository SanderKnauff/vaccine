package ooo.sansk.vaccine.complextree;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComplexTreeTest {
    private static final Properties PROPERTIES = new Properties();

    private Vaccine vaccine;

    @BeforeEach
    void setUp() {
        vaccine = new Vaccine();
    }

    @Test
    void testComplexTreeNoDuplicated() {
        vaccine.inject(PROPERTIES, "ooo.sansk.vaccine.complextree");
        final var occurrences = vaccine.getCandidates().stream().collect(Collectors.groupingBy(Object::getClass, Collectors.counting()));

        assertEquals(6, occurrences.size());
        occurrences.forEach((k, v) -> assertEquals(1, (long) v));
    }


    @Component
    public static class ParentA {
        public ParentA(ChildA childA, ChildB childB) {
        }
    }

    @Component
    public static class ParentB {
        public ParentB(ChildC childC) {
        }
    }

    @Component
    public static class ChildA {
        public ChildA(SharedChild sharedChild) {
        }
    }

    @Component
    public static class ChildB {
        public ChildB(SharedChild sharedChild) {
        }
    }

    @Component
    public static class ChildC {
        public ChildC(SharedChild sharedChild) {
        }
    }

    @Component
    public static class SharedChild {
    }
}
