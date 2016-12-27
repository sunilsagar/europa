/*
  $Id: $
  @file MonitorQueue.java
  @brief Contains the MonitorQueue.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.inject.Singleton;
import com.distelli.persistence.PageIterator;

import org.apache.log4j.Logger;
import com.distelli.europa.models.*;
import com.distelli.europa.db.*;
import javax.inject.Inject;

//This must be annotated with @Singleton so that there is a
//single instance of this object
@Singleton
public class MonitorQueue
{
    private static final Logger log = Logger.getLogger(MonitorQueue.class);

    private List<ContainerRepo> _reposToMonitor;
    private boolean _shouldReload = false;

    @Inject
    private ContainerRepoDb _containerRepoDb;
    @Inject
    private MonitorTaskFactory _monitorTaskFactory;

    public MonitorQueue()
    {

    }

    public synchronized void setReload(boolean shouldReload)
    {
        _shouldReload = shouldReload;
    }

    private void reload()
    {
        _reposToMonitor = new ArrayList<ContainerRepo>();
        PageIterator pageIterator = new PageIterator().pageSize(1000);
        do {
            List<ContainerRepo> repos = _containerRepoDb.listRepos(null, pageIterator);
            _reposToMonitor.addAll(repos);
        } while(pageIterator.getMarker() != null);
        _shouldReload = false;
    }

    public synchronized MonitorTaskList getMonitorTasks()
    {
        if(_reposToMonitor == null || _shouldReload == true)
            reload();

        List<MonitorTask> tasks = new ArrayList<MonitorTask>();
        MonitorTaskList taskList = new MonitorTaskList();
        CountDownLatch latch = new CountDownLatch(_reposToMonitor.size());
        taskList.setCountDownLatch(latch);
        for(ContainerRepo repo : _reposToMonitor)
        {
            MonitorTask task = _monitorTaskFactory.createMonitorTask(repo, latch);
            tasks.add(task);
        }
        taskList.setTasks(tasks);
        return taskList;
    }
}