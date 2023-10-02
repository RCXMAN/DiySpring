package com.practice.diy.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ResourceResolver {
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan (Function<Resource, R> mapper) throws IOException {
        String basePackagePath = this.basePackage.replace(".", "/");
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private <R> void scan0(String basePackagePath, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        Enumeration<URL> urls = getContextClassLoader().getResources(basePackagePath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uriToString(uri));

            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            uriBaseStr = removeTrailingSlash(uriBaseStr);

            if (uriBaseStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    private Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    private <R> void scanFile(boolean isJar, String uriBaseStr, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(uriBaseStr, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = path.substring(uriBaseStr.substring(5).length());
                res = new Resource("file:" + path, removeLeadingSlash(name));
            }

            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    private ClassLoader getContextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }

    private String uriToString(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    private String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }

        return s;
    }

}
