package sttp.tapir.docs.openapi

import sttp.tapir.openapi.OpenAPI.ReferenceOr
import sttp.tapir.openapi.{ExampleValue, Parameter, ParameterIn, Schema}
import sttp.tapir.{EndpointIO, EndpointInput}

import scala.collection.immutable.ListMap

private[openapi] object EndpointInputToParameterConverter {
  def from[T](query: EndpointInput.Query[T], schema: ReferenceOr[Schema], example: Option[ExampleValue]): Parameter = {
    Parameter(
      query.name,
      ParameterIn.Query,
      query.info.description,
      Some(!query.codec.meta.schema.isOptional),
      if (query.info.deprecated) Some(true) else None,
      None,
      None,
      None,
      None,
      schema,
      example,
      ListMap.empty,
      ListMap.empty
    )
  }

  def from[T](pathCapture: EndpointInput.PathCapture[T], schema: ReferenceOr[Schema], example: Option[ExampleValue]): Parameter = {
    Parameter(
      pathCapture.name.getOrElse("?"),
      ParameterIn.Path,
      pathCapture.info.description,
      Some(true),
      None,
      None,
      None,
      None,
      None,
      schema,
      example,
      ListMap.empty,
      ListMap.empty
    )
  }

  def from[T](header: EndpointIO.Header[T], schema: ReferenceOr[Schema], example: Option[ExampleValue]): Parameter = {
    Parameter(
      header.name,
      ParameterIn.Header,
      header.info.description,
      Some(!header.codec.meta.schema.isOptional),
      if (header.info.deprecated) Some(true) else None,
      None,
      None,
      None,
      None,
      schema,
      example,
      ListMap.empty,
      ListMap.empty
    )
  }

  def from[T](header: EndpointIO.FixedHeader, schema: ReferenceOr[Schema], example: Option[ExampleValue]): Parameter = {
    Parameter(
      header.name,
      ParameterIn.Header,
      header.info.description,
      Some(true),
      if (header.info.deprecated) Some(true) else None,
      None,
      None,
      None,
      None,
      schema,
      example,
      ListMap.empty,
      ListMap.empty
    )
  }

  def from[T](cookie: EndpointInput.Cookie[T], schema: ReferenceOr[Schema], example: Option[ExampleValue]): Parameter = {
    Parameter(
      cookie.name,
      ParameterIn.Cookie,
      cookie.info.description,
      Some(!cookie.codec.meta.schema.isOptional),
      if (cookie.info.deprecated) Some(true) else None,
      None,
      None,
      None,
      None,
      schema,
      example,
      ListMap.empty,
      ListMap.empty
    )
  }
}
