package com.github.FelipeJuan435;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mojo(name = "detect-smells", defaultPhase = LifecyclePhase.TEST)
public class MyMojo extends AbstractMojo {

    @Parameter(property = "testSmellDetector.jarPath", required = true)
    private String jarPath;

    @Parameter(property = "testSmellDetector.inputCsvPath", required = true)
    private String inputCsvPath;
    
    @Parameter(property = "testSmellDetector.testPath", required = true)
    private String testPath;
    
    public void execute() throws MojoExecutionException {
    	// Validar que los archivos existen
        File jarFile = new File(jarPath);
        File inputCsvFile = new File(inputCsvPath);

        if (!jarFile.exists()) {
            throw new MojoExecutionException("El archivo TestSmellDetector.jar no se encuentra en la ruta especificada: " + jarPath);
        }

        Path csvPath = Paths.get(inputCsvPath);
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
        	// Crear archivo csv input
            Files.createDirectories(csvPath.getParent());
            writer.write(String.format("myApp,%s,%s%n", testPath, ""));
        } catch (Exception e) {
            throw new MojoExecutionException("Error creando inputCSV file", e);
        }
        getLog().info("Archivo CSV creado en: " + csvPath);
        
        // Ejecutar TestSmellDetector.jar
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath, inputCsvPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Leer y mostrar la salida del comando
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("TestSmellDetector.jar termino con un codigo de error: " + exitCode);
            }

            // Leer el archivo de salida
            Path outputCsvPath = inputCsvFile.toPath().resolveSibling("output.csv");
            File outputCsvFile = outputCsvPath.toFile();
            if (!outputCsvFile.exists()) {
                throw new MojoExecutionException("No se genero el archivo CSV de salida en la ruta esperada: " + outputCsvPath);
            }

            // Imprimir contenido del CSV de salida
            try (BufferedReader outputReader = new BufferedReader(new FileReader(outputCsvFile))) {
                String outputLine;
                getLog().info("Contenido del archivo de salida:");
                while ((outputLine = outputReader.readLine()) != null) {
                    getLog().info(outputLine);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error ejecutando TestSmellDetector.jar", e);
        }
    }
}