package test

import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler
import java.util.logging.{Level, LogRecord, ConsoleHandler, Logger}

object DefaultWriters {
  def defaultContentType(response:HttpServletResponse) {response.setContentType("text/plain; charset=utf-8")}
  def defaultOkWriter(request:HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_OK)
    defaultContentType(response)
    response.getWriter.println("OK")
  }

  def defaultErrorWriter(request:HttpServletRequest, response: HttpServletResponse) {
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    defaultContentType(response)
    response.getWriter.println("ERROR")
  }
}

object DefaultHandlers {
  val log = Logger.getLogger("handlers");

  def sleeper(minWaitInMillis:Int, maxWaitInMillis:Int) = (request: HttpServletRequest, response: HttpServletResponse) => {
    val waitTimeRange = maxWaitInMillis-minWaitInMillis
    val sleepMs = (scala.util.Random.nextDouble * waitTimeRange).asInstanceOf[Long] + minWaitInMillis
    log.finest("sleeping for %d ms".format(sleepMs))
    try {
      Thread.sleep(sleepMs)
    }
    catch {
      case e: InterruptedException => {
        Thread.currentThread.interrupt()
        throw new RuntimeException("interrupted")
      }
    }
  }

  def randomErrors(errorPercentage: Int) = (request: HttpServletRequest, response: HttpServletResponse) => {
    val errorTreshold = errorPercentage * 1000
    val seed: Int = scala.util.Random.nextInt(100000)
    val error = seed < (100000 - errorTreshold)
    log.finest("isError=%b, random=%d, treshold=%d".format(error, seed, errorTreshold))
    if (error) throw new RandomErrorException
  }
}

class RandomErrorException extends RuntimeException

class JettyServer(
                   handlers: List[(HttpServletRequest,HttpServletResponse) => Unit],
                   okWriter:(HttpServletRequest,HttpServletResponse) => Unit = DefaultWriters.defaultOkWriter,
                   errorWriter:(HttpServletRequest,HttpServletResponse) => Unit = DefaultWriters.defaultErrorWriter) {

  require (!handlers.isEmpty)

  System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.JavaUtilLog")
  setupLogging()

  val server = new Server
  val connector = new SelectChannelConnector
  val log = Logger.getLogger("jetty")

  connector.setPort(8080)
  server.setConnectors(Array[Connector](connector))
  server.setHandler(new AbstractHandler {
    def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
      log.fine("handling request, calling handler functions "+handlers)
      var failed = false
      handlers foreach (x => {
        try {
          x(request, response)
        } catch {
          case e:RandomErrorException =>
            errorWriter(request, response)
            failed = true
          case e:RuntimeException =>
            log.log(Level.SEVERE, "Error", e)
        }
      })
      if(!failed) okWriter(request, response)
      baseRequest.setHandled(true)
    }
  })

  def start() {
    server.start()
    server.join()
  }

  def setupLogging() {
    val logger: Logger = Logger.getLogger("")
    logger.getHandlers.foreach(h => logger.removeHandler(h))
    logger.addHandler(new ConsoleHandler {
      override def publish(p1: LogRecord) {
        println(format("%tT:%-5.5s:%s:%s", p1.getMillis, p1.getLevel, p1.getLoggerName, p1.getMessage))
        if(p1.getThrown != null) {
          p1.getThrown.printStackTrace(Console.out)
        }
      }
    })
    logger.setLevel(Level.ALL)
    Logger.getLogger("org.eclipse.jetty.util.log").setLevel(Level.INFO)
  }
}