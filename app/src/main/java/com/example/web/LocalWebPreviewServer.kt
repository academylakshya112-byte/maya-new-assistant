package com.example.web

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

object LocalWebPreviewServer {
    private const val TAG = "LocalWebPreviewServer"
    const val PORT = 8765
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false

    fun start(getHtmlProvider: () -> String) {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
            return
        }
        isRunning = true
        Thread({
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "Local web preview server started on port $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    Thread({
                        handleClient(client, getHtmlProvider)
                    }, "MayaWebClientThread").start()
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error in web preview server: ${e.message}")
                }
            }
        }, "MayaWebServerThread").start()
    }

    private fun handleClient(socket: Socket, getHtmlProvider: () -> String) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val firstLine = reader.readLine() ?: return
                // Drain remaining headers
                var line: String? = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    line = reader.readLine()
                }

                val html = getHtmlProvider()
                val bytes = html.toByteArray(StandardCharsets.UTF_8)
                val out: OutputStream = s.getOutputStream()
                val headers = ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n").toByteArray(StandardCharsets.UTF_8)
                out.write(headers)
                out.write(bytes)
                out.flush()
            }
        } catch (e: Exception) {
            Log.v(TAG, "Client handle exception (normal on browser close): ${e.message}")
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
    }
}
