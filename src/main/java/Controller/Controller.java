package Controller;

import Annotations.Bind;
import Models.*;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;

public class Controller {
    private BaseModel model;
    private Map<String, Object> results; // script-generated variables

    public Controller(String modelName) {
        try {
            Class<?> clas = Class.forName("Models." + modelName);
            if (BaseModel.class.isAssignableFrom(clas)) {
                model = (BaseModel) clas.getDeclaredConstructor().newInstance();
            } else {
                throw new IllegalArgumentException("Class does not extend BaseModel: " + modelName);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load model: " + modelName, e);
        }
        results = new LinkedHashMap<>();
    }

    public Controller readDataFrom(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            Map<String, String[]> data = new HashMap<>();
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 1) {
                    data.put(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
                }
            }

            // the model fields
            for (Field field : model.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);

                    if (field.getType().equals(int.class)) {
                        // field for number of years
                        field.set(model, data.get("LATA").length);
                    } else if (field.getType().equals(double[].class)) {
                        String[] values = data.get(field.getName());
                        if (values != null) {
                            double[] doubleValues = new double[data.get("LATA").length];
                            for (int i = 0; i < values.length; i++) {
                                doubleValues[i] = Double.parseDouble(values[i]);
                            }
                            // remaining values with the last value
                            for (int i = values.length; i < doubleValues.length; i++) {
                                doubleValues[i] = doubleValues[values.length - 1];
                            }
                            field.set(model, doubleValues);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runModel() {
        if (model != null) {
            model.run();
        } else {
            throw new IllegalStateException("Model is not initialized.");
        }
        return this;
    }

    public Controller runScriptFromFile(String fName) {
        try {
            Binding binding = prepareBinding();

            // to read script content from the file
            String scriptContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fName)));

            // to execute the script
            GroovyShell shell = new GroovyShell(binding);
            shell.evaluate(scriptContent);

            // script variables
            getScriptVariables(binding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runScript(String script) {
        try {
            Binding binding = prepareBinding();

            // the script passed as a string
            GroovyShell shell = new GroovyShell(binding);
            shell.evaluate(script);

            // script variables
            getScriptVariables(binding);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public String getResultsAsTsv() {
        StringBuilder sb = new StringBuilder();
        Set<String> printedFields = new HashSet<>();

        try {
            // the number of years (LL) dynamically
            int LL = getNumberOfYears();

            // the header row for years
            appendYearRow(sb, LL);

            // fields annotated with @Bind
            appendBindFields(sb, printedFields, LL);

            // script-generated results
            appendScriptResults(sb, printedFields, LL);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private int getNumberOfYears() throws Exception {
        Field llField = model.getClass().getDeclaredField("LL");
        llField.setAccessible(true);
        return (int) llField.get(model); // number of years
    }

    private void appendYearRow(StringBuilder sb, int LL) {
        sb.append("LATA"); // first column name
        int startYear = 2015;
        for (int i = 0; i < LL; i++) {
            sb.append("\t").append(startYear + i);
        }
        sb.append("\n");
    }

    private void appendBindFields(StringBuilder sb, Set<String> printedFields, int LL) throws IllegalAccessException {
        for (Field field : model.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Bind.class) && !field.getName().equals("LL")) {
                field.setAccessible(true);
                sb.append(field.getName());
                printedFields.add(field.getName());

                Object value = field.get(model);
                if (value instanceof double[]) {
                    double[] array = (double[]) value;
                    for (int i = 0; i < LL; i++) {
                        sb.append("\t").append(array[i]);
                    }
                }
                sb.append("\n");
            }
        }
    }

    private void appendScriptResults(StringBuilder sb, Set<String> printedFields, int LL) {
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            String key = entry.getKey();
            if (key.equals("LL") || printedFields.contains(key)) {
                continue; // to skip LL or already printed fields
            }

            sb.append(key);
            Object value = entry.getValue();

            if (value instanceof double[]) {
                double[] array = (double[]) value;
                for (int i = 0; i < array.length; i++) {
                    sb.append("\t").append(array[i]);
                }
            }
            sb.append("\n");
        }
    }

    private Binding prepareBinding() throws Exception {
        Binding binding = new Binding();

        // @Bind fields available in the script
        disableAccessCheck(binding);

        // to add the LL field (number of years) to the binding
        Field llField = model.getClass().getDeclaredField("LL");
        ReflectionUtility.disableAccessCheck(llField); // Ensure access to private field
        binding.setVariable("LL", llField.get(model));

        return binding;
    }

    private void disableAccessCheck(Binding binding) throws IllegalAccessException {
        for (Field field : model.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Bind.class)) {
                ReflectionUtility.disableAccessCheck(field); // access to private field
                binding.setVariable(field.getName(), field.get(model));
            }
        }
    }

    private void getScriptVariables(Binding binding) {
        for (Object obj : binding.getVariables().entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) obj;
            String variableName = entry.getKey().toString();
            Object variableValue = entry.getValue();

            // variables with names longer than 1 character
            if (variableName.length() > 1) {
                results.put(variableName, variableValue);
            }
        }
    }
}