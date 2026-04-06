package com.eilaji.backend.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.typesafe.config.ConfigFactory

object JwtConfig {
    private val config = ConfigFactory.load()
    
    private val secret: String = config.getString("jwt.secret")
    private val issuer: String = config.getString("jwt.issuer")
    private val audience: String = config.getString("jwt.audience")
    val expiresIn: Long = config.getLong("jwt.expiresIn")
    val refreshExpiresIn: Long = config.getLong("jwt.refreshExpiresIn")
    
    private val algorithm = Algorithm.HMAC256(secret)
    
    fun getVerifier(): JWTVerifier {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }
    
    fun getAlgorithm(): Algorithm {
        return algorithm
    }
    
    fun getIssuer(): String {
        return issuer
    }
    
    fun getAudience(): String {
        return audience
    }
}
