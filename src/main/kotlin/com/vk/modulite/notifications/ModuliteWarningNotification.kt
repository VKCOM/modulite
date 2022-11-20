package com.vk.modulite.notifications

import com.intellij.notification.NotificationType

class ModuliteWarningNotification(content: String = "", important: Boolean = false) :
    ModuliteNotification(content, NotificationType.WARNING, important)
