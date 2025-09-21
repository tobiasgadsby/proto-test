package com.protoTest;

import com.example.testing.protos.main.Company;
import com.example.testing.protos.main.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.google.protobuf.*;
import com.protoTest.common.Constants;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import jakarta.annotation.PostConstruct;
import org.apache.catalina.User;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import com.protoTest.common.Classifiers;

import static com.protoTest.generation.FieldGenerationUtils.*;

@Service
public class ProtoTestService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ProtoTestService.class);
    Logger logger = Logger.getLogger(ProtoTestService.class.getName());

    ObjectMapper mapper;

    private CRFClassifier<CoreLabel> classifier;

    @Autowired
    public ProtoTestService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() throws InvalidProtocolBufferException {

        logger.info("ProtoTestService init");

        Descriptors.Descriptor descriptor = UserProfile.getDescriptor();
        DynamicMessage generatedMessage = generateProtoObject(descriptor);
        UserProfile person = UserProfile.parseFrom(generatedMessage.toByteArray());
        System.out.println("ProtoTestService init done");
    }

    public DynamicMessage generateProtoObject(Descriptors.Descriptor descriptor) {

        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);

        try {
            String modelPath = "modelOutput/field-ner-model.ser.gz";
            classifier = CRFClassifier.getClassifier(modelPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {

            logger.info("field: " + normaliseFieldName(field.getName()));

            List<CoreLabel> classify = classifier.classify(normaliseFieldName(field.getName())).get(0);

            classify.forEach(classifyMessage -> logger.info("classifyMessage: " + classifyMessage.get(CoreAnnotations.AnswerAnnotation.class)));

            String classification = modalLabel(classify);

            logger.info("classification: " + classification);

            if (field.isRepeated()) {
                for (int i = 0; i < 5; i++) {
                    Object value = generateValueForField(field, classification);
                    builder.addRepeatedField(field, value);
                }
            } else {
                Object value = generateValueForField(field, classification);
                builder.setField(field, value);
            }

        }

        return builder.build();
    }

    private static String modalLabel(List<CoreLabel> labels) {
        Map<String, Integer> frequency = new HashMap<>();

        Integer max = 0;
        String mode = "";

        for (CoreLabel label : labels) {

            String classification = label.get(CoreAnnotations.AnswerAnnotation.class);

            int count = 1;

            if (frequency.containsKey(classification)) {

                count = frequency.get(classification);
                count++;
                frequency.put(classification, count);

            } else {
                frequency.put(classification, count);
            }

            if (count > max) {
                max = count;
                mode = classification;
            }

        }
        return mode;
    }

    private Object generateValueForField(Descriptors.FieldDescriptor field, String classification) {
        if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            return generateProtoObject(field.getMessageType());
        } if (!(classification.equals("O") || field.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM)) {
            return generateNerValue(field, classification);
        } else {
            return generatePrimitiveValue(field);
        }
    }

    private String normaliseFieldName(String field) {

        List<String> words = Arrays.asList(field.split("[_]"));

        return String.join(" ", words.stream().map(
                word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase()
        ).toList());

    }

    private Object generateNerValue(Descriptors.FieldDescriptor field, String classification) {

        Descriptors.FieldDescriptor.JavaType fieldType = field.getJavaType();

        Faker faker = new Faker();

        try {
            return switch (classification) {
                case "IDENTIFIER" -> generateIdentifier(fieldType);
                case "NAME"-> generateName(faker, Classifiers.NAME);
                case "FIRST_NAME" -> generateName(faker, Classifiers.FIRST_NAME);
                case "LAST_NAME" -> generateName(faker, Classifiers.LAST_NAME);
                case "ADDRESS" -> generateAddress(faker, Classifiers.ADDRESS);
                case "POSTAL_CODE" -> generateAddress(faker, Classifiers.POSTAL_CODE);
                case "PEOPLE_COUNT" -> generateCount();
                case "USER_NAME" -> generateUsername(faker, fieldType);
                case "EMAIL" -> generateEmail(faker);
                case "TIME" -> generateTime(faker, fieldType);
                default -> throw new ServiceException("Unsupported classification: " + classification);
            };
        } catch (RuntimeException e) {
            return generatePrimitiveValue(field);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

    }

    private Object generatePrimitiveValue(Descriptors.FieldDescriptor field) {

        Descriptors.FieldDescriptor.Type fieldType = field.getType();

        Random random = new Random();

        return switch (fieldType) {
            case INT32, UINT32 -> Constants.DUMMY_INTEGER;
            case INT64, UINT64 -> Constants.DUMMY_LONG;
            case STRING -> Constants.DUMMY_STRING;
            case BOOL -> Constants.DUMMY_BOOLEAN;
            case DOUBLE -> Constants.DUMMY_DOUBLE;
            case FLOAT -> Constants.DUMMY_FLOAT;
            case ENUM -> field.getEnumType().getValues().get(random.nextInt(field.getEnumType().getValues().size()));
            case BYTES -> Constants.DUMMY_BYTESTRING;
            default -> throw new RuntimeException("Unsupported field type: " + fieldType);
        };
    }
}
