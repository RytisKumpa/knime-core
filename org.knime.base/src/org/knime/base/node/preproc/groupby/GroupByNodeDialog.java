/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.dialogutil.AggregationColumnPanel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * The node dialog of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class GroupByNodeDialog extends NodeDialogPane {

    /**The width of the default component.*/
    public static final int DEFAULT_WIDTH = 680;

    /**The height of the default component.*/
    public static final int DEFAULT_HEIGHT = 550;

    private final JPanel m_panel;

    private final SettingsModelFilterString m_groupByCols =
        new SettingsModelFilterString(GroupByNodeModel.CFG_GROUP_BY_COLUMNS);

    private final SettingsModelIntegerBounded m_maxUniqueValues =
        new SettingsModelIntegerBounded(
                GroupByNodeModel.CFG_MAX_UNIQUE_VALUES, 10000, 1,
                Integer.MAX_VALUE);

    private final SettingsModelBoolean m_enableHilite =
        new SettingsModelBoolean(GroupByNodeModel.CFG_ENABLE_HILITE, false);

    private final SettingsModelBoolean m_sortInMemory =
        new SettingsModelBoolean(GroupByNodeModel.CFG_SORT_IN_MEMORY, false);

    private final SettingsModelBoolean m_retainOrder =
        new SettingsModelBoolean(GroupByNodeModel.CFG_RETAIN_ORDER, false);

    private final SettingsModelBoolean m_inMemory =
        new SettingsModelBoolean(GroupByNodeModel.CFG_IN_MEMORY, false);

    private final SettingsModelString m_columnNamePolicy =
        new SettingsModelString(GroupByNodeModel.CFG_COLUMN_NAME_POLICY,
                ColumnNamePolicy.getDefault().getLabel());

    private final DialogComponentColumnFilter m_groupCol;

    private final AggregationColumnPanel m_aggrColPanel =
        new AggregationColumnPanel();

    /**Constructor for class NewGroupByNodeDialogPane.
     *
     */
    public GroupByNodeDialog() {
        //create the root tab
        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));
        addTab("Options", m_panel);

//The group column box
        m_groupCol =
            new DialogComponentColumnFilter(m_groupByCols, 0, false);
        m_groupCol.setIncludeTitle(" Group column(s) ");
        m_groupCol.setExcludeTitle(" Available column(s) ");
        m_groupByCols.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                //remove all group columns from the aggregation column list
                groupByColsChanged();
            }
        });
        final JPanel groupColPanel = m_groupCol.getComponentPanel();
        groupColPanel.setLayout(new GridLayout(1, 1));
        groupColPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Group settings "));
        m_panel.add(groupColPanel);

//The aggregation column box
        m_panel.add(m_aggrColPanel.getComponentPanel());

//The advanced settings box
        final JComponent advancedBox = createAdvancedOptionsBox();
        m_panel.add(advancedBox);

//calculate the component size
        int width = (int)Math.max(m_groupCol.getComponentPanel().
                getMinimumSize().getWidth(), m_aggrColPanel.
                    getComponentPanel().getMinimumSize().getWidth());
        width = (int)Math.max(width, advancedBox.getMinimumSize().getWidth());
        width = Math.max(width, DEFAULT_WIDTH);
        final Dimension dimension =
            new Dimension(width, DEFAULT_HEIGHT);
        m_panel.setMinimumSize(dimension);
        m_panel.setPreferredSize(dimension);

        //add the  process in memory change listener
        m_inMemory.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                inMemoryChanged();
            }
        });
        inMemoryChanged();
        final Component descriptionTab =
            AggregationMethods.createDescriptionPane();
        descriptionTab.setMinimumSize(dimension);
        descriptionTab.setMaximumSize(dimension);
        descriptionTab.setPreferredSize(dimension);
        addTab("Description", descriptionTab);
    }

    /**
     * Call this method if the process in memory flag has changed.
     */
    protected void inMemoryChanged() {
        m_retainOrder.setEnabled(!m_inMemory.getBooleanValue());
        m_sortInMemory.setEnabled(!m_inMemory.getBooleanValue());
    }

    private JComponent createAdvancedOptionsBox() {
//general option box
        final Box generalBox = new Box(BoxLayout.X_AXIS);
        final DialogComponent maxNoneNumericVals =
            new DialogComponentNumber(m_maxUniqueValues,
                    "Maximum unique values per group", new Integer(1000), 5);
        maxNoneNumericVals.setToolTipText("All groups with more unique values "
                + "will be skipped and replaced by a missing value");
        generalBox.add(maxNoneNumericVals.getComponentPanel());

        final DialogComponentStringSelection colNamePolicy =
            new DialogComponentStringSelection(m_columnNamePolicy, null,
                    ColumnNamePolicy.getPolicyLabels());
        generalBox.add(colNamePolicy.getComponentPanel());

//memory option box
        final Box memoryBox = new Box(BoxLayout.X_AXIS);
        final DialogComponent enableHilite = new DialogComponentBoolean(
                m_enableHilite, "Enable hiliting");
        memoryBox.add(enableHilite.getComponentPanel());
        memoryBox.add(Box.createHorizontalStrut(35));

        final DialogComponent inMemory = new DialogComponentBoolean(
                m_inMemory, "Process in memory");
        inMemory.setToolTipText(
        "Processes all data in memory.");
        memoryBox.add(inMemory.getComponentPanel());

        final DialogComponent sortInMemory = new DialogComponentBoolean(
                m_sortInMemory, "Sort in memory");
        memoryBox.add(sortInMemory.getComponentPanel());

        final DialogComponent retainOrder = new DialogComponentBoolean(
                m_retainOrder, "Retain row order");
        retainOrder.setToolTipText(
                "Retains the original row order of the input table.");
        memoryBox.add(retainOrder.getComponentPanel());

//Advanced settings box
        final Box box = new Box(BoxLayout.Y_AXIS);
        box.add(generalBox);
        box.add(memoryBox);
        box.setMaximumSize(box.getPreferredSize());
        final Box rootBox = new Box(BoxLayout.X_AXIS);
        rootBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Advanced settings "));
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(box);
        rootBox.add(Box.createHorizontalGlue());
        return rootBox;
    }

    /**
     * Synchronizes the available aggregation column list and the
     * selected group columns.
     */
    void groupByColsChanged() {
        m_aggrColPanel.excludeColsChange(m_groupByCols.getIncludeList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        assert (specs.length == 1);
        final DataTableSpec spec = (DataTableSpec)specs[0];
        try {
            m_maxUniqueValues.loadSettingsFrom(settings);
            m_enableHilite.loadSettingsFrom(settings);
            m_sortInMemory.loadSettingsFrom(settings);
            m_columnNamePolicy.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }

        try {
            //this option was introduced in Knime 2.0
            m_aggrColPanel.loadSettingsFrom(settings, spec);
        } catch (final InvalidSettingsException e) {
            final List<ColumnAggregator> columnMethods =
                GroupByNodeModel.compGetColumnMethods(spec,
                        m_groupByCols.getIncludeList(), settings);
            m_aggrColPanel.initialize(spec, columnMethods);
        }
        try {
            //this option was introduced in Knime 2.0.3+
            m_retainOrder.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_retainOrder.setBooleanValue(false);
        }
        try {
            //this option was introduced in Knime 2.1.2+
            m_inMemory.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_inMemory.setBooleanValue(false);
        }
        m_groupCol.loadSettingsFrom(settings, new DataTableSpec[] {spec});
        groupByColsChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_groupCol.saveSettingsTo(settings);
        m_maxUniqueValues.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);
        m_sortInMemory.saveSettingsTo(settings);
        m_columnNamePolicy.saveSettingsTo(settings);
        m_aggrColPanel.saveSettingsTo(settings);
        m_retainOrder.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);
    }
}
