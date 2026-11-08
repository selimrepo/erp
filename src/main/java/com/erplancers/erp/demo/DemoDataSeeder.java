package com.erplancers.erp.demo;

import com.github.javafaker.Faker;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

/**
 * Generates deterministic-in-shape, disposable demo records for every mapped JPA entity.
 * It is deliberately disabled unless erp.demo-data.enabled=true is configured.
 */
@Component
@ConditionalOnProperty(prefix = "erp.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo123!";
    private final EntityManager entityManager;
    private final int recordsPerEntity;
    private final Faker faker = new Faker(new Locale("tr"));
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<Class<?>, Object> knownEntities = new HashMap<>();

    public DemoDataSeeder(
            EntityManager entityManager,
            @Value("${erp.demo-data.records-per-entity:3}") int recordsPerEntity) {
        this.entityManager = entityManager;
        this.recordsPerEntity = Math.max(1, recordsPerEntity);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<EntityType<?>> entities = new ArrayList<>(entityManager.getMetamodel().getEntities());
        entities.sort(Comparator.comparing(EntityType::getName));

        // Reuse existing rows as relationship targets and avoid duplicating populated tables.
        for (EntityType<?> entity : entities) {
            Object existing = findOne(entity.getJavaType());
            if (existing != null) {
                knownEntities.put(entity.getJavaType(), existing);
            }
        }

        Set<EntityType<?>> pending = new LinkedHashSet<>(entities);
        int inserted = 0;
        boolean progress;
        do {
            progress = false;
            Iterator<EntityType<?>> iterator = pending.iterator();
            while (iterator.hasNext()) {
                EntityType<?> entity = iterator.next();
                Class<?> type = entity.getJavaType();
                if (countRows(entity) > 0) {
                    iterator.remove();
                    progress = true;
                    continue;
                }

                Object first = null;
                boolean deferred = false;
                for (int index = 0; index < recordsPerEntity; index++) {
                    Object sample = instantiate(type);
                    if (!populate(sample, type, index)) {
                        deferred = true;
                        break;
                    }
                    entityManager.persist(sample);
                    entityManager.flush();
                    if (first == null) {
                        first = sample;
                    }
                    inserted++;
                }

                if (!deferred) {
                    knownEntities.put(type, first);
                    iterator.remove();
                    progress = true;
                }
            }
        } while (progress && !pending.isEmpty());

        if (!pending.isEmpty()) {
            log.warn("Demo data skipped {} entities because required relationship targets could not be created: {}",
                    pending.size(), pending.stream().map(EntityType::getName).toList());
        }
        log.info("Demo data generation completed: {} records inserted across {} JPA entities.",
                inserted, entities.size() - pending.size());
    }

    private long countRows(EntityType<?> entity) {
        return entityManager.createQuery(
                "select count(e) from " + entity.getName() + " e", Long.class)
                .getSingleResult();
    }

    private Object findOne(Class<?> type) {
        try {
            return entityManager.createQuery("select e from " + entityName(type) + " e", type)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String entityName(Class<?> type) {
        return entityManager.getMetamodel().entity(type).getName();
    }

    private Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("A no-argument constructor is required for demo entity " + type.getName(), e);
        }
    }

    private boolean populate(Object target, Class<?> type, int index) {
        for (Field field : fieldsOf(type)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                    || field.isSynthetic() || field.isAnnotationPresent(Id.class)
                    || field.isAnnotationPresent(EmbeddedId.class)
                    || field.isAnnotationPresent(Version.class)) {
                continue;
            }
            field.setAccessible(true);
            try {
                if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                    Class<?> relatedType = field.getType();
                    Object related = knownEntities.getOrDefault(relatedType, findOne(relatedType));
                    boolean required = isRequired(field);
                    if (related == null && required) {
                        return false;
                    }
                    if (related != null) {
                        field.set(target, related);
                    }
                    continue;
                }
                if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)
                        || field.isAnnotationPresent(ElementCollection.class)) {
                    field.set(target, collectionValue(field, type));
                    continue;
                }
                if (field.isAnnotationPresent(Embedded.class)) {
                    continue;
                }
                Object value = fakeValue(field.getName(), field.getType(), index);
                if (value != null || field.getType().isPrimitive()) {
                    field.set(target, value);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot populate " + type.getName() + "." + field.getName(), e);
            }
        }
        return true;
    }

    private boolean isRequired(Field field) {
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null && !manyToOne.optional()) {
            return true;
        }
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null && !oneToOne.optional()) {
            return true;
        }
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        return joinColumn != null && !joinColumn.nullable();
    }

    private Object collectionValue(Field field, Class<?> ownerType) {
        if (Set.class.isAssignableFrom(field.getType())) {
            if (field.getName().toLowerCase(Locale.ROOT).contains("role")) {
                return new LinkedHashSet<>(Set.of("ROLE_ADMIN", "ROLE_USER"));
            }
            return new LinkedHashSet<>();
        }
        if (List.class.isAssignableFrom(field.getType()) || Collection.class.isAssignableFrom(field.getType())) {
            return new ArrayList<>();
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return new LinkedHashMap<>();
        }
        return null;
    }

    private Object fakeValue(String fieldName, Class<?> type, int index) {
        String name = fieldName.toLowerCase(Locale.ROOT);
        if (type == String.class) {
            if (name.contains("password")) {
                return passwordEncoder.encode(DEMO_PASSWORD);
            }
            if (name.contains("email")) {
                return faker.internet().emailAddress();
            }
            if (name.contains("phone") || name.contains("mobile") || name.contains("fax")) {
                return faker.phoneNumber().phoneNumber();
            }
            if (name.contains("firstname") || name.equals("name") || name.contains("contactname")) {
                return faker.name().firstName();
            }
            if (name.contains("lastname") || name.contains("surname")) {
                return faker.name().lastName();
            }
            if (name.contains("username")) {
                return "demo-" + faker.name().username() + "-" + index;
            }
            if (name.contains("address") || name.contains("street")) {
                return faker.address().streetAddress();
            }
            if (name.contains("city")) {
                return faker.address().city();
            }
            if (name.contains("country")) {
                return faker.address().country();
            }
            if (name.contains("postal") || name.contains("zip")) {
                return faker.address().zipCode();
            }
            if (name.contains("url") || name.contains("website")) {
                return faker.internet().url();
            }
            if (name.equals("code") || name.endsWith("code") || name.contains("number")) {
                return "DMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            }
            if (name.contains("description") || name.contains("comment") || name.contains("note")
                    || name.contains("content") || name.contains("message")) {
                return faker.lorem().sentence(12);
            }
            if (name.contains("title") || name.contains("subject") || name.contains("name")
                    || name.contains("label") || name.contains("company")) {
                return faker.company().name();
            }
            if (name.contains("currency")) {
                return "TRY";
            }
            if (name.contains("language")) {
                return "tr";
            }
            if (name.contains("timezone")) {
                return "Europe/Istanbul";
            }
            if (name.contains("salutation")) {
                return faker.options().option("Sayın", "Sn.", "Merhaba");
            }
            return faker.lorem().word();
        }
        if (type == boolean.class || type == Boolean.class) {
            return faker.bool().bool();
        }
        if (type == int.class || type == Integer.class) {
            return faker.number().numberBetween(1, 1000);
        }
        if (type == long.class || type == Long.class) {
            return faker.number().numberBetween(100L, 100_000L);
        }
        if (type == double.class || type == Double.class) {
            return faker.number().randomDouble(2, 1, 100_000);
        }
        if (type == float.class || type == Float.class) {
            return (float) faker.number().randomDouble(2, 1, 100_000);
        }
        if (type == short.class || type == Short.class) {
            return (short) faker.number().numberBetween(1, 100);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) faker.number().numberBetween(1, 100);
        }
        if (type == BigDecimal.class) {
            return BigDecimal.valueOf(faker.number().randomDouble(2, 1, 100_000));
        }
        if (type == BigInteger.class) {
            return BigInteger.valueOf(faker.number().numberBetween(1L, 100_000L));
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == LocalDate.class) {
            return LocalDate.now().minusDays(faker.number().numberBetween(0, 365));
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.now().minusDays(faker.number().numberBetween(0, 365));
        }
        if (type == OffsetDateTime.class) {
            return OffsetDateTime.now().minusDays(faker.number().numberBetween(0, 365));
        }
        if (type == Instant.class) {
            return Instant.now().minusSeconds(faker.number().numberBetween(0L, 31_536_000L));
        }
        if (type == Date.class) {
            return Date.from(Instant.now().minusSeconds(faker.number().numberBetween(0L, 31_536_000L)));
        }
        if (type == LocalTime.class) {
            return LocalTime.of(faker.number().numberBetween(8, 18), faker.number().numberBetween(0, 59));
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length == 0 ? null : constants[faker.random().nextInt(constants.length)];
        }
        if (type == String[].class) {
            return new String[]{faker.phoneNumber().phoneNumber()};
        }
        return null;
    }

    private List<Field> fieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }
        return fields;
    }
}
