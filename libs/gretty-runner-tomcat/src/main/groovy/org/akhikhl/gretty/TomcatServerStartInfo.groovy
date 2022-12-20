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
import org.apache.catalina.Context
import org.apache.catalina.connector.Connector
import org.apache.catalina.startup.Tomcat
import org.apache.juli.logging.Log
import org.apache.juli.logging.LogFactory

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class TomcatServerStartInfo {

  private static final Log log = LogFactory.getLog(TomcatServerStartInfo)

  def getInfo(Tomcat tomcat, Connector[] connectors, Map params) {

    if(connectors == null)
      connectors = tomcat.service.findConnectors()

    def portsInfo = connectors.collect { it.localPort }
    portsInfo = (portsInfo.size() == 1 ? 'port ' : 'ports ') + portsInfo.join(', ')
    log.info "${params.servletContainerDescription} started and listening on ${portsInfo}"

    Connector httpConn = connectors.find { it.scheme == 'http' }
    Connector httpsConn = connectors.find { it.scheme == 'https' }

    List contextInfo = []

    String host = tomcat.hostname == '0.0.0.0' ? 'localhost' : tomcat.hostname

    for(Context context in tomcat.host.findChildren().findAll { it instanceof Context }) {
      log.info "${context.name - '/'} runs at:"
      if(httpConn) {
        log.info "  http://${host}:${httpConn.localPort}${context.path}"
        contextInfo.add([
          protocol: 'http',
          host: host,
          port: httpConn.localPort,
          contextPath: context.path,
          baseURI: "http://${host}:${httpConn.localPort}${context.path}"
        ])
      }
      if(httpsConn) {
        log.info "  https://${host}:${httpsConn.localPort}${context.path}"
        contextInfo.add([
          protocol: 'https',
          host: host,
          port: httpsConn.localPort,
          contextPath: context.path,
          baseURI: "https://${host}:${httpsConn.localPort}${context.path}"
        ])
      }
    }

    def serverStartInfo = [ status: 'successfully started' ]

    serverStartInfo.host = host

    if(httpConn)
      serverStartInfo.httpPort = httpConn.localPort

    if(httpsConn)
      serverStartInfo.httpsPort = httpsConn.localPort

    serverStartInfo.contexts = contextInfo
    serverStartInfo
  }
}
