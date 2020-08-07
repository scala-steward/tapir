package sttp.tapir.server.stub

import io.circe.generic.auto._
import sttp.client._
import sttp.client.monad._
import sttp.client.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SttpStubServerTest extends AnyFlatSpec with Matchers {

  behavior of "SttpStubServer"
  implicit val idMonad: MonadError[Identity] = IdMonad

  it should "combine tapir endpoint with sttp stub" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint
      .in("api" / "sometest4")
      .in(query[Int]("amount"))
      .post
      .out(jsonBody[ResponseWrapper])

    implicit val backend = SttpBackendStub
      .apply(idMonad)
      .whenRequestMatchesEndpoint(endpoint)
      .thenSuccess(ResponseWrapper(1.0))
    val response: Identity[Response[Either[Unit, ResponseWrapper]]] = endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(11).send()

    response.body shouldBe Right(ResponseWrapper(1.0))
  }

  it should "combine tapir endpoint with sttp stub - errors" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint
      .in("api" / "sometest4")
      .in(query[Int]("amount"))
      .post
      .errorOut(jsonBody[ResponseWrapper])

    implicit val backend = SttpBackendStub
      .apply(idMonad)
      .whenRequestMatchesEndpoint(endpoint)
      .thenError(ResponseWrapper(1.0), StatusCode.BadRequest)
    val response: Identity[Response[Either[ResponseWrapper, Unit]]] = endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(11).send()

    response shouldBe Response(Left(ResponseWrapper(1.0)), StatusCode.BadRequest)
  }

  /*
  Mismatch between input value: (id1,11), and inputs:

  Vector(
    Tuple(
      Vector(
        Tuple(Vector(FixedPath(api, x, Info(None, List(), false)), PathCapture(Some(id), x, Info(None, List(), false))), x),
        Query(amount, x, Info(None, List(), false))
      ),
      x
    ),
    FixedMethod(POST, x, Info(None, List(), false))
  )
   */

  it should "combine tapir endpoint with sttp stub - multiple inputs" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint
      .in("api" / path[String]("id") and query[Int]("amount"))
      .post
      .out(jsonBody[ResponseWrapper])

    implicit val backend = SttpBackendStub
      .apply(idMonad)
      .whenRequestMatchesEndpoint(endpoint)
      .thenSuccess(ResponseWrapper(1.0))
    val response: Identity[Response[Either[Unit, ResponseWrapper]]] =
      endpoint.toSttpRequestUnsafe(uri"http://test.com").apply("id1" -> 11).send()

    response.body shouldBe Right(ResponseWrapper(1.0))
  }

  it should "combine tapir endpoint with sttp stub - header output" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint.post
      .out(header[String]("X"))

    implicit val backend = SttpBackendStub
      .apply(idMonad)
      .whenRequestMatchesEndpoint(endpoint)
      .thenSuccess("x")
    val response: Identity[Response[Either[Unit, String]]] =
      endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(()).send()

    response.body shouldBe Right("x")
    response.header("X") shouldBe Some("x")
  }

  it should "match with inputs" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint
      .in("api" / "sometest4")
      .in(query[Int]("amount"))
      .post
      .out(jsonBody[ResponseWrapper])

    implicit val backend: SttpBackendStub[Identity, Nothing, NothingT] = SttpBackendStub
      .apply(idMonad)
      .whenInputMatches(endpoint) { amount => amount > 0 }
      .thenSuccess(ResponseWrapper(1.0))
      .whenInputMatches(endpoint) { amount => amount <= 0 }
      .generic
      .thenRespondServerError()

    val response1: Identity[Response[Either[Unit, ResponseWrapper]]] = endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(11).send()
    val response2: Identity[Response[Either[Unit, ResponseWrapper]]] = endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(-1).send()

    response1.body shouldBe Right(ResponseWrapper(1.0))
    response2.body shouldBe Left(())
    response2.code shouldBe StatusCode.InternalServerError
    response2.statusText shouldBe "Internal server error"
  }

  it should "match with decode failure" in {
    import sttp.tapir.client.sttp._
    // given
    val endpoint = sttp.tapir.endpoint
      .in("api" / "sometest4")
      .in(
        query[Int]("amount")
          .validate(Validator.min(0))
      )
      .post
      .out(jsonBody[ResponseWrapper])

    implicit val backend: SttpBackendStub[Identity, Nothing, NothingT] = SttpBackendStub
      .apply(idMonad)
      .whenDecodingInputFailure(endpoint)
      .generic
      .thenRespondWithCode(StatusCode.BadRequest)
    val response: Identity[Response[Either[Unit, ResponseWrapper]]] = endpoint.toSttpRequestUnsafe(uri"http://test.com").apply(-1).send()

    response shouldBe Response(Left(()), StatusCode.BadRequest)
  }
}

final case class ResponseWrapper(response: Double)
