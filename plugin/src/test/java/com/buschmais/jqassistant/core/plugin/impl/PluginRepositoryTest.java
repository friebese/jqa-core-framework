package com.buschmais.jqassistant.core.plugin.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.plugin.api.*;
import com.buschmais.jqassistant.core.plugin.impl.plugin.TestReportPlugin;
import com.buschmais.jqassistant.core.plugin.impl.plugin.TestScannerPlugin;
import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies plugin repository related functionality.
 */
public class PluginRepositoryTest {

    /**
     * Verifies that properties are loaded and passed to plugins.
     *
     * @throws PluginRepositoryException
     *             If the test fails.
     */
    @Test
    public void pluginProperties() throws PluginRepositoryException {
        PluginConfigurationReader pluginConfigurationReader = new PluginConfigurationReaderImpl(PluginRepositoryTest.class.getClassLoader());
        Map<String, Object> properties = new HashMap<>();
        properties.put("testKey", "testValue");
        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();
        // scanner plugins
        verifyProperties(getScannerPluginProperties(pluginRepository, properties));
        // report plugins
        verifyProperties(getReportPluginProperties(pluginRepository, properties));
        pluginRepository.destroy();
    }

    @Test
    public void repositories() throws PluginRepositoryException {
        PluginConfigurationReader pluginConfigurationReader = new PluginConfigurationReaderImpl(PluginRepositoryTest.class.getClassLoader());
        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();
        // Scanner plugins
        ScannerContext scannerContext = mock(ScannerContext.class);
        Map<String, ScannerPlugin<?, ?>> scannerPlugins = pluginRepository.getScannerPluginRepository().getScannerPlugins(scannerContext,
                Collections.emptyMap());
        assertThat(scannerPlugins).hasSize(2);
        assertThat(scannerPlugins.get(TestScannerPlugin.class.getSimpleName()), notNullValue());
        assertThat(scannerPlugins.get("testScanner"), notNullValue());
        // Report plugins
        ReportContext reportContext = mock(ReportContext.class);
        Map<String, ReportPlugin> reportPlugins = pluginRepository.getReportPluginRepository().getReportPlugins(reportContext, Collections.emptyMap());
        assertThat(reportPlugins.size(), equalTo(3));
        assertThat(reportPlugins.get(TestReportPlugin.class.getSimpleName()), notNullValue());
        assertThat(reportPlugins.get("testReport"), notNullValue());
        pluginRepository.destroy();
    }

    private void verifyProperties(Map<String, Object> pluginProperties) {
        assertThat(pluginProperties, notNullValue());
        assertThat(pluginProperties.get("testKey"), CoreMatchers.equalTo("testValue"));
    }

    private Map<String, Object> getScannerPluginProperties(PluginRepository pluginRepository, Map<String, Object> properties) throws PluginRepositoryException {
        ScannerPluginRepository scannerPluginRepository = pluginRepository.getScannerPluginRepository();
        ScannerContext scannerContext = mock(ScannerContext.class);
        Map<String, ScannerPlugin<?, ?>> scannerPlugins = scannerPluginRepository.getScannerPlugins(scannerContext, properties);
        assertThat(scannerPlugins.size(), greaterThan(0));
        for (ScannerPlugin<?, ?> scannerPlugin : scannerPlugins.values()) {
            if (scannerPlugin instanceof TestScannerPlugin) {
                return ((TestScannerPlugin) scannerPlugin).getProperties();
            }
        }
        return null;
    }

    private Map<String, Object> getReportPluginProperties(PluginRepository pluginRepository, Map<String, Object> properties) throws PluginRepositoryException {
        ReportPluginRepository reportPluginRepository = pluginRepository.getReportPluginRepository();
        Map<String, ReportPlugin> reportPlugins = reportPluginRepository.getReportPlugins(mock(ReportContext.class), properties);
        assertThat(reportPlugins.size()).isGreaterThan(0);
        for (ReportPlugin reportPlugin : reportPlugins.values()) {
            if (reportPlugin instanceof TestReportPlugin) {
                return ((TestReportPlugin) reportPlugin).getProperties();
            }
        }
        return null;
    }
}