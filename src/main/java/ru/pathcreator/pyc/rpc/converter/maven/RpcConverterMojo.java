package ru.pathcreator.pyc.rpc.converter.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import ru.pathcreator.pyc.rpc.converter.annotations.RpcSbe;
import ru.pathcreator.pyc.rpc.converter.core.generation.GenerationEngine;
import ru.pathcreator.pyc.rpc.converter.core.model.GenerationBundle;
import ru.pathcreator.pyc.rpc.converter.core.model.JavaSourceArtifact;
import ru.pathcreator.pyc.rpc.converter.core.model.ResourceArtifact;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven goal that scans compiled project classes for {@link RpcSbe} DTOs and generates SBE schemas plus runtime
 * codec implementations.
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public final class RpcConverterMojo extends AbstractMojo {

    /**
     * Current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing compiled project classes that will be scanned for annotated DTOs.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    /**
     * Output directory for generated Java source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/rpc-converter", required = true)
    private File generatedSourcesDirectory;

    /**
     * Output directory for generated resources such as SBE XML schemas and service metadata.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/rpc-converter", required = true)
    private File generatedResourcesDirectory;

    /**
     * Package name used for generated Java types.
     */
    @Parameter(property = "rpc.generatedPackage", defaultValue = "${project.groupId}.rpc.converter.generated")
    private String generatedPackage;

    /**
     * Java release used to compile generated Java sources.
     */
    @Parameter(defaultValue = "${maven.compiler.release}")
    private String compilerRelease;

    /**
     * Executes DTO discovery, generation, resource writing, and compilation of generated sources.
     *
     * @throws MojoExecutionException on unexpected generation or I/O failures
     * @throws MojoFailureException   when the project DTO model is invalid for generation
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!classesDirectory.exists()) {
            getLog().info("No compiled classes found, skipping rpc-converter generation.");
            return;
        }
        try {
            final List<Class<?>> annotatedTypes = discoverAnnotatedTypes();
            if (annotatedTypes.isEmpty()) {
                getLog().info("No @RpcSbe DTOs found.");
                return;
            }
            final GenerationBundle bundle = new GenerationEngine().generate(annotatedTypes, generatedPackage);
            writeJavaSources(bundle.javaSources());
            writeResources(bundle.resources());
            compileGeneratedSources(bundle.javaSources());
        } catch (final IllegalStateException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final Exception e) {
            throw new MojoExecutionException("Failed to generate RPC codecs", e);
        }
    }

    private List<Class<?>> discoverAnnotatedTypes() throws IOException, DependencyResolutionRequiredException, ClassNotFoundException {
        List<Path> classFiles;
        try (final Stream<Path> stream = Files.walk(classesDirectory.toPath())) {
            classFiles = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$Generated"))
                    .collect(Collectors.toList());
        }
        final List<URL> urls = new ArrayList<>();
        urls.add(classesDirectory.toURI().toURL());
        for (final String element : project.getCompileClasspathElements()) {
            urls.add(new File(element).toURI().toURL());
        }
        final List<Class<?>> annotated = new ArrayList<>();
        try (final URLClassLoader classLoader = new URLClassLoader(urls.toArray(URL[]::new), Thread.currentThread().getContextClassLoader())) {
            for (final Path classFile : classFiles) {
                final String className = toClassName(classFile);
                final Class<?> type = classLoader.loadClass(className);
                if (type.isAnnotationPresent(RpcSbe.class)) {
                    annotated.add(type);
                }
            }
        }
        return annotated;
    }

    private String toClassName(final Path classFile) {
        final String relative = classesDirectory.toPath().relativize(classFile).toString();
        return relative.substring(0, relative.length() - ".class".length()).replace(File.separatorChar, '.');
    }

    private void writeJavaSources(final List<JavaSourceArtifact> sources) throws IOException {
        for (final JavaSourceArtifact source : sources) {
            final Path file = generatedSourcesDirectory.toPath().resolve(source.fullyQualifiedClassName().replace('.', '/') + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.sourceCode(), StandardCharsets.UTF_8);
        }
    }

    private void writeResources(final List<ResourceArtifact> resources) throws IOException {
        for (final ResourceArtifact resource : resources) {
            final Path generated = generatedResourcesDirectory.toPath().resolve(resource.relativePath());
            final Path classesTarget = classesDirectory.toPath().resolve(resource.relativePath());
            Files.createDirectories(generated.getParent());
            Files.createDirectories(classesTarget.getParent());
            Files.writeString(generated, resource.content(), StandardCharsets.UTF_8);
            Files.writeString(classesTarget, resource.content(), StandardCharsets.UTF_8);
        }
    }

    private void compileGeneratedSources(final List<JavaSourceArtifact> sources) throws IOException, DependencyResolutionRequiredException, MojoFailureException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new MojoFailureException("No system Java compiler available. Run Maven with a JDK.");
        }
        final List<File> javaFiles = new ArrayList<>();
        for (final JavaSourceArtifact source : sources) {
            javaFiles.add(generatedSourcesDirectory.toPath()
                    .resolve(source.fullyQualifiedClassName().replace('.', '/') + ".java")
                    .toFile());
        }
        final List<String> options = new ArrayList<>();
        options.add("--release");
        options.add(compilerRelease == null || compilerRelease.isBlank() ? "25" : compilerRelease);
        options.add("-d");
        options.add(classesDirectory.getAbsolutePath());
        options.add("-classpath");
        options.add(classpath());
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            final boolean ok = compiler.getTask(
                    null,
                    fileManager,
                    diagnostic -> getLog().error(diagnostic.toString()),
                    options,
                    null,
                    fileManager.getJavaFileObjectsFromFiles(javaFiles)
            ).call();
            if (!ok) {
                throw new MojoFailureException("Compilation of generated RPC codecs failed.");
            }
        }
    }

    private String classpath() throws DependencyResolutionRequiredException {
        final List<String> parts = new ArrayList<>();
        parts.add(classesDirectory.getAbsolutePath());
        parts.addAll(project.getCompileClasspathElements());
        return String.join(File.pathSeparator, parts);
    }
}