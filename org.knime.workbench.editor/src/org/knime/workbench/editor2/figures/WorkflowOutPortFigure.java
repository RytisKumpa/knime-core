/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   22.01.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.PortType;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortFigure extends AbstractWorkflowPortFigure  {

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowOutPortFigure.class);

    /**
     *
     * @param type port type (data, model, etc)
     * @param nrOfPorts total number of ports
     * @param portIndex port index
     * @param wfmName name of the subworkflow for tooltip
     */
    public WorkflowOutPortFigure(final PortType type,
            final int nrOfPorts, final int portIndex, final String wfmName) {
        super(type, nrOfPorts, portIndex);
        setToolTip(new NewToolTipFigure(wfmName + " out port: " + portIndex));
    }


    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected PointList createShapePoints(final Rectangle rect) {
        if (getType().equals(BufferedDataTable.TYPE)) {
            // triangle
            Rectangle parent = getParent().getBounds().getCopy();
            int yPos = (parent.height / (getNrPorts() + 1)) 
                * (getPortIndex() + 1);
            getBounds().x = parent.width - SIZE;
            getBounds().y = yPos;
            getBounds().width = SIZE;
            getBounds().height = SIZE;
            Rectangle r = getBounds().getCopy();
            PointList list = new PointList(3);
            list.addPoint(r.x, r.y);
            list.addPoint(r.x + r.width, r.y + (r.height / 2));
            list.addPoint(r.x, r.y + r.height);
            return list;
        } else {
            // square
            Rectangle parent = getParent().getBounds().getCopy();
            int yPos = (parent.height / (getNrPorts() + 1)) 
                * (getPortIndex() + 1);
            int xPos = parent.width - SIZE;
            getBounds().x = xPos;
            getBounds().y = yPos;
            getBounds().width = SIZE;
            getBounds().height = SIZE;
            PointList list = new PointList(4);
            list.addPoint(new Point(xPos, yPos));
            list.addPoint(new Point(xPos + SIZE, yPos));
            list.addPoint(new Point(xPos + SIZE, yPos + SIZE));
            list.addPoint(new Point(xPos, yPos + SIZE));
            return list;
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Locator getLocator() {
        return new WorkflowPortLocator(getType(), getPortIndex(),
                true, getNrPorts());
    }
}
