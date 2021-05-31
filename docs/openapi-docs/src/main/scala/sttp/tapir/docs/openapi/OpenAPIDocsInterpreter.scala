package sttp.tapir.docs.openapi

import sttp.tapir.{Endpoint, DocsExtension}
import sttp.tapir.openapi.{Info, OpenAPI}
import sttp.tapir.server.ServerEndpoint

trait OpenAPIDocsInterpreter {
  def toOpenAPI[I, E, O, S](e: Endpoint[I, E, O, S], title: String, version: String)(implicit options: OpenAPIDocsOptions): OpenAPI =
    toOpenAPI(e, Info(title, version))

  def toOpenAPI[I, E, O, S](e: Endpoint[I, E, O, S], info: Info)(implicit options: OpenAPIDocsOptions): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, Seq(e), options, List.empty)

  def toOpenAPI[I, E, O, S](e: Endpoint[I, E, O, S], info: Info, docsExtensions: List[DocsExtension[_]])(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, Seq(e), options, docsExtensions)

  def toOpenAPI[I, E, O, S, F[_]](se: ServerEndpoint[I, E, O, S, F], title: String, version: String)(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI =
    toOpenAPI(se.endpoint, Info(title, version))

  def toOpenAPI[I, E, O, S, F[_]](se: ServerEndpoint[I, E, O, S, F], info: Info)(implicit options: OpenAPIDocsOptions): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, Seq(se.endpoint), options, List.empty)

  def toOpenAPI[I, E, O, S, F[_]](se: ServerEndpoint[I, E, O, S, F], info: Info, docsExtensions: List[DocsExtension[_]])(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, Seq(se.endpoint), options, docsExtensions)

  def toOpenAPI(es: Iterable[Endpoint[_, _, _, _]], title: String, version: String)(implicit options: OpenAPIDocsOptions): OpenAPI =
    toOpenAPI(es, Info(title, version))

  def toOpenAPI(es: Iterable[Endpoint[_, _, _, _]], info: Info)(implicit options: OpenAPIDocsOptions): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, es, options, List.empty)

  def toOpenAPI(es: Iterable[Endpoint[_, _, _, _]], info: Info, docsExtensions: List[DocsExtension[_]])(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, es, options, docsExtensions)

  def serverEndpointsToOpenAPI[F[_]](ses: Iterable[ServerEndpoint[_, _, _, _, F]], title: String, version: String)(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI = serverEndpointsToOpenAPI(ses, Info(title, version))

  def serverEndpointsToOpenAPI[F[_]](ses: Iterable[ServerEndpoint[_, _, _, _, F]], info: Info)(implicit
      options: OpenAPIDocsOptions
  ): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, ses.map(_.endpoint), options, List.empty)

  def serverEndpointsToOpenAPI[F[_]](ses: Iterable[ServerEndpoint[_, _, _, _, F]], info: Info, docsExtensions: List[DocsExtension[_]])(
      implicit options: OpenAPIDocsOptions
  ): OpenAPI =
    EndpointToOpenAPIDocs.toOpenAPI(info, ses.map(_.endpoint), options, docsExtensions)
}

object OpenAPIDocsInterpreter extends OpenAPIDocsInterpreter
