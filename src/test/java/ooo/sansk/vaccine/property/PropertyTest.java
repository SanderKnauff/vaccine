package ooo.sansk.vaccine.property;

import ooo.sansk.vaccine.Vaccine;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.annotation.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class PropertyTest {
    private Vaccine vaccine;

    @BeforeEach
    void setUp() {
        vaccine = new Vaccine();
    }

    @Test
    void testPropertyResolver() {
        final var properties = new Properties();
        final var parentProperty = "parentProperty";
        final var childProperty = "childProperty";
        properties.setProperty("parent", parentProperty);
        properties.setProperty("child", childProperty);
        vaccine.inject(properties, "ooo.sansk.vaccine.property");

        final var parent = (PropertyHolderParent) vaccine.getInjected(PropertyHolderParent.class).orElseThrow(NullPointerException::new);
        final var child = (PropertyHolderChild) vaccine.getInjected(PropertyHolderChild.class).orElseThrow(NullPointerException::new);

        assertNotNull(parent);
        assertNotNull(child);
        assertEquals(child, parent.child());
        assertEquals(parentProperty, parent.property());
        assertEquals(childProperty, child.property());
    }

    @Component
    public record PropertyHolderParent(PropertyHolderChild child, String property) {
        public PropertyHolderParent(PropertyHolderChild child, @Property("parent") String property) {
            this.child = child;
            this.property = property;
        }
    }

    @Component
    public record PropertyHolderChild(String property) {
        public PropertyHolderChild(@Property("child") String property) {
            this.property = property;
        }
    }
}
