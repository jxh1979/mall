package com.complex.terminal

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
  private lateinit var statusView: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    statusView = TextView(this).apply {
      text = "终端待连接"
      textSize = 18f
      gravity = Gravity.CENTER
      setPadding(24, 24, 24, 24)
    }

    val checkButton = Button(this).apply {
      text = "检查 160 API"
      setOnClickListener { checkBackendHealth() }
    }

    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      setPadding(48, 48, 48, 48)
      addView(statusView)
      addView(checkButton)
    }

    setContentView(root)
    checkBackendHealth()
  }

  private fun checkBackendHealth() {
    statusView.text = "正在连接 160 API..."
    thread {
      val result = runCatching {
        val endpoint = buildHealthEndpoint()
        val connection = endpoint.openConnection() as HttpURLConnection // nosemgrep: URL is built from BuildConfig and validated by validateApiUrl before use.
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        if (body.contains("\"ok\":true")) "160 API 在线" else "160 API 返回异常"
      }.getOrElse { error ->
        "160 API 连接失败：${error.message ?: "unknown"}"
      }

      runOnUiThread {
        statusView.text = result
      }
    }
  }

  private fun buildHealthEndpoint(): URL {
    val baseUrl = BuildConfig.COMPLEX_API_BASE_URL.trim().trimEnd('/')
    val endpoint = URL("$baseUrl/api/v1/system/health")
    validateApiUrl(endpoint)
    return endpoint
  }

  private fun validateApiUrl(url: URL) {
    val protocol = url.protocol.lowercase(Locale.ROOT)
    val host = url.host.lowercase(Locale.ROOT)
    val allowedHosts = BuildConfig.COMPLEX_API_ALLOWED_HOSTS
      .split(',')
      .map { it.trim().lowercase(Locale.ROOT) }
      .filter { it.isNotEmpty() }
      .toSet()

    require(protocol == "https" || isLoopbackHost(host)) {
      "COMPLEX_API_BASE_URL must use https unless it targets a local smoke endpoint"
    }
    require(host in allowedHosts) {
      "COMPLEX_API_BASE_URL host is not in COMPLEX_ANDROID_API_ALLOWED_HOSTS"
    }
  }

  private fun isLoopbackHost(host: String): Boolean {
    return host == "localhost" || host == "127.0.0.1" || host == "::1"
  }
}
