/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gradle task for control over jetty
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
@DisableCachingByDefault
abstract class AppServiceTask extends DefaultTask {

  private static Logger log = LoggerFactory.getLogger(AppServiceTask)

  @TaskAction
  void action() {
    String command = getCommand()
    //
    ServerConfig serverConfig = new ServerConfig()
    ConfigUtils.complementProperties(serverConfig, project.gretty.serverConfig, ProjectUtils.getDefaultServerConfig(project))
    ProjectUtils.resolveServerConfig(project, serverConfig)
    //
    File portPropertiesFile = DefaultLauncher.getPortPropertiesFile(project, serverConfig)
    if(!portPropertiesFile.exists())
      throw new GradleException("Gretty seems to be not running, cannot send command '$command' to it.")
    Properties portProps = new Properties()
    portPropertiesFile.withReader 'UTF-8', {
      portProps.load(it)
    }
    int servicePort = portProps.servicePort as int

    log.debug 'Sending command {} to port {}', command, servicePort
    ServiceProtocol.createWriter(servicePort).write(command)
  }

  @Internal
  abstract String getCommand()
}
