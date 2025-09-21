package com.protoTest.generation;

import com.github.javafaker.Faker;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldType;
import com.google.protobuf.JavaType;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.protoTest.common.Classifiers;

import java.util.Random;
import java.util.UUID;

public class FieldGenerationUtils {

    public static Object generateIdentifier(Descriptors.FieldDescriptor.JavaType javaType) {

        Random random = new Random();

        return switch (javaType) {
            case INT -> random.nextInt(99999999);
            case STRING -> UUID.randomUUID().toString();
            default -> throw new RuntimeException("Unsupported JavaType for Identifier Generation: " + javaType);
        };
    }

    public static String generateName(Faker faker, Classifiers classifier) {

        return switch (classifier) {
            case NAME -> faker.name().fullName();
            case FIRST_NAME -> faker.name().firstName();
            case LAST_NAME -> faker.name().lastName();
            default -> throw new RuntimeException("Unsupported Classifier: " + classifier);
        };

    }

    public static String generateAddress(Faker faker, Classifiers classifier) {
        return switch(classifier) {
            case ADDRESS -> faker.address().fullAddress();
            case POSTAL_CODE -> faker.address().zipCode();
            default -> throw new RuntimeException("Unsupported Classifier: " + classifier);
        };
    }

    public static Integer generateCount() {
        return new Random().nextInt(99999);
    }

    public static String generateUsername(Faker faker, Descriptors.FieldDescriptor.JavaType javaType) {

        if (javaType != Descriptors.FieldDescriptor.JavaType.STRING) {
            throw new RuntimeException("Unsupported JavaType for Username Generation: " + javaType);
        }

        return faker.name().username();
    }

    public static String generateEmail(Faker faker) {
        return faker.internet().emailAddress();
    }

    public static Object generateTime(Faker faker,  Descriptors.FieldDescriptor.JavaType javaType) {

        Random random = new Random();

        return switch (javaType) {
             case STRING -> Timestamps.toString(Timestamp.newBuilder().setSeconds(random.nextLong()).setNanos(random.nextInt()).build());
             case INT -> random.nextInt(999999);
             case LONG -> random.nextLong(999999);
             default -> throw new RuntimeException("Unsupported JavaType for Time Generation: " + javaType);
        };

    }
}
