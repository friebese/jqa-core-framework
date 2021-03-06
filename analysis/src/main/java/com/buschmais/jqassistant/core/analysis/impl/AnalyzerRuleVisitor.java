package com.buschmais.jqassistant.core.analysis.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.analysis.api.AnalyzerConfiguration;
import com.buschmais.jqassistant.core.analysis.api.AnalyzerContext;
import com.buschmais.jqassistant.core.analysis.api.RuleInterpreterPlugin;
import com.buschmais.jqassistant.core.analysis.api.model.ConceptDescriptor;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.executor.AbstractRuleVisitor;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.Constraint;
import com.buschmais.jqassistant.core.rule.api.model.Executable;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.rule.api.model.Group;
import com.buschmais.jqassistant.core.rule.api.model.Parameter;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.model.Severity;

/**
 * Implementation of a rule visitor for analysis execution.
 */
public class AnalyzerRuleVisitor extends AbstractRuleVisitor {

    private AnalyzerConfiguration configuration;
    private AnalyzerContext analyzerContext;
    private Map<String, String> ruleParameters;
    private ReportPlugin reportPlugin;
    private Map<String, Collection<RuleInterpreterPlugin>> ruleInterpreterPlugins;

    /**
     * Constructor.
     *
     * @param configuration
     *            The configuration
     * @param ruleParameters
     *            The rule parameter.s
     * @param ruleInterpreterPlugins
     *            The {@link RuleInterpreterPlugin}s.
     * @param reportPlugin
     *            The report writer.
     */
    AnalyzerRuleVisitor(AnalyzerConfiguration configuration, AnalyzerContext analyzerContext, Map<String, String> ruleParameters,
            Map<String, Collection<RuleInterpreterPlugin>> ruleInterpreterPlugins, ReportPlugin reportPlugin) {
        this.configuration = configuration;
        this.analyzerContext = analyzerContext;
        this.ruleParameters = ruleParameters;
        this.ruleInterpreterPlugins = ruleInterpreterPlugins;
        this.reportPlugin = reportPlugin;
    }

    @Override
    public void beforeRules() throws RuleException {
        reportPlugin.begin();
    }

    @Override
    public void afterRules() throws RuleException {
        reportPlugin.end();
    }

    @Override
    public boolean visitConcept(Concept concept, Severity effectiveSeverity) throws RuleException {
        ConceptDescriptor conceptDescriptor = analyzerContext.getStore().find(ConceptDescriptor.class, concept.getId());
        Result.Status status;
        if (conceptDescriptor == null || configuration.isExecuteAppliedConcepts()) {
            analyzerContext.getLogger()
                    .info("Applying concept '" + concept.getId() + "' with severity: '" + concept.getSeverity().getInfo(effectiveSeverity) + "'" + ".");
            reportPlugin.beginConcept(concept);
            Result<Concept> result = execute(concept, effectiveSeverity);
            reportPlugin.setResult(result);
            status = result.getStatus();
            if (conceptDescriptor == null) {
                conceptDescriptor = analyzerContext.getStore().create(ConceptDescriptor.class);
                conceptDescriptor.setId(concept.getId());
                conceptDescriptor.setStatus(status);
            }
            reportPlugin.endConcept();
        } else {
            status = conceptDescriptor.getStatus();
        }
        return Result.Status.SUCCESS.equals(status);
    }

    @Override
    public void skipConcept(Concept concept, Severity effectiveSeverity) throws RuleException {
        reportPlugin.beginConcept(concept);
        Result<Concept> result = Result.<Concept> builder().rule(concept).status(Result.Status.SKIPPED).severity(effectiveSeverity).build();
        reportPlugin.setResult(result);
        reportPlugin.endConcept();
    }

    @Override
    public void visitConstraint(Constraint constraint, Severity effectiveSeverity) throws RuleException {
        analyzerContext.getLogger()
                .info("Validating constraint '" + constraint.getId() + "' with severity: '" + constraint.getSeverity().getInfo(effectiveSeverity) + "'.");
        reportPlugin.beginConstraint(constraint);
        reportPlugin.setResult(execute(constraint, effectiveSeverity));
        reportPlugin.endConstraint();
    }

    @Override
    public void skipConstraint(Constraint constraint, Severity effectiveSeverity) throws RuleException {
        reportPlugin.beginConstraint(constraint);
        Result<Constraint> result = Result.<Constraint> builder().rule(constraint).status(Result.Status.SKIPPED).severity(effectiveSeverity).build();
        reportPlugin.setResult(result);
        reportPlugin.endConstraint();
    }

    @Override
    public void beforeGroup(Group group, Severity effectiveSeverity) throws RuleException {
        analyzerContext.getLogger().info("Executing group '" + group.getId() + "'");
        reportPlugin.beginGroup(group);
    }

    @Override
    public void afterGroup(Group group) throws RuleException {
        reportPlugin.endGroup();
    }

    private <T extends ExecutableRule> Result<T> execute(T executableRule, Severity severity) throws RuleException {
        Map<String, Object> ruleParameters = getRuleParameters(executableRule);
        Executable<?> executable = executableRule.getExecutable();
        Collection<RuleInterpreterPlugin> languagePlugins = ruleInterpreterPlugins.get(executable.getLanguage());
        if (languagePlugins == null) {
            throw new RuleException("Could not determine plugin to execute " + executableRule);
        }
        for (RuleInterpreterPlugin languagePlugin : languagePlugins) {
            if (languagePlugin.accepts(executableRule)) {
                Result<T> result = languagePlugin.execute(executableRule, ruleParameters, severity, analyzerContext);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new RuleException("No plugin for language '" + executable.getLanguage() + "' returned a result for " + executableRule);
    }

    private Map<String, Object> getRuleParameters(ExecutableRule executableRule) throws RuleException {
        Map<String, Object> ruleParameters = new HashMap<>();
        Map<String, Parameter> parameters = executableRule.getParameters();
        for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
            String parameterName = entry.getKey();
            Parameter parameter = entry.getValue();
            Object parameterValue;
            String parameterValueAsString = this.ruleParameters.get(parameterName);
            if (parameterValueAsString != null) {
                try {
                    parameterValue = parameter.getType().parse(parameterValueAsString);
                } catch (RuleException e) {
                    throw new RuleException("Cannot determine value for parameter " + parameterName + "' of rule '" + executableRule + "'.");
                }
            } else {
                parameterValue = parameter.getDefaultValue();
            }
            if (parameterValue == null) {
                throw new RuleException("No value or default value defined for required parameter '" + parameterName + "' of rule '" + executableRule + "'.");
            }
            ruleParameters.put(parameterName, parameterValue);
        }
        return ruleParameters;
    }

}
