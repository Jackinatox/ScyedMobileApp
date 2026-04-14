package com.jackinatox.android.composestarter.data

data class DashboardData(
    // API health
    val status: String,
    val apiResponseTime: String,
    // Database
    val dbConnected: Boolean,
    val dbLatency: String,
    // Game stats
    val usersTotal: Int,
    val gameServersTotal: Int,
    val gameServersActive: Int,
    val gameServersExpired: Int,
    // Orders
    val ordersTotal: Int,
    val ordersPending: Int,
    val ordersPaid: Int,
    // Other DB counts
    val gameDataTotal: Int,
    val locationsTotal: Int,
    val ticketsTotal: Int,
    val ticketsOpen: Int,
    // Node.js process
    val processUptime: String,
    val heapUsed: String,
    val rss: String,
    val nodeVersion: String,
    // Machine
    val hostname: String,
    val cpuCount: Int,
    val freeMemory: String,
    val totalMemory: String,
    val loadAverage: List<Double>,
    val systemUptime: String,
    val nodeEnv: String,
    // Fetch metadata
    val fetchedAt: Long,
    val latencyMs: Long,
)
