package ooo.sansk.vaccine;

import ooo.sansk.vaccine.annotation.AfterCreate;
import ooo.sansk.vaccine.annotation.Component;
import ooo.sansk.vaccine.annotation.Property;
import ooo.sansk.vaccine.annotation.Provided;
import ooo.sansk.vaccine.exception.CircularDependencyException;
import ooo.sansk.vaccine.exception.ConstructorStalemateException;
import ooo.sansk.vaccine.exception.DependencyInstantiationException;
import ooo.sansk.vaccine.exception.PackageLoadFailedException;
import ooo.sansk.vaccine.exception.UnknownDependencyException;
import ooo.sansk.vaccine.model.ComponentDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Vaccine {
    private static final Logger logger = LoggerFactory.getLogger(Vaccine.class);

    private final List<Object> candidates = new ArrayList<>();

    private List<ComponentDependency> dependencies = new ArrayList<>();
    private Properties properties;

    public void inject(Properties properties, String basePackage) {
        inject(properties, basePackage, Thread.currentThread().getContextClassLoader());
    }

    public void inject(Properties properties, String basePackage, ClassLoader classLoader) throws UnknownDependencyException {
        this.properties = properties;

        logger.info("Initializing Injection");
        final var classes = scanForComponentClasses(classLoader, basePackage);

        dependencies = scanClassesForCandidates(classes);

        for (final var componentDependency : dependencies) {
            if (isDependencyNotCreated(componentDependency)) {
                resolveDependency(componentDependency);
            }
        }

        logger.info("Found following components:");
        dependencies.forEach(dependency -> logger.info(dependency.getType().getName()));
    }

    private boolean isDependencyNotCreated(ComponentDependency dependency) {
        for (final var candidate : dependencies) {
            if (candidate.getClass().equals(dependency.getType())) {
                return false;
            }
        }
        return true;
    }

    private List<Class<?>> scanForComponentClasses(ClassLoader classLoader, String basePackage) {
        try {
            final var list = new ArrayList<Class<?>>();
            for (final var c : PackageScanner.getClassesForPackage(classLoader, basePackage)) {
                if (c.isAnnotationPresent(Component.class)) {
                    list.add(c);
                }
            }
            return list;
        } catch (ClassNotFoundException e) {
            throw new PackageLoadFailedException("Could not load packages", e);
        }
    }

    private List<ComponentDependency> scanClassesForCandidates(List<Class<?>> classes) {
        final var list = new ArrayList<ComponentDependency>();
        for (final var aClass : classes) {
            ComponentDependency injectionDetails = getInjectionDetails(aClass);
            list.add(injectionDetails);
        }
        return list;
    }

    private ComponentDependency getInjectionDetails(Class<?> clazz) {
        final var constructors = clazz.getConstructors();
        if (constructors.length > 1) {
            throw new ConstructorStalemateException(clazz);
        }
        final var foundDependencies = new ArrayList<Class<?>>();
        for (final var parameter : constructors[0].getParameters()) {
            if (!parameter.isAnnotationPresent(Property.class)) {
                foundDependencies.add(parameter.getType());
            }
        }
        return new ComponentDependency(clazz, foundDependencies.toArray(new Class[0]), getProvidedComponentsFromClass(clazz));
    }

    private Class<?>[] getProvidedComponentsFromClass(Class<?> clazz) {
        final var list = new ArrayList<Class<?>>();
        for (final var method : clazz.getMethods()) {
            if (!method.isAnnotationPresent(Provided.class)) {
                continue;
            }

            if (method.getParameterCount() != 0) {
                throw new IllegalArgumentException("Provided can only be used on methods without parameters");
            }

            list.add(method.getReturnType());
        }
        return list.toArray(new Class[0]);
    }

    private void resolveDependency(ComponentDependency dependency) {
        dependency.setObject(createOrGetCandidateInstance(dependency.getType(), new ArrayList<>()));
    }

    private Object createOrGetCandidateInstance(Class<?> candidate, List<Class<?>> parents) {
        for (final var createdInstance : candidates) {
            if (candidate.equals(createdInstance.getClass())) {
                return createdInstance;
            }
        }

        final var instance = createInstanceFromCandidate(candidate, parents);
        candidates.add(instance);
        runAfterCreation(instance);
        return instance;
    }

    private Object createInstanceFromCandidate(Class<?> candidate, List<Class<?>> parents) {
        if (parents.contains(candidate)) {
            throw new CircularDependencyException(parents, candidate);
        }
        if (!classHasSingleConstructor(candidate)) {
            throw new ConstructorStalemateException(candidate);
        }

        final var constructor = candidate.getConstructors()[0];
        try {
            return createObjectInstance(candidate, parents, constructor);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new DependencyInstantiationException("Could not create Instance for " + candidate.getName(), e);
        }
    }

    private Object createObjectInstance(
            Class<?> candidate,
            List<Class<?>> parents,
            Constructor<?> constructor
    ) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        if (constructorHasNoParameters(constructor)) {
            return constructor.newInstance();
        }

        final var constructorParameters = new ArrayList<>(constructor.getParameterCount());
        for (final var parameter : constructor.getParameters()) {
            if (parameter.isAnnotationPresent(Property.class)) {
                constructorParameters.add(resolvePropertyDependency(parameter));
                continue;
            }

            final var parameterType = parameter.getType();
            if (parameterType.isAnnotationPresent(Component.class)) {
                parents.add(candidate);
                constructorParameters.add(createOrGetCandidateInstance(parameterType, parents));
            } else {
                final var provider = getProvider(parameterType);
                if (provider.isPresent()) {
                    parents.add(candidate);
                    Object providerInstance = createOrGetCandidateInstance(provider.get(), parents);
                    constructorParameters.add(searchAndCreateProviderInstance(providerInstance, parameterType));
                } else {
                    throw new UnknownDependencyException(candidate, parameterType);
                }
            }
        }
        return constructor.newInstance(constructorParameters.toArray());
    }

    private boolean constructorHasNoParameters(Constructor<?> constructor) {
        return constructor.getParameterCount() == 0;
    }

    private boolean classHasSingleConstructor(Class<?> candidate) {
        return candidate.getConstructors().length == 1;
    }

    private Object searchAndCreateProviderInstance(
            Object providerInstance,
            Class<?> requestedType
    ) throws IllegalAccessException, InvocationTargetException {
        for (Object candidate : candidates) {
            if (requestedType.isInstance(candidate)) {
                return candidate;
            }
        }

        for (final var method : providerInstance.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Provided.class) || !method.getReturnType().equals(requestedType)) {
                continue;
            }

            if (method.getParameterCount() != 0) {
                throw new IllegalArgumentException("Provided can only be used on methods without parameters");
            }

            final var providedObject = method.invoke(providerInstance);
            candidates.add(providedObject);
            return providedObject;
        }

        return null;
    }

    private String resolvePropertyDependency(Parameter parameter) {
        return (String) properties.get(parameter.getAnnotation(Property.class).value());
    }

    private void runAfterCreation(Object injectable) {
        for (final var method : injectable.getClass().getMethods()) {
            if (!method.isAnnotationPresent(AfterCreate.class)) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                throw new IllegalArgumentException("Methods annotated with @AfterCreate can not have parameters");
            }
            try {
                method.invoke(injectable);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.error("Could not run @PostConstruct for {}", injectable.getClass().getName(), e);
            }
        }
    }

    private Optional<Class<?>> getProvider(Class<?> type) {
        for (final var dependency : dependencies) {
            if (Arrays.asList(dependency.getProvidedClasses()).contains(type)) {
                return Optional.of(dependency.getType());
            }
        }
        return Optional.empty();
    }


    public Optional<Object> getInjected(Class<?> type) {
        for (final var candidate : candidates) {
            if (candidate.getClass().equals(type)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public List<Object> getCandidates() {
        return candidates;
    }
}
