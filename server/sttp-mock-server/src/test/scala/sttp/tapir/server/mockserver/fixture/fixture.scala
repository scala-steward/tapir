package sttp.tapir.server.mockserver.fixture

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import java.util.UUID

case class CreatePersonCommand(name: String, age: Int)

object CreatePersonCommand {
  implicit val codec: Codec.AsObject[CreatePersonCommand] = deriveCodec[CreatePersonCommand]
}

case class PersonView(id: UUID, name: String, age: Int)

object PersonView {
  implicit val codec: Codec.AsObject[PersonView] = deriveCodec[PersonView]
}

case class ApiError(code: Int, message: String)

object ApiError {
  implicit val codec: Codec.AsObject[ApiError] = deriveCodec[ApiError]
}
