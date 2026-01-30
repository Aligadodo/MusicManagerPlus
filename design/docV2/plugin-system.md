# Plugin System Documentation

## Overview

The FileManager Plus plugin system provides a flexible and extensible framework for adding new functionality to the application. Plugins can be used to implement custom file processing strategies, integrate with external services, or add new features to the system.

## Architecture

The plugin system is built on top of Java's ServiceLoader mechanism, which provides a standard way to discover and load services at runtime. This approach allows plugins to be added without modifying the core application code.

### Key Components

1. **IPlugin Interface**: Defines the standard methods that all plugins must implement
2. **PluginRegistry**: Manages plugin discovery, registration, and lifecycle
3. **ExecutionContext**: Provides runtime context for plugin execution
4. **PluginConfigDTO**: Represents plugin configuration data

## Creating a Plugin

To create a new plugin, follow these steps:

### 1. Implement the IPlugin Interface

```java
package com.filemanager.plugin.example;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.IPlugin;
import com.filemanager.plugin.ExecutionContext;

import java.util.List;

public class ExamplePlugin implements IPlugin {

    @Override
    public String getId() {
        return "example-plugin";
    }

    @Override
    public String getName() {
        return "Example Plugin";
    }

    @Override
    public String getDescription() {
        return "An example plugin demonstrating the plugin system";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("exampleSetting", "defaultValue");
        config.setValue("enabled", true);
        return config;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        // Plugin execution logic here
        // Return list of ChangeRecord objects representing the changes made
        return List.of();
    }
}
```

### 2. Register the Plugin

Create a service provider configuration file in `src/main/resources/META-INF/services/` named `com.filemanager.plugin.IPlugin` and add the fully qualified class name of your plugin:

```
com.filemanager.plugin.example.ExamplePlugin
```

### 3. Build and Deploy

Build your plugin as a JAR file and place it in the appropriate directory for plugin discovery. The exact location depends on your deployment environment, but typically plugins are placed in a `plugins` directory alongside the main application.

## Plugin Lifecycle

1. **Discovery**: Plugins are discovered during application startup using ServiceLoader
2. **Registration**: Discovered plugins are registered with the PluginRegistry
3. **Initialization**: Plugins are initialized with their default configurations
4. **Execution**: Plugins are executed when requested through the API
5. **Reloading**: Plugins can be reloaded at runtime using the `/plugins/reload` endpoint

## Plugin Configuration

Plugins can define their own configuration options using the `PluginConfigDTO` class. Configuration values are stored in memory and can be updated through the API.

### Example Configuration

```java
PluginConfigDTO config = new PluginConfigDTO();
config.setValue("targetDirectory", "/path/to/target");
config.setValue("recursive", true);
config.setValue("filePattern", "*.mp3");
```

## Plugin Execution

When a plugin is executed, it receives:

1. **filePaths**: A list of file paths to process
2. **config**: The current configuration for the plugin
3. **context**: An execution context providing runtime information

Plugins return a list of `ChangeRecord` objects representing the changes they made to the files.

### Example Execution Flow

1. Client sends a request to execute a plugin
2. PluginController receives the request
3. PluginServiceImpl retrieves the plugin from the registry
4. PluginServiceImpl calls the plugin's execute method
5. Plugin processes the files and generates change records
6. PluginServiceImpl returns the change records to the controller
7. Controller returns the change records to the client

## Built-in Plugins

FileManager Plus includes several built-in plugins:

| Plugin ID | Name | Description |
|-----------|------|-------------|
| `file-collection` | File Collection Plugin | Collects and organizes files based on configuration |
| `metadata-scraper` | Metadata Scraper Plugin | Scrapes and updates file metadata from external sources |
| `file-cleanup` | File Cleanup Plugin | Cleans up files based on age, size, and other criteria |

## Extending the Plugin System

The plugin system can be extended in several ways:

### 1. Custom Plugin Interfaces

Create specialized plugin interfaces that extend `IPlugin` for specific types of functionality:

```java
public interface MetadataPlugin extends IPlugin {
    List<MetadataField> getSupportedFields();
    void updateMetadata(String filePath, Map<MetadataField, Object> metadata);
}
```

### 2. Plugin Dependencies

Plugins can depend on other plugins or external libraries. When building plugins with dependencies, ensure that all required libraries are included in the plugin JAR or available in the classpath.

### 3. Plugin Configuration UI

Create custom configuration UIs for plugins in the client applications. The Flutter Web client can dynamically generate configuration forms based on plugin configuration schemas.

## Best Practices

### Plugin Development

1. **Keep plugins focused**: Each plugin should implement a single, well-defined functionality
2. **Handle errors gracefully**: Plugins should catch and handle exceptions internally
3. **Provide clear documentation**: Include documentation for plugin configuration options and usage
4. **Test thoroughly**: Test plugins with various file types and configurations
5. **Use logging**: Log important events and errors for debugging

### Plugin Security

1. **Validate input**: Always validate file paths and configuration values
2. **Limit file system access**: Only access files that are explicitly provided
3. **Avoid system commands**: Use Java APIs instead of executing system commands
4. **Be mindful of resources**: Clean up resources to avoid leaks
5. **Respect user preferences**: Honor configuration settings and user choices

## Troubleshooting

### Common Issues

1. **Plugin not discovered**: Check that the service provider configuration file is correctly named and located
2. **Plugin fails to load**: Check for missing dependencies or runtime errors
3. **Plugin execution errors**: Check plugin logs and error messages
4. **Configuration not saved**: Ensure that configuration updates are properly handled

### Debugging Tips

1. **Enable debug logging**: Set logging level to DEBUG for plugin-related classes
2. **Use the reload endpoint**: Test plugin changes without restarting the application
3. **Check the plugin registry**: Verify that plugins are properly registered
4. **Test with sample files**: Use small test files to isolate issues

## Conclusion

The plugin system is a powerful feature of FileManager Plus that allows for extensive customization and extension. By following the guidelines and best practices outlined in this document, you can create powerful plugins that enhance the functionality of the application and provide value to users.
