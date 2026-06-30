package com.focusguard.app.domain.model

data class SubTask(
    val id: String,
    val title: String,
    val done: Boolean = false,
)

data class Task(
    val taskId: String,
    val userId: String,
    val title: String,
    val deadline: String,
    val effortHours: Float,
    val category: String,
    val priorityScore: Int,
    val status: TaskStatus,
    val priorityRankReason: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val subTasks: List<SubTask> = emptyList(),
    val checkinStreak: Int = 0,
)

enum class TaskStatus { pending, in_progress, completed }

// Priority colour bucket used by UI
fun Task.priorityLevel(): PriorityLevel = when {
    priorityScore >= 80 -> PriorityLevel.HIGH
    priorityScore >= 50 -> PriorityLevel.MEDIUM
    else                -> PriorityLevel.LOW
}

enum class PriorityLevel { HIGH, MEDIUM, LOW }
