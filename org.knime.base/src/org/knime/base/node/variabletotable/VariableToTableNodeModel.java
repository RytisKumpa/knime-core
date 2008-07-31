/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.variabletotable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.util.Pair;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class VariableToTableNodeModel extends NodeModel {

    private final VariableToTableSettings m_settings;
    
    /** One input, one output. */
    public VariableToTableNodeModel() {
        super(1, 1);
        m_settings = new VariableToTableSettings();
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = createOutSpec();
        BufferedDataContainer cont = exec.createDataContainer(spec);
        List<Pair<String, ScopeVariable.Type>> vars = 
            m_settings.getVariablesOfInterest();
        DataCell[] specs = new DataCell[vars.size()];
        for (int i = 0; i < vars.size(); i++) {
            Pair<String, ScopeVariable.Type> c = vars.get(i);
            ScopeVariable var = peekScopeVariable(c.getFirst());
            if (var == null || !var.getType().equals(c.getSecond())) {
                specs[i] = DataType.getMissingCell();
            } else {
                switch (c.getSecond()) {
                case DOUBLE:
                    specs[i] = new DoubleCell(var.getDoubleValue());
                    break;
                case INTEGER:
                    specs[i] = new IntCell(var.getIntValue());
                    break;
                case STRING:
                    String sValue = var.getStringValue();
                    specs[i] = new StringCell(sValue == null ? "" : sValue);
                    break;
                }
            }
        }
        cont.addRowToTable(new DefaultRow("values", specs));
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec()};
    }
    
    private DataTableSpec createOutSpec() throws InvalidSettingsException {
        List<Pair<String, ScopeVariable.Type>> vars = 
            m_settings.getVariablesOfInterest();
        if (vars.isEmpty()) {
            throw new InvalidSettingsException("No variables selected");
        }
        DataColumnSpec[] specs = new DataColumnSpec[vars.size()];
        for (int i = 0; i < vars.size(); i++) {
            Pair<String, ScopeVariable.Type> c = vars.get(i);
            DataType type;
            switch (c.getSecond()) {
            case DOUBLE:
                type = DoubleCell.TYPE;
                break;
            case INTEGER:
                type = IntCell.TYPE;
                break;
            case STRING:
                type = StringCell.TYPE;
                break;
            default:
                throw new InvalidSettingsException("Unsupported variable type: "
                        + c.getSecond());
            }
            specs[i] = 
                new DataColumnSpecCreator(c.getFirst(), type).createSpec();
        }
        return new DataTableSpec(specs);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new VariableToTableSettings().loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
