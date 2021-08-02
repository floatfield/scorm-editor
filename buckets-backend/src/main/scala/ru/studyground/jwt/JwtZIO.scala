package ru
package studyground
package jwt

import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtCore, JwtHeader}
import zio.json._

case class Header(
    alg: Option[String],
    typ: Option[String],
    cty: Option[String],
    kid: Option[String]
) {
  def toJwtHeader: JwtHeader =
    JwtHeader(alg.map(JwtAlgorithm.fromString), typ, cty, kid)
}

case class Claim(
    iss: Option[String],
    sub: Option[String],
    aud: Option[Set[String]],
    exp: Option[Long],
    nbf: Option[Long],
    iat: Option[Long],
    jti: Option[String]
) {
  def toJwtClaim: JwtClaim =
    JwtClaim(
      content = "{}",
      iss,
      sub,
      aud,
      exp,
      nbf,
      iat,
      jti
    )
}

object JwtZIO extends JwtCore[JwtHeader, JwtClaim] {
  private implicit val headerDecoder: JsonDecoder[JwtHeader] =
    DeriveJsonDecoder.gen[Header].map(_.toJwtHeader)
  private implicit val claimDecoder: JsonDecoder[JwtClaim] =
    DeriveJsonDecoder.gen[Claim].map(_.toJwtClaim)

  override protected def parseHeader(header: String): JwtHeader =
    header.fromJson[JwtHeader].getOrElse(JwtHeader(None))

  override protected def parseClaim(claim: String): JwtClaim =
    claim.fromJson[JwtClaim].map(_.withContent(claim)).getOrElse(JwtClaim())

  override protected def extractAlgorithm(
      header: JwtHeader
  ): Option[JwtAlgorithm] =
    header.algorithm

  override protected def extractExpiration(claim: JwtClaim): Option[Long] =
    claim.expiration

  override protected def extractNotBefore(claim: JwtClaim): Option[Long] =
    claim.notBefore
}
