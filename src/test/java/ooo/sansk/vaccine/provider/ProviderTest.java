package ooo.sansk.vaccine.provider;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.annotation.Provided;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProviderTest {
    private static final Properties PROPERTIES = new Properties();

    private Vaccine vaccine;

    @BeforeEach
    void setUp() {
        vaccine = new Vaccine();
    }

    @Test
    void testProviderAnnotation() {
        vaccine.inject(PROPERTIES, "ooo.sansk.vaccine.provider");

        assertNotNull(((ParentB) vaccine.getInjected(ParentB.class).orElseThrow(null)).childProvided());
    }

    @Component
    public static class ParentA {
        public ParentA(ChildInjected childInjected) {
        }
    }

    @Component
        public record ParentB(ChildProvided childProvided) {
    }

    @Component
    public static class ChildInjected {
        @Provided
        public ChildProvided childProvided() {
            return new ChildProvided();
        }
    }

    public static class ChildProvided {
        private ChildProvided() {
        }
    }

}
