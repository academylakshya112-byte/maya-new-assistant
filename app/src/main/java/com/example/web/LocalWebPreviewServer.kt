package com.example.web

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

object LocalWebPreviewServer {
    private const val TAG = "LocalWebPreviewServer"
    const val DEFAULT_PORT = 8765
    
    @Volatile
    var actualPort: Int = DEFAULT_PORT
        private set

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
                // Try ports starting from DEFAULT_PORT
                var bound = false
                for (p in DEFAULT_PORT..8780) {
                    try {
                        val ss = ServerSocket(p, 50, InetAddress.getByName("127.0.0.1"))
                        ss.reuseAddress = true
                        serverSocket = ss
                        actualPort = p
                        bound = true
                        Log.i(TAG, "Local web preview server successfully bound on 127.0.0.1:$actualPort")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Port $p in use, trying next...")
                    }
                }

                if (!bound) {
                    val ss = ServerSocket(0)
                    serverSocket = ss
                    actualPort = ss.localPort
                    Log.i(TAG, "Local web preview server bound on ephemeral port $actualPort")
                }

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
            socket.soTimeout = 3000
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val firstLine = reader.readLine() ?: return
                // Drain headers
                var line: String? = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    line = reader.readLine()
                }

                val html = getHtmlProvider()
                val bytes = html.toByteArray(StandardCharsets.UTF_8)
                val out: OutputStream = s.getOutputStream()
                val responseHeader = ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n" +
                        "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n")
                out.write(responseHeader.toByteArray(StandardCharsets.UTF_8))
                out.write(bytes)
                out.flush()
            }
        } catch (e: Exception) {
            Log.v(TAG, "Client socket handled: ${e.message}")
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
