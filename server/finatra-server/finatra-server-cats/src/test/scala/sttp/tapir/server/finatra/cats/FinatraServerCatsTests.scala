package sttp.tapir.server.finatra.cats

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO, Resource, Timer}
import sttp.tapir.Endpoint
import sttp.tapir.server.finatra.{FinatraRoute, FinatraServerOptions, FinatraServerTests}
import sttp.tapir.server.tests.ServerTests
import sttp.tapir.server.{DecodeFailureHandler, ServerDefaults, ServerEndpoint}
import sttp.tapir.tests.{Port, PortCounter}

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class FinatraServerCatsTests extends ServerTests[IO, Nothing, FinatraRoute] {
  override def streamingSupport: Boolean = false

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  override def pureResult[T](t: T): IO[T] = IO.pure(t)
  override def suspendResult[T](t: => T): IO[T] = IO.apply(t)

  override def route[I, E, O](
      e: ServerEndpoint[I, E, O, Nothing, IO],
      decodeFailureHandler: Option[DecodeFailureHandler] = None
  ): FinatraRoute = {
    implicit val serverOptions: FinatraServerOptions =
      FinatraServerOptions.default.copy(decodeFailureHandler = decodeFailureHandler.getOrElse(ServerDefaults.decodeFailureHandler))
    e.toRoute
  }

  override def routeRecoverErrors[I, E <: Throwable, O](e: Endpoint[I, E, O, Nothing], fn: I => IO[O])(implicit
      eClassTag: ClassTag[E]
  ): FinatraRoute = e.toRouteRecoverErrors(fn)

  override def server(routes: NonEmptyList[FinatraRoute], port: Port): Resource[IO, Unit] = FinatraServerTests.server(routes, port)

  override lazy val portCounter: PortCounter = new PortCounter(59000)
}
