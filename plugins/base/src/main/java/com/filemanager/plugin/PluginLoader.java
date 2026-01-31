package com.filemanager.plugin;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private final List<URLClassLoader> pluginClassLoaders = new ArrayList<>();
    private final List<IPlugin> externalPlugins = new ArrayList<>();

    public List<IPlugin> loadPluginsFromDirectory(String pluginDirPath) {
        List<IPlugin> loadedPlugins = new ArrayList<>();
        
        Path pluginDir = Paths.get(pluginDirPath);
        if (!Files.exists(pluginDir) || !Files.isDirectory(pluginDir)) {
            return loadedPlugins;
        }

        try {
            Files.list(pluginDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(jarPath -> {
                    List<IPlugin> plugins = loadPluginFromJar(jarPath.toFile());
                    loadedPlugins.addAll(plugins);
                });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return loadedPlugins;
    }

    public List<IPlugin> loadPluginFromJar(File jarFile) {
        List<IPlugin> loadedPlugins = new ArrayList<>();

        if (!jarFile.exists() || !jarFile.isFile()) {
            return loadedPlugins;
        }

        try {
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarUrl},
                Thread.currentThread().getContextClassLoader()
            );

            pluginClassLoaders.add(classLoader);

            ServiceLoader<IPlugin> serviceLoader = ServiceLoader.load(IPlugin.class, classLoader);
            
            for (IPlugin plugin : serviceLoader) {
                loadedPlugins.add(plugin);
                externalPlugins.add(plugin);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return loadedPlugins;
    }

    public boolean isPluginJar(File jarFile) {
        if (!jarFile.exists() || !jarFile.isFile()) {
            return false;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            return jar.stream()
                .anyMatch(entry -> entry.getName().equals("META-INF/services/com.filemanager.plugin.IPlugin"));
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> scanPluginDirectory(String pluginDirPath) {
        List<String> pluginJars = new ArrayList<>();
        
        Path pluginDir = Paths.get(pluginDirPath);
        if (!Files.exists(pluginDir) || !Files.isDirectory(pluginDir)) {
            return pluginJars;
        }

        try {
            Files.list(pluginDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .filter(path -> isPluginJar(path.toFile()))
                .forEach(path -> pluginJars.add(path.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pluginJars;
    }

    public void unloadExternalPlugins() {
        externalPlugins.clear();
        
        for (URLClassLoader classLoader : pluginClassLoaders) {
            try {
                classLoader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        pluginClassLoaders.clear();
    }

    public List<IPlugin> getExternalPlugins() {
        return new ArrayList<>(externalPlugins);
    }

    public void reloadExternalPlugins(String pluginDirPath) {
        unloadExternalPlugins();
        loadPluginsFromDirectory(pluginDirPath);
    }
}