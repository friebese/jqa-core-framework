package com.buschmais.jqassistant.core.plugin.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.core.plugin.api.PluginConfigurationReader;
import com.buschmais.jqassistant.core.plugin.api.PluginRepositoryException;
import com.buschmais.jqassistant.core.plugin.api.ScannerPluginRepository;
import com.buschmais.jqassistant.core.plugin.schema.v1.IdClassType;
import com.buschmais.jqassistant.core.plugin.schema.v1.JqassistantPlugin;
import com.buschmais.jqassistant.core.plugin.schema.v1.ScannerType;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;

/**
 * Scanner plugin repository implementation.
 */
public class ScannerPluginRepositoryImpl extends AbstractPluginRepository implements ScannerPluginRepository {

    private Map<String, ScannerPlugin<?, ?>> scannerPlugins = new HashMap<>();

    /**
     * Constructor.
     */
    public ScannerPluginRepositoryImpl(PluginConfigurationReader pluginConfigurationReader) {
        super(pluginConfigurationReader);
    }

    @Override
    public void initialize() throws PluginRepositoryException {
        getScannerPlugins(plugins);
    }

    @Override
    public void destroy() {
        scannerPlugins.values().forEach(scannerPlugin -> scannerPlugin.destroy());
    }

    @Override
    public Map<String, ScannerPlugin<?, ?>> getScannerPlugins(ScannerContext scannerContext, Map<String, Object> properties) {
        for (ScannerPlugin<?, ?> scannerPlugin : scannerPlugins.values()) {
            scannerPlugin.configure(scannerContext, new HashMap<>(properties));
        }
        return scannerPlugins;
    }

    private void getScannerPlugins(List<JqassistantPlugin> plugins) throws PluginRepositoryException {
        for (JqassistantPlugin plugin : plugins) {
            ScannerType scannerType = plugin.getScanner();
            if (scannerType != null) {
                for (IdClassType classType : scannerType.getClazz()) {
                    ScannerPlugin<?, ?> scannerPlugin = createInstance(classType.getValue());
                    if (scannerPlugin != null) {
                        scannerPlugin.initialize();
                        String id = classType.getId();
                        if (id == null) {
                            id = scannerPlugin.getClass().getSimpleName();
                        }
                        scannerPlugins.put(id, scannerPlugin);
                    }
                }
            }
        }
    }
}
