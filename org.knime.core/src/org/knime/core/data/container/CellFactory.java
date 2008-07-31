/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data.container;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionMonitor;

/**
 * Factory for a ColumnRearranger to provide new columns which are, e.g. 
 * appended to a given table.
 * @see org.knime.core.data.container.ColumnRearranger
 * @see org.knime.core.data.container.ColumnRearranger#append(CellFactory)
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface CellFactory {

    /**
     * Get the new cells for a given row. These cells are incorporated into the 
     * existing row. The way it is done is defined through the ColumnRearranger
     * using this object.
     * @param row The row of interest.
     * @return The new cells to that row.
     * @throws IllegalArgumentException  If there is no mapping available.
     */
    DataCell[] getCells(final DataRow row);
    
    /**
     * The column specs for the cells that are generated in the getCells()
     * method. This method is only called once, there is no need to cache
     * the return value. The length of the returned array must match the 
     * length of the array returned by the getCells(DataRow) method and also
     * the types must match, i.e. the type of the respective DataColumnSpec
     * must be of the same type or a super type of the cell as returned
     * by getCells(DataRow).
     * @return The specs to the newly created cells.
     */
    DataColumnSpec[] getColumnSpecs();
    
    /** This method is called when a row has been processed. It allows the 
     * implementor to set progress in the execution monitor and also some
     * meaningful progress message.
     * <p>Note, you don't need to check <code>exec.checkCanceled()</code> in
     * the implementation as this is done in the calling class.
     * @param curRowNr The number of the row just processed
     * @param rowCount The total number of rows.
     * @param lastKey The row's key.
     * @param exec The execution monitor to report progress to.
     */ 
    void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec);
        
}
