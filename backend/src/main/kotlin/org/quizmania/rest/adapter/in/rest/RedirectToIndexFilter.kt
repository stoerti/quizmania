package org.quizmania.rest.adapter.`in`.rest

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import mu.KLogging
import org.springframework.stereotype.Component

/**
 * Redirect all deep links used in react app to index page to no get 404 on reload
 */
@Component
class RedirectToIndexFilter : Filter {

  companion object : KLogging()

  override fun doFilter(
    request: ServletRequest,
    response: ServletResponse?,
    chain: FilterChain
  ) {
    val req = request as HttpServletRequest
    val requestURI = req.requestURI

    if (requestURI.startsWith("/game") || requestURI.startsWith("/login") || requestURI.startsWith("/logout")) {
      // all requests not api or static will be forwarded to index page.
      logger.debug { "Forwarding $requestURI to index page" }
      request.getRequestDispatcher("/").forward(request, response)
      return
    }

    chain.doFilter(request, response)
  }
}
