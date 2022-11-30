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

/**
 *
 * @author akhikhl
 */
@CompileStatic(TypeCheckingMode.SKIP)
class JettyServerStartInfo {

  Map getInfo(server, JettyConfigurer configurer, Map params) {

    def portsInfo = server.getConnectors().collect { it.localPort }
    portsInfo = (portsInfo.size() == 1 ? 'port ' : 'ports ') + portsInfo.join(', ')
    configurer.info '{} started and listening on {}', params.servletContainerDescription, portsInfo

    def httpConn = configurer.findHttpConnector(server)
    def httpsConn = configurer.findHttpsConnector(server)

    List contextInfo = []

    def collectContextInfo
    collectContextInfo = { handler ->
      if(handler.respondsTo('getHandlers')) {
        for(def h in handler.getHandlers()) {
          collectContextInfo(h)
        }
      }
      if(handler.respondsTo('getDisplayName'))
        configurer.info '{} runs at:', (handler.getDisplayName() - '/')
      if(handler.respondsTo('getContextPath')) {
        String contextPath = handler.getContextPath()
        if(httpConn) {
          String host = httpConn.host == '0.0.0.0' ? 'localhost' : httpConn.host          
          configurer.info '  http://{}:{}{}', host, httpConn.localPort, contextPath
          contextInfo.add([
            protocol: 'http',
            host: host,
            port: httpConn.localPort,
            contextPath: contextPath,
            baseURI: "http://${host}:${httpConn.localPort}${contextPath}"
          ])
        }
        if(httpsConn) {
          String host = httpsConn.host == '0.0.0.0' ? 'localhost' : httpsConn.host
          configurer.info '  https://{}:{}{}', host, httpsConn.localPort, contextPath
          contextInfo.add([
            protocol: 'https',
            host: host,
            port: httpsConn.localPort,
            contextPath: contextPath,
            baseURI: "https://${host}:${httpsConn.localPort}${contextPath}"
          ])
        }
      }
    }

    collectContextInfo(server.handler)

    Map serverStartInfo = [ status: 'successfully started' ]

    serverStartInfo.host = httpConn?.host ?: httpsConn?.host
    if(serverStartInfo.host == '0.0.0.0')
      serverStartInfo.host = 'localhost'

    if(httpConn)
      serverStartInfo.httpPort = httpConn.localPort

    if(httpsConn)
      serverStartInfo.httpsPort = httpsConn.localPort

    serverStartInfo.contexts = contextInfo
    serverStartInfo
  }
}

