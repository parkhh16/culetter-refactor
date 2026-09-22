package com.example.myapplication.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WearMessageRepository(private val ctx: Context) {
    
    companion object {
        private const val TAG = "WearMessageRepository"
    }

    suspend fun sendVoice(file: File): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "sendVoice called with file: ${file.absolutePath}, size: ${file.length()} bytes")
        
        try {
            // 연결된 노드 확인
            val connectedNodes = Tasks.await(Wearable.getNodeClient(ctx).connectedNodes)
            Log.d(TAG, "Connected nodes count: ${connectedNodes.size}")
            connectedNodes.forEach { node ->
                Log.d(TAG, "Connected node: ${node.id}, name: ${node.displayName}, isNearby: ${node.isNearby}")
            }
            
            if (connectedNodes.isEmpty()) {
                Log.e(TAG, "No connected nodes found - 워치와 폰이 연결되지 않았습니다")
                Log.e(TAG, "해결 방법: 1) 같은 Google 계정으로 로그인 2) Bluetooth/WiFi 켜기 3) 앱 재시작")
                return@withContext false
            }
            
            // 200KB 이하면 Message, 이상은 Channel
            return@withContext if (file.length() <= 200 * 1024) {
                Log.d(TAG, "Using Message API for small file")
                sendMessage(file.readBytes())
            } else {
                Log.d(TAG, "Using Channel API for large file")
                sendChannel(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendVoice", e)
            false
        }
    }

    suspend fun sendMessage(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "sendMessage called with ${bytes.size} bytes")
            val node = Tasks.await(Wearable.getNodeClient(ctx).connectedNodes).firstOrNull() ?: return@withContext false
            Log.d(TAG, "Sending message to node: ${node.id}")
            val result = Tasks.await(Wearable.getMessageClient(ctx).sendMessage(node.id, "/voice", bytes))
            Log.d(TAG, "Message sent successfully, result: $result")
            true
        }.getOrElse { 
            Log.e(TAG, "sendMessage failed", it)
            false 
        }
    }

    suspend fun sendChannel(file: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "sendChannel called with file: ${file.absolutePath}")
            val node = Tasks.await(Wearable.getNodeClient(ctx).connectedNodes).firstOrNull() ?: return@withContext false
            Log.d(TAG, "Opening channel to node: ${node.id}")
            val channel = Tasks.await(Wearable.getChannelClient(ctx).openChannel(node.id, "/voice"))
            Log.d(TAG, "Channel opened successfully")
            val out = Tasks.await(Wearable.getChannelClient(ctx).getOutputStream(channel))
            Log.d(TAG, "Writing file data to channel")
            file.inputStream().use { it.copyTo(out) }
            Log.d(TAG, "File data written, closing channel")
            Wearable.getChannelClient(ctx).close(channel)
            Log.d(TAG, "Channel closed successfully")
            true
        }.getOrElse { 
            Log.e(TAG, "sendChannel failed", it)
            false 
        }
    }

    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "ping called")
            val connectedNodes = Tasks.await(Wearable.getNodeClient(ctx).connectedNodes)
            Log.d(TAG, "Connected nodes for ping: ${connectedNodes.size}")
            val node = connectedNodes.firstOrNull() ?: return@withContext false
            Log.d(TAG, "Sending ping to node: ${node.id}")
            val result = Tasks.await(Wearable.getMessageClient(ctx).sendMessage(node.id, "/ping", "hello".toByteArray()))
            Log.d(TAG, "Ping sent successfully, result: $result")
            true
        }.getOrElse { 
            Log.e(TAG, "ping failed", it)
            false 
        }
    }
}
