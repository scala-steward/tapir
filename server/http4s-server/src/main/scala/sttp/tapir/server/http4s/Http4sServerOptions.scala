package sttp.tapir.server.http4s

import cats.Applicative
import cats.effect.Sync
import cats.implicits.catsSyntaxOptionId
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.Interceptor
import sttp.tapir.server.interceptor.content.UnsupportedMediaTypeInterceptor
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DecodeFailureInterceptor, DefaultDecodeFailureHandler}
import sttp.tapir.server.interceptor.exception.{DefaultExceptionHandler, ExceptionHandler, ExceptionInterceptor}
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog, ServerLogInterceptor}
import sttp.tapir.server.interceptor.metrics.MetricsRequestInterceptor
import sttp.tapir.server.interceptor.reject.RejectInterceptor
import sttp.tapir.{Defaults, TapirFile}

import java.io.File

/** @tparam F The effect type used for response body streams. Usually the same as `G`.
  * @tparam G The effect type used for representing arbitrary side-effects, such as creating files or logging.
  *           Usually the same as `F`.
  */
case class Http4sServerOptions[F[_], G[_]](
    createFile: ServerRequest => G[TapirFile],
    deleteFile: TapirFile => G[Unit],
    ioChunkSize: Int,
    interceptors: List[Interceptor[G, Http4sResponseBody[F]]]
) {
  def prependInterceptor(i: Interceptor[G, Http4sResponseBody[F]]): Http4sServerOptions[F, G] =
    copy(interceptors = i :: interceptors)
  def appendInterceptor(i: Interceptor[G, Http4sResponseBody[F]]): Http4sServerOptions[F, G] =
    copy(interceptors = interceptors :+ i)
}

object Http4sServerOptions {

  /** Creates default [[Http4sServerOptions]] with `additionalInterceptors`, sitting between two configurable
    * interceptor groups.
    *
    * The options can be then further customised using copy constructors or the methods to append/prepend
    * interceptors.
    *
    * @param exceptionHandler Whether to respond to exceptions, or propagate them to http4s.
    * @param rejectInterceptor How to respond when decoding fails for all interpreted endpoints.
    * @param serverLog The server log using which an interceptor will be created, if any. To keep the default, use
    *                  `Http4sServerOptions.Log.defaultServerLog`
    * @param additionalInterceptors Additional interceptors, e.g. handling decode failures, or providing alternate
    *                               responses.
    * @param unsupportedMediaTypeInterceptor Whether to return 415 (unsupported media type) if there's no body in the
    *                                        endpoint's outputs, which can satisfy the constraints from the `Accept`
    *                                        header
    * @param decodeFailureHandler The decode failure handler, from which an interceptor will be created.
    */
  def customInterceptors[F[_], G[_]: Sync](
      rejectInterceptor: Option[RejectInterceptor[G, Http4sResponseBody[F]]],
      exceptionHandler: Option[ExceptionHandler],
      serverLog: Option[ServerLog[G[Unit]]],
      metricsInterceptor: Option[MetricsRequestInterceptor[G, Http4sResponseBody[F]]] = None,
      additionalInterceptors: List[Interceptor[G, Http4sResponseBody[F]]] = Nil,
      unsupportedMediaTypeInterceptor: Option[UnsupportedMediaTypeInterceptor[G, Http4sResponseBody[F]]] =
        new UnsupportedMediaTypeInterceptor[G, Http4sResponseBody[F]]().some,
      decodeFailureHandler: DecodeFailureHandler = DefaultDecodeFailureHandler.handler
  ): Http4sServerOptions[F, G] =
    Http4sServerOptions(
      defaultCreateFile[G],
      defaultDeleteFile[G],
      8192,
      metricsInterceptor.toList ++
        rejectInterceptor.toList ++
        exceptionHandler.map(new ExceptionInterceptor[G, Http4sResponseBody[F]](_)).toList ++
        serverLog.map(Log.serverLogInterceptor[F, G]).toList ++
        additionalInterceptors ++
        unsupportedMediaTypeInterceptor.toList ++
        List(new DecodeFailureInterceptor[G, Http4sResponseBody[F]](decodeFailureHandler))
    )

  def defaultCreateFile[F[_]](implicit sync: Sync[F]): ServerRequest => F[File] = _ => sync.blocking(Defaults.createTempFile())

  def defaultDeleteFile[F[_]](implicit sync: Sync[F]): TapirFile => F[Unit] = file => sync.blocking(Defaults.deleteFile()(file))

  object Log {
    def defaultServerLog[F[_]: Sync]: DefaultServerLog[F[Unit]] =
      DefaultServerLog[F[Unit]](
        doLogWhenHandled = debugLog[F],
        doLogAllDecodeFailures = debugLog[F],
        doLogExceptions = (msg: String, ex: Throwable) => Sync[F].delay(Http4sServerToHttpInterpreter.log.error(ex)(msg)),
        noLog = Applicative[F].unit
      )

    def serverLogInterceptor[F[_], G[_]](serverLog: ServerLog[G[Unit]]): ServerLogInterceptor[G[Unit], G, Http4sResponseBody[F]] =
      new ServerLogInterceptor[G[Unit], G, Http4sResponseBody[F]](serverLog, (f, _) => f)

    private def debugLog[F[_]: Sync](msg: String, exOpt: Option[Throwable]): F[Unit] =
      exOpt match {
        case None     => Sync[F].delay(Http4sServerToHttpInterpreter.log.debug(msg))
        case Some(ex) => Sync[F].delay(Http4sServerToHttpInterpreter.log.debug(ex)(msg))
      }
  }

  def default[F[_], G[_]: Sync]: Http4sServerOptions[F, G] =
    customInterceptors(Some(RejectInterceptor.default), Some(DefaultExceptionHandler), Some(Log.defaultServerLog[G]))
}
