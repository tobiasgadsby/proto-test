package com.protoTest;

import com.example.testing.protos.main.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.protoTest.common.Constants;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ProtoTestService {

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

            logger.info("field: " + field.getName() + " : " + normaliseFieldName(field.getName()));

            List<List<CoreLabel>> classify = classifier.classify(normaliseFieldName(field.getName()));
            CoreLabel label = classify.get(0).get(0);
            String tag =  label.get(CoreAnnotations.AnswerAnnotation.class);


            logger.info("clasification: " + tag);

            if (field.getContainingOneof() != null) continue;

            if (field.isRepeated()) {
                for (int i=0; i < 5; i++) {
                    Object value = generateValueForField(field);
                    builder.addRepeatedField(field, value);
                }
            } else {
                Object value = generateValueForField(field);
                builder.setField(field, value);
            }

        }

        return builder.build();
    }

    private Object generateValueForField(Descriptors.FieldDescriptor field) {
        if (field.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            return generateProtoObject(field.getMessageType());
        } else {
            return generatePrimitiveValue(field);
        }
    }

    private String normaliseFieldName(String field) {

        List<String> words = Arrays.asList(field.split("[_]"));

        StringBuilder stringBuilder = new StringBuilder();

        return String.join(" ", words.stream().map(
                word -> word.replaceAll("[^a-zA-Z]", "").toLowerCase()
        ).toList());

    }

    private Object generatePrimitiveValue(Descriptors.FieldDescriptor field) {

        Descriptors.FieldDescriptor.Type fieldType = field.getType();

        return switch (fieldType) {
            case INT32, UINT32 -> Constants.DUMMY_INTEGER;
            case INT64, UINT64 -> Constants.DUMMY_LONG;
            case STRING -> Constants.DUMMY_STRING;
            case BOOL -> Constants.DUMMY_BOOLEAN;
            case DOUBLE -> Constants.DUMMY_DOUBLE;
            case FLOAT -> Constants.DUMMY_FLOAT;
            case ENUM -> field.getEnumType().getValues().get(0);
            case BYTES -> Constants.DUMMY_BYTESTRING;
            default -> throw new RuntimeException("Unsupported field type: " + fieldType);
        };
    }
}
