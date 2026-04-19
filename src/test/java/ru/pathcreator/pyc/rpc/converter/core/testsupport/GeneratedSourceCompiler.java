package ru.pathcreator.pyc.rpc.converter.core.testsupport;

import ru.pathcreator.pyc.rpc.converter.core.model.JavaSourceArtifact;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GeneratedSourceCompiler {
    private GeneratedSourceCompiler() {
    }

    public static Path compile(List<JavaSourceArtifact> sources) throws IOException {
        Path outputDirectory = Files.createTempDirectory("rpc-converter-generated");
        List<Path> javaFiles = new ArrayList<>();
        for (JavaSourceArtifact source : sources) {
            Path file = outputDirectory.resolve(source.fullyQualifiedClassName().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.sourceCode(), StandardCharsets.UTF_8);
            javaFiles.add(file);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK compiler is required to compile generated sources");

        List<String> options = List.of(
                "--release", "25",
                "-classpath", System.getProperty("java.class.path"),
                "-d", outputDirectory.toString()
        );

        List<String> diagnostics = new ArrayList<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            boolean success = compiler.getTask(
                    null,
                    fileManager,
                    diagnostic -> diagnostics.add(diagnostic.toString()),
                    options,
                    null,
                    fileManager.getJavaFileObjectsFromPaths(javaFiles)
            ).call();
            assertTrue(success, "Generated sources must compile:\n" + String.join("\n", diagnostics));
        }

        return outputDirectory;
    }
}