package com.jackinatox.android.composestarter.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "DashboardRepo"

class DashboardRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchData(config: WatchConfig): Result<DashboardData> =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.i(TAG, "► FETCH  ${config.url}")
            val startMs = System.currentTimeMillis()

            try {
                val request = Request.Builder()
                    .url(config.url)
                    .addHeader("X-API-Key", config.apiKey)
                    .build()

                val body = client.newCall(request).execute().use { response ->
                    val elapsed = System.currentTimeMillis() - startMs
                    Log.i(TAG, "◄ HTTP ${response.code}  in ${elapsed}ms")
                    if (!response.isSuccessful) {
                        Log.e(TAG, "✗ Request failed: HTTP ${response.code} ${response.message}")
                        return@withContext Result.failure(
                            Exception("HTTP ${response.code}: ${response.message}")
                        )
                    }
                    response.body?.string() ?: run {
                        Log.e(TAG, "✗ Empty response body")
                        return@withContext Result.failure(Exception("Empty response body"))
                    }
                }

                val latencyMs = System.currentTimeMillis() - startMs
                val root = json.parseToJsonElement(body).jsonObject

                // ── top-level ──────────────────────────────────────────────
                val status         = root["status"]?.jsonPrimitive?.content ?: "unknown"
                val apiResponseTime = root["responseTime"]?.jsonPrimitive?.content ?: ""

                // ── database ───────────────────────────────────────────────
                val db        = root["database"]?.jsonObject
                val dbStats   = db?.get("stats")?.jsonObject
                val gs        = dbStats?.get("gameServers")?.jsonObject
                val orders    = dbStats?.get("orders")?.jsonObject
                val tickets   = dbStats?.get("supportTickets")?.jsonObject

                val dbConnected        = db?.get("connected")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val dbLatency          = db?.get("latency")?.jsonPrimitive?.content ?: ""
                val usersTotal         = dbStats?.get("users")?.jsonObject?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val gameServersTotal   = gs?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val gameServersActive  = gs?.get("active")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val gameServersExpired = gs?.get("expired")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ordersTotal        = orders?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ordersPending      = orders?.get("pending")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ordersPaid         = orders?.get("paid")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val gameDataTotal      = dbStats?.get("gameData")?.jsonObject?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val locationsTotal     = dbStats?.get("locations")?.jsonObject?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ticketsTotal       = tickets?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val ticketsOpen        = tickets?.get("open")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                // ── system ─────────────────────────────────────────────────
                val sys     = root["system"]?.jsonObject
                val process = sys?.get("process")?.jsonObject
                val mem     = process?.get("memory")?.jsonObject
                val machine = sys?.get("system")?.jsonObject
                val env     = sys?.get("environment")?.jsonObject

                val processUptime = process?.get("uptime")?.jsonObject?.get("formatted")?.jsonPrimitive?.content ?: ""
                val heapUsed      = mem?.get("heapUsed")?.jsonPrimitive?.content ?: ""
                val rss           = mem?.get("rss")?.jsonPrimitive?.content ?: ""
                val nodeVersion   = process?.get("nodeVersion")?.jsonPrimitive?.content ?: ""
                val hostname      = machine?.get("hostname")?.jsonPrimitive?.content ?: ""
                val cpuCount      = machine?.get("cpus")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val freeMemory    = machine?.get("freeMemory")?.jsonPrimitive?.content ?: ""
                val totalMemory   = machine?.get("totalMemory")?.jsonPrimitive?.content ?: ""
                val systemUptime  = machine?.get("uptime")?.jsonObject?.get("formatted")?.jsonPrimitive?.content ?: ""
                val loadAverage   = machine?.get("loadAverage")?.jsonArray
                    ?.map { it.jsonPrimitive.content.toDoubleOrNull() ?: 0.0 } ?: emptyList()
                val nodeEnv       = env?.get("nodeEnv")?.jsonPrimitive?.content ?: ""

                // ── log summary ────────────────────────────────────────────
                Log.i(TAG, "")
                Log.i(TAG, "┌─── PARSED ────────────────────────────")
                Log.i(TAG, "│  status        →  $status")
                Log.i(TAG, "│  db            →  connected=$dbConnected  latency=$dbLatency")
                Log.i(TAG, "│  users         →  $usersTotal")
                Log.i(TAG, "│  gameServers   →  total=$gameServersTotal  active=$gameServersActive  expired=$gameServersExpired")
                Log.i(TAG, "│  orders        →  total=$ordersTotal  pending=$ordersPending  paid=$ordersPaid")
                Log.i(TAG, "│  tickets       →  total=$ticketsTotal  open=$ticketsOpen")
                Log.i(TAG, "│  systemUptime  →  $systemUptime")
                Log.i(TAG, "│  processUptime →  $processUptime")
                Log.i(TAG, "│  memory        →  rss=$rss  heap=$heapUsed")
                Log.i(TAG, "│  load          →  ${loadAverage.joinToString("  ")}")
                Log.i(TAG, "└───────────────────────────────────────")
                Log.i(TAG, "✓ DONE  ${latencyMs}ms")
                Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.i(TAG, "")

                Result.success(
                    DashboardData(
                        status = status,
                        apiResponseTime = apiResponseTime,
                        dbConnected = dbConnected,
                        dbLatency = dbLatency,
                        usersTotal = usersTotal,
                        gameServersTotal = gameServersTotal,
                        gameServersActive = gameServersActive,
                        gameServersExpired = gameServersExpired,
                        ordersTotal = ordersTotal,
                        ordersPending = ordersPending,
                        ordersPaid = ordersPaid,
                        gameDataTotal = gameDataTotal,
                        locationsTotal = locationsTotal,
                        ticketsTotal = ticketsTotal,
                        ticketsOpen = ticketsOpen,
                        processUptime = processUptime,
                        heapUsed = heapUsed,
                        rss = rss,
                        nodeVersion = nodeVersion,
                        hostname = hostname,
                        cpuCount = cpuCount,
                        freeMemory = freeMemory,
                        totalMemory = totalMemory,
                        loadAverage = loadAverage,
                        systemUptime = systemUptime,
                        nodeEnv = nodeEnv,
                        fetchedAt = System.currentTimeMillis(),
                        latencyMs = latencyMs,
                    )
                )
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startMs
                Log.e(TAG, "✗ FETCH FAILED after ${elapsed}ms", e)
                Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.i(TAG, "")
                Result.failure(e)
            }
        }
}
