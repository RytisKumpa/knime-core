/* 
 * -------------------------------------------------------------------
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
 *   07.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions.job;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This implements an Eclipse job for managing (sub) jobs that execute node(s)
 * inside a <code>WorkflowManager</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeExecutionManagerJob extends Job implements WorkflowListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeExecutionManagerJob.class);

    private WorkflowManager m_manager;

    private NodeContainerEditPart[] m_parts;

    private boolean m_finished;

    private IProgressMonitor m_monitor;

    private ArrayList<Job> m_scheduledJobs;

    private int m_counter;

    /**
     * @param manager The manager that hosts the nodes
     * @param nodeParts Controller edit parts that manage the nodes that should
     *            be executed.
     */
    public NodeExecutionManagerJob(final WorkflowManager manager,
            final NodeContainerEditPart[] nodeParts) {
        super("Execution Manager");
        m_manager = manager;
        m_parts = nodeParts;
        m_counter = 0;

    }

    /**
     * Create jobs for the currently available nodes.
     * 
     * @param monitor The monitor
     */
    private synchronized void createJobsForAvailableNodes(
            final IProgressMonitor monitor) {
        // start a sub-job for each available node
        NodeContainer nextNode = m_manager.getNextExecutableNode();
        while (nextNode != null) {
            m_counter++;
            // start the job
            monitor.beginTask("Executing " + nextNode.getNodeNameWithID(),
                    IProgressMonitor.UNKNOWN);
            SingleNodeJob subJob = new SingleNodeJob(m_manager, nextNode,
                    new SubProgressMonitor(monitor, 100), 100);

            // Sub jobs do the main work and therefore
            // have a high priority (unlike the "system" priority of the overall
            // execution Manager
            subJob.setUser(false);
            subJob.setSystem(false);
            subJob.setPriority(Job.LONG);
            subJob.schedule();

            // we remember the job so that we can join it at the end
            m_scheduledJobs.add(subJob);
            // get next job
            nextNode = m_manager.getNextExecutableNode();
        }
    }

    /**
     * @see org.eclipse.core.runtime.jobs.Job
     *      #run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {

        // we must register on the manager to get lifecycle events
        m_manager.addListener(this);
        m_monitor = monitor;
        // list to remember the sub jobs
        m_scheduledJobs = new ArrayList<Job>();

        m_finished = false;

        // state to return to user (OK, cancel)
        IStatus state = Status.OK_STATUS;

        // loop over all parts, and start sub-jobs
        m_finished = false;

        for (int i = 0; i < m_parts.length; i++) {

            // This is the node up to which the flow should be executed.
            NodeContainer container = m_parts[i].getNodeContainer();
            // check if the part is already locked execution
            if (m_parts[i].isLocked()) {
                LOGGER.warn("Node is (already?) locked, skipping...");
                continue;
            }

            // create a monitor object
            LOGGER.info("executing up to node #" + container.getID());
            monitor.beginTask("Executing flow up to "
                    + container.getNodeNameWithID(), IProgressMonitor.UNKNOWN);

            // Prepare execution
            m_manager.prepareForExecUpToNode(container.getID());

            // create and schedule jobs for currently available nodes

            // explicit synchronization to avoid deadlock
            synchronized (m_manager) {
                createJobsForAvailableNodes(m_monitor);
            
            }
        }

        // wait until the state changes or the monitor was chanceled
        while (!(m_finished || monitor.isCanceled())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //
            // User canceled (pressed terminate button)
            //
            if (monitor.isCanceled()) {
                LOGGER.info("User requested cancelation, "
                        + "stopping scheduled sub-jobs !");
                // try to cancel all sub jobs
                for (int j = 0; j < m_scheduledJobs.size(); j++) {
                    m_scheduledJobs.get(j).cancel();
                }
                m_finished = true;
                state = Status.CANCEL_STATUS;
                break;
            }

        }

        // We must wait for all jobs to end
        for (int i = 0; i < m_scheduledJobs.size(); i++) {
            Job job = m_scheduledJobs.get(i);
            LOGGER.debug("Waiting for remaining job to finish: "
                    + job.getName());
            try {
                job.join();
            } catch (InterruptedException e) {
                LOGGER.error("Could not join job: " + job.getName(), e);
            }
        }
        if (m_counter == 0) {
            LOGGER.warn("No nodes could be executed!");
        }

        LOGGER.info("Execution finished (" + m_counter + " node(s))");
        m_manager.removeListener(this);
        return state;
    }

    /**
     * Creates new jobs if there's another node ready to be executed or set the
     * 'finished' flag.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        switch (event.getEventType()) {
        case WorkflowEvent.EXEC_POOL_CHANGED:
            LOGGER.info("Exec pool has changed...");
            m_finished = false;

            // explicit synchronization to avoid deadlock
            synchronized (m_manager) {
                createJobsForAvailableNodes(m_monitor);
            }
            break;

        case WorkflowEvent.EXEC_POOL_DONE:
            LOGGER.info("Execution pool has finished...");
            m_finished = true;
            break;
        default:
            break;
        }

    }

}
