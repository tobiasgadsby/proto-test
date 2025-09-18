package com.protoTest.ner;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class NerTrainer {

    public static void main(String[] args) {

        Properties props = new Properties();
        try (FileInputStream stream = new FileInputStream("src/main/resources/ner.properties")) {
            props.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            String modelOutputPath = props.getProperty("serializeTo");
            File modelOutputFile = new File(modelOutputPath);

            System.out.println("----------------------------------------");
            System.out.println("Attempting to save model to absolute path: " + modelOutputFile.getAbsolutePath());
            System.out.println("----------------------------------------");

            CRFClassifier<CoreLabel> classifier = new CRFClassifier<>(props);
            classifier.train();

            classifier.serializeClassifier(modelOutputPath);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
