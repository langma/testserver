package test

import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyServer(handler: RequestHandler) {
  val server = new Server
  val connector: SelectChannelConnector = new SelectChannelConnector
  connector.setPort(8080)
  server.setConnectors(Array[Connector](connector))
  server.setHandler(handler)

  def start() {
    server.start()
    server.join()
  }
}

trait Sleeping extends RequestHandler {
  abstract override def handleSuccess(request: HttpServletRequest, response: HttpServletResponse) {
    sleep()
    super.handleSuccess(request, response)
  }

  abstract override def handleError(request: HttpServletRequest, response: HttpServletResponse) {
    sleep()
    super.handleError(request, response)
  }

  private def sleep() {
    try {
      Thread.sleep((scala.util.Random.nextDouble * 100).asInstanceOf[Long] + 10)
    }
    catch {
      case e: InterruptedException => {
        Thread.currentThread.interrupt()
        throw new RuntimeException("interrupted")
      }
    }
  }
}

trait RandomErrors extends RequestHandler {
  val errorPercentage: Float
  val errorTreshold = errorPercentage * 1000.0f

  abstract override def isError: Boolean = {
    scala.util.Random.nextInt(100000) < (100000 - errorTreshold)
  }
}

trait JSONReplier extends RequestHandler {
  abstract override def setContentType(response: HttpServletResponse) {
    response.setContentType("application/json;charset=UTF-8")
  }

  abstract override def handleSuccess(request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_OK)
    val queryString: String = if (request.getQueryString == null) "" else "?" + request.getQueryString
    val pathInfo: String = request.getPathInfo
    response.getWriter.println("{ \"status\":200, \"query\":\"" + pathInfo + queryString + "\" }")
  }

  abstract override def handleError(request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    response.getWriter.println("{ \"status\":500 }")
  }

}

class RequestHandler extends AbstractHandler {

  def setContentType(response: HttpServletResponse) {
    response.setContentType("text/html;charset=UTF8")
  }

  def isError: Boolean = false;

  def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
    setContentType(response)
    baseRequest.setHandled(true)
    if (isError)
      handleError(request, response)
    else
      handleSuccess(request, response)
  }

  def handleSuccess(request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_OK)
  }

  def handleError(request: HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
  }
}


