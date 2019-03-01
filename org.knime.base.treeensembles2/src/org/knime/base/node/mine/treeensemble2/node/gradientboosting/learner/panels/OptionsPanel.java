/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   22.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.SplitCriterion;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class OptionsPanel extends JPanel {

    static final DataTableSpec NO_VALID_INPUT_SPEC =
        new DataTableSpec(new DataColumnSpecCreator("<no valid input>", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("<no valid fingerprint input>", DenseBitVectorCell.TYPE).createSpec());

    // Attribute selection

    private final ColumnSelectionComboxBox m_targetColumnBox;

    private final JRadioButton m_useFingerprintColumnRadio;

    private final JRadioButton m_useOrdinaryColumnsRadio;

    private final ColumnSelectionComboxBox m_fingerprintColumnBox;

    private final DataColumnSpecFilterPanel m_includeColumnsFilterPanel2;

    // Tree Options

    private final JCheckBox m_maxLevelChecker;

    private final JSpinner m_maxLevelSpinner;

    // Boosting Options

    private final JSpinner m_nrModelsSpinner;

    private final JSpinner m_learningRateSpinner;


    private final boolean m_isRegression;

    /**
     * @param isRegression
     */
    @SuppressWarnings("unchecked")
    public OptionsPanel(final boolean isRegression) {
        super(new GridBagLayout());
        Class<? extends DataValue> targetClass = isRegression ? DoubleValue.class : NominalValue.class;
        m_targetColumnBox = new ColumnSelectionComboxBox((Border)null, targetClass);
        m_targetColumnBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    newTargetSelected();
                }
            });
        m_fingerprintColumnBox = new ColumnSelectionComboxBox((Border)null,
            new DataValueColumnFilter(BitVectorValue.class, ByteVectorValue.class, DoubleVectorValue.class));
        m_includeColumnsFilterPanel2 = new DataColumnSpecFilterPanel();

        m_useFingerprintColumnRadio = new JRadioButton("Use fingerprint attribute");
        m_useOrdinaryColumnsRadio = new JRadioButton("Use column attributes");
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_useFingerprintColumnRadio);
        bg.add(m_useOrdinaryColumnsRadio);
        ActionListener actListener = e -> {
                boolean isFP = bg.getSelection() == m_useFingerprintColumnRadio.getModel();
                m_fingerprintColumnBox.setEnabled(isFP);
                m_includeColumnsFilterPanel2.setEnabled(!isFP);
            };
        m_useFingerprintColumnRadio.addActionListener(actListener);
        m_useOrdinaryColumnsRadio.addActionListener(actListener);
        m_useFingerprintColumnRadio.doClick();

        // Tree Options

        m_maxLevelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
        m_maxLevelChecker = new JCheckBox("Limit number of levels (tree depth)");
        m_maxLevelChecker.addItemListener(e -> m_maxLevelSpinner.setEnabled(m_maxLevelChecker.isSelected()));
        m_maxLevelChecker.doClick();

        // Boosting Options

        m_nrModelsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 2));

        m_learningRateSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0, 1, 0.05));

        m_isRegression = isRegression;
        initPanel();
    }

    private void initPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Target Column "), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_targetColumnBox, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel(""), gbc);
        gbc.gridy += 1;
        add(new JLabel("Attribute Selection"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_useFingerprintColumnRadio, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_fingerprintColumnBox, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_useOrdinaryColumnsRadio, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        add(m_includeColumnsFilterPanel2, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel(""), gbc);

        // Tree Options

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Tree Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        add(m_maxLevelChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_maxLevelSpinner, gbc);

        // Boosting Options

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        add(new JSeparator(), gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        add(new JLabel("Boosting Options"), gbc);
        gbc.gridwidth = 1;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Number of models"), gbc);
        gbc.gridwidth = 2;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_nrModelsSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        add(new JLabel("Learning rate"), gbc);
        gbc.gridwidth = 2;
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        add(m_learningRateSpinner, gbc);

    }

    /**
     * Load settings from config <b>cfg</b> and table spec <b>inSpec</b>
     *
     * @param inSpec
     * @param cfg
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final DataTableSpec inSpec, final GradientBoostingLearnerConfiguration cfg)
        throws NotConfigurableException {
        loadColumnSettings(inSpec, cfg);
        loadTreeSettings(cfg);
        loadBoostingSettings(cfg);
    }

    /**
     * @param cfg
     */
    private void loadBoostingSettings(final GradientBoostingLearnerConfiguration cfg) {
        m_nrModelsSpinner.setValue(cfg.getNrModels());
        m_learningRateSpinner.setValue(cfg.getLearningRate());
    }

    /**
     * @param cfg
     */
    private void loadTreeSettings(final GradientBoostingLearnerConfiguration cfg) {
        int maxLevel = cfg.getMaxLevels();
        if ((maxLevel != TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) != m_maxLevelChecker.isSelected()) {
            m_maxLevelChecker.doClick();
        }
        if (maxLevel == TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE) {
            m_maxLevelSpinner.setValue(10);
        } else {
            m_maxLevelSpinner.setValue(maxLevel);
        }
    }

    /**
     * @param inSpec {@link DataTableSpec} of the input table
     * @param cfg loaded configuration
     * @throws NotConfigurableException
     */
    private void loadColumnSettings(final DataTableSpec inSpec, final GradientBoostingLearnerConfiguration cfg)
        throws NotConfigurableException {
        boolean hasOrdinaryColumnsInInput = inputHasOrdinaryColumns(inSpec);
        boolean hasFPColumnInInput = inputHasVectorColumn(inSpec);

        String fpColumn = cfg.getFingerprintColumn();
        m_useOrdinaryColumnsRadio.setEnabled(true);
        m_useFingerprintColumnRadio.setEnabled(true);
        m_useOrdinaryColumnsRadio.doClick(); // default, fix later
        if (hasOrdinaryColumnsInInput) {
            m_includeColumnsFilterPanel2.loadConfiguration(cfg.getColumnFilterConfig(), inSpec);
        } else {
            m_useOrdinaryColumnsRadio.setEnabled(false);
            m_useFingerprintColumnRadio.doClick();
            m_includeColumnsFilterPanel2.loadConfiguration(cfg.getColumnFilterConfig(), NO_VALID_INPUT_SPEC);
        }
        if (hasFPColumnInInput) {
            m_fingerprintColumnBox.update(inSpec, fpColumn);
        } else {
            m_useOrdinaryColumnsRadio.doClick();
            m_fingerprintColumnBox.update(NO_VALID_INPUT_SPEC, "");
            m_useFingerprintColumnRadio.setEnabled(false);
            fpColumn = null;
        }
        if (fpColumn != null || !hasOrdinaryColumnsInInput) {
            m_useFingerprintColumnRadio.doClick();
        } else {
            m_useOrdinaryColumnsRadio.doClick();
        }

        m_targetColumnBox.update(inSpec, cfg.getTargetColumn());
    }

    /**
     * @param inSpec {@link DataTableSpec} of the input table
     * @return true if the input contains a supported vector column
     */
    private static boolean inputHasVectorColumn(final DataTableSpec inSpec) {
        return inSpec.containsCompatibleType(BitVectorValue.class) || inSpec.containsCompatibleType(ByteVectorValue.class)
            || inSpec.containsCompatibleType(DoubleVectorValue.class);
    }

    /**
     * @param inSpec {@link DataTableSpec} of the input table
     * @return true if the input table contains at least one supported ordinary column
     */
    private static boolean inputHasOrdinaryColumns(final DataTableSpec inSpec) {
        int nrNominalCols = 0;
        int nrNumericCols = 0;
        for (DataColumnSpec col : inSpec) {
            DataType type = col.getType();
            if (type.isCompatible(NominalValue.class)) {
                nrNominalCols += 1;
            } else if (type.isCompatible(DoubleValue.class)) {
                nrNumericCols += 1;
            }
        }
        return nrNominalCols > 1 || nrNumericCols > 0;
    }

    /**
     * Save settings in config <b>cfg</b>
     *
     * @param cfg config to save to
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void saveSettings(final GradientBoostingLearnerConfiguration cfg) throws InvalidSettingsException {
        cfg.setTargetColumn(m_targetColumnBox.getSelectedColumn());
        if (m_useFingerprintColumnRadio.isSelected()) {
            String fpColumn = m_fingerprintColumnBox.getSelectedColumn();
            cfg.setFingerprintColumn(fpColumn);
        } else {
            assert m_useOrdinaryColumnsRadio.isSelected();
            Set<String> incls = m_includeColumnsFilterPanel2.getIncludedNamesAsSet();
            if (incls.isEmpty()) {
                throw new InvalidSettingsException("No learn columns selected");
            }
        }
        m_includeColumnsFilterPanel2.saveConfiguration(cfg.getColumnFilterConfig());
        cfg.setIgnoreColumnsWithoutDomain(true);
        cfg.setSaveTargetDistributionInNodes(false);

        // Tree Options

        int maxLevel = m_maxLevelChecker.isSelected() ? (Integer)m_maxLevelSpinner.getValue()
            : TreeEnsembleLearnerConfiguration.MAX_LEVEL_INFINITE;
        cfg.setMaxLevels(maxLevel);

        cfg.setHardCodedRootColumn(null);

        // Boosting Options

        cfg.setNrModels((Integer)m_nrModelsSpinner.getValue());
        cfg.setLearningRate((Double)m_learningRateSpinner.getValue());

        // will not be used in model but causes nullpointer exception if not set.
        cfg.setSplitCriterion(SplitCriterion.Gini);

    }

    /**
     */
    private void newTargetSelected() {
        DataColumnSpec col = (DataColumnSpec)m_targetColumnBox.getSelectedItem();
        if (col == null) {
            return;
        }
        m_includeColumnsFilterPanel2.resetHiding();
        m_includeColumnsFilterPanel2.hideNames(col);
    }

    /**
     * @return the isRegression
     */
    boolean isRegression() {
        return m_isRegression;
    }
}
