import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import test._
import test.DefaultHandlers._

object TestServer extends Application {

  val jsonOk = (request: HttpServletRequest, response: HttpServletResponse) => {
    response.setContentType("application/json;charset=UTF-8")
    response.setStatus(HttpServletResponse.SC_OK)
    val queryString: String = if (request.getQueryString == null) "" else "?" + request.getQueryString
    val pathInfo: String = request.getPathInfo
    log.fine("Request: "+pathInfo)
    response.getWriter.println("{ \"status\":200, \"query\":\"" + pathInfo + queryString + "\" }")
  }

  val jsonError = (request: HttpServletRequest, response: HttpServletResponse) => {
    response.setContentType("application/json;charset=UTF-8")
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    response.getWriter.println("{ \"status\":500 }")    
  }

  new JettyServer(List(sleeper(minWaitInMillis = 1000, maxWaitInMillis = 2000), randomErrors(errorPercentage = 50)), okWriter = jsonOk, errorWriter = jsonError).start()
}
