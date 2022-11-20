package com.vk.modulite.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.notification.impl.NotificationFullContent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.util.function.BiConsumer

open class ModuliteNotification(
    content: String = "",
    type: NotificationType = NotificationType.INFORMATION,
    important: Boolean = false,
) : Notification(
    if (important) importantID else ID,
    "Modulite",
    content,
    type,
), NotificationFullContent {

    companion object {
        const val ID = "Modulite"
        const val importantID = "Modulite Important"
        private val LOG = logger<ModuliteNotification>()
    }

    fun withActions(vararg actions: NotificationAction): ModuliteNotification {
        actions.forEach {
            addAction(it)
        }

        return this
    }

    fun withTitle(title: String): ModuliteNotification {
        setTitle(title)
        return this
    }

    fun show() {
        ApplicationManager.getApplication().invokeLater {
            Notifications.Bus.notify(this)
            LOG.info("Notification: title: $title, content: ${content.ifEmpty { "<empty>" }}, type: $type")
        }
    }

    class Action(msg: String, private val runnable: BiConsumer<AnActionEvent, Notification>) : NotificationAction(msg) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            runnable.accept(e, notification)
        }
    }
}
