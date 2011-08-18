import test._

object TestServer extends Application {
  new JettyServer(new RequestHandler with JSONReplier with RandomErrors with Sleeping {val errorPercentage=0.1f}).start()
}
