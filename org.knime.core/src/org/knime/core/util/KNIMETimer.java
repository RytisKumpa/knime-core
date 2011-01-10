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
 * ---------------------------------------------------------------------
 * 
 * History
 *   01.08.2007 (thor): created
 */
package org.knime.core.util;

import java.util.Timer;

/**
 * This final singleton class is a global timer available for all classes inside
 * KNIME. This timer is especially useful for nodes that execute external code
 * which is not aware of execution canceling and such stuff.
 * 
 * <b>Users of this timer must make sure, that the scheduled tasks are
 * fast-running, otherwise other tasks will be blocked.</b>
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class KNIMETimer extends Timer {
    private static final KNIMETimer INSTANCE = new KNIMETimer();

    private KNIMETimer() {
        super("Global KNIME Timer");
    }

    /**
     * Do not call this method, it won't let you cancel this timer, but
     * instead throw an {@link UnsupportedOperationException} at you.
     */
    @Override
    public void cancel() {
        throw new UnsupportedOperationException(
                "You must not cancel the global timer!");
    }

    /**
     * Returns the singleton instance of the global KNIME timer.
     * 
     * @return a timer
     */
    public static KNIMETimer getInstance() {
        return INSTANCE;
    }
}
