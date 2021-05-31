package sttp.tapir.server.interpreter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.model.Uri._
import sttp.model._
import sttp.monad.MonadError
import sttp.tapir.TestUtil._
import sttp.tapir._
import sttp.tapir.model.{ConnectionInfo, ServerRequest, ServerResponse}
import sttp.tapir.server.interceptor._

import scala.collection.immutable

class ServerInterpreterTest extends AnyFlatSpec with Matchers {
  it should "call the interceptors in the correct order" in {
    var callTrail: List[String] = Nil

    class AddToTrailInterceptor(prefix: String) extends EndpointInterceptor[Id, Unit] {
      override def apply(responder: Responder[Id, Unit], endpointHandler: EndpointHandler[Id, Unit]): EndpointHandler[Id, Unit] =
        new EndpointHandler[Id, Unit] {
          override def onDecodeSuccess[I](
              ctx: DecodeSuccessContext[Id, I]
          )(implicit monad: MonadError[Id], bodyListener: BodyListener[Id, Unit]): Id[ServerResponse[Unit]] = {
            callTrail ::= s"$prefix success"
            endpointHandler.onDecodeSuccess(ctx)(idMonadError, unitBodyListener)
          }

          override def onDecodeFailure(
              ctx: DecodeFailureContext
          )(implicit monad: MonadError[Id], bodyListener: BodyListener[Id, Unit]): Id[Option[ServerResponse[Unit]]] = {
            callTrail ::= s"$prefix failure"
            endpointHandler.onDecodeFailure(ctx)(idMonadError, unitBodyListener)
          }
        }
    }

    val testRequest = new ServerRequest {
      override def protocol: String = ""
      override def connectionInfo: ConnectionInfo = ConnectionInfo(None, None, None)
      override def underlying: Any = ()
      override def pathSegments: List[String] = Nil
      override def queryParameters: QueryParams = QueryParams.fromSeq(List(("x", "1")))
      override def method: Method = Method.GET
      override def uri: Uri = uri"http://example.com"
      override def headers: immutable.Seq[Header] = Nil
    }

    // given
    val interceptor1 = new AddToTrailInterceptor("1")
    val interceptor2 = new RequestInterceptor[Id, Unit] {
      override def apply(
          responder: Responder[Id, Unit],
          requestHandler: EndpointInterceptor[Id, Unit] => RequestHandler[Id, Unit]
      ): RequestHandler[Id, Unit] = RequestHandler.from { (request, monad) =>
        callTrail ::= "2 request"
        requestHandler(new AddToTrailInterceptor("2")).apply(request)(monad)
      }
    }
    val interceptor3 = new AddToTrailInterceptor("3")

    val interpreter =
      new ServerInterpreter[Any, Id, Unit, Nothing](
        TestRequestBody,
        UnitToResponseBody,
        List(interceptor1, interceptor2, interceptor3),
        _ => ()
      )

    // when
    interpreter.apply(testRequest, endpoint.in(query[String]("x")).serverLogic[Id](_ => Right(())))

    // then
    callTrail.reverse shouldBe List("2 request", "1 success", "2 success", "3 success")
  }
}
