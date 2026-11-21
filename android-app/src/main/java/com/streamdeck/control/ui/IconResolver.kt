package com.streamdeck.control.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconResolver {
    private val iconMap = linkedMapOf(
        "volume_up" to Icons.AutoMirrored.Filled.VolumeUp,
        "volume_down" to Icons.AutoMirrored.Filled.VolumeDown,
        "volume_off" to Icons.AutoMirrored.Filled.VolumeOff,
        "play" to Icons.Filled.PlayArrow,
        "pause" to Icons.Filled.Pause,
        "stop" to Icons.Filled.Stop,
        "skip_previous" to Icons.Filled.SkipPrevious,
        "skip_next" to Icons.Filled.SkipNext,
        "brightness_high" to Icons.Filled.BrightnessHigh,
        "brightness_low" to Icons.Filled.BrightnessLow,
        "wifi" to Icons.Filled.Wifi,
        "bluetooth" to Icons.Filled.Bluetooth,
        "monitor" to Icons.Filled.Monitor,
        "fullscreen" to Icons.Filled.Fullscreen,
        "screenshot" to Icons.Filled.CameraAlt,
        "settings" to Icons.Filled.Settings,
        "language" to Icons.Filled.Language,
        "folder" to Icons.Filled.Folder,
        "content_copy" to Icons.Filled.ContentCopy,
        "content_paste" to Icons.Filled.ContentPaste,
        "content_cut" to Icons.Filled.ContentCut,
        "search" to Icons.Filled.Search,
        "lock" to Icons.Filled.Lock,
        "power_settings_new" to Icons.Filled.PowerSettingsNew,
        "restart" to Icons.Filled.RestartAlt,
        "keyboard" to Icons.Filled.Keyboard,
        "mouse" to Icons.Filled.Mouse,
        "mic" to Icons.Filled.Mic,
        "camera" to Icons.Filled.Camera,
        "record" to Icons.Filled.FiberManualRecord,
        "timer" to Icons.Filled.Timer,
        "star" to Icons.Filled.Star,
        "favorite" to Icons.Filled.Favorite,
        "dashboard" to Icons.Filled.Dashboard,
        "add" to Icons.Filled.Add,
        "remove" to Icons.Filled.Remove,
        "check" to Icons.Filled.Check,
        "close" to Icons.Filled.Close,
        "refresh" to Icons.Filled.Refresh,
        "menu" to Icons.Filled.Menu,
        "edit" to Icons.Filled.Edit,
        "delete" to Icons.Filled.Delete,
        "link" to Icons.Filled.Link,
        "home" to Icons.Filled.Home,
        "more" to Icons.Filled.MoreVert,
        "power" to Icons.Filled.PowerSettingsNew,
        "music" to Icons.Filled.MusicNote,
        "video" to Icons.Filled.Videocam,
        "speaker" to Icons.Filled.Speaker,
        "headphones" to Icons.Filled.Headphones,
        "laptop" to Icons.Filled.Laptop,
        "computer" to Icons.Filled.Computer,
        "terminal" to Icons.Filled.Code,
        "download" to Icons.Filled.Download,
        "upload" to Icons.Filled.Upload,
        "file" to Icons.AutoMirrored.Filled.InsertDriveFile,
        "image" to Icons.Filled.Image,
        "brush" to Icons.Filled.Brush,
        "crop" to Icons.Filled.Crop,
        "layers" to Icons.Filled.Layers,
        "filter" to Icons.Filled.FilterAlt,
        "text" to Icons.Filled.TextFields,
        "draw" to Icons.Filled.Draw,
        "live" to Icons.Filled.LiveTv,
        "cast" to Icons.Filled.Cast,
        "screen_share" to Icons.AutoMirrored.Filled.ScreenShare,
        "phone" to Icons.Filled.Phone,
        "mail" to Icons.Filled.Mail,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "notifications" to Icons.Filled.Notifications,
        "share" to Icons.Filled.Share,
        "unlock" to Icons.Filled.LockOpen,
        "security" to Icons.Filled.Security,
        "usb" to Icons.Filled.Usb,
        "battery" to Icons.Filled.BatteryFull,
        "cloud" to Icons.Filled.Cloud,
    )

    private val categoriesByIcon = mapOf(
        "General" to listOf("home", "search", "settings", "edit", "delete", "add", "remove", "check", "close", "refresh", "more", "menu", "power", "power_settings_new", "dashboard", "star", "favorite", "content_copy", "content_paste", "content_cut"),
        "Media" to listOf("play", "pause", "stop", "skip_previous", "skip_next", "music", "video", "mic", "volume_up", "volume_down", "volume_off", "speaker", "headphones", "record"),
        "Computer" to listOf("monitor", "laptop", "computer", "keyboard", "mouse", "download", "upload", "folder", "file", "language"),
        "Creative" to listOf("camera", "image", "brush", "crop", "layers", "filter", "text", "draw"),
        "Streaming" to listOf("live", "cast", "screen_share", "fullscreen", "screenshot"),
        "Communication" to listOf("phone", "mail", "chat", "notifications", "share", "link"),
        "System" to listOf("lock", "unlock", "security", "bluetooth", "wifi", "usb", "battery", "cloud", "restart", "timer", "brightness_high", "brightness_low"),
    )

    data class IconEntry(val name: String, val category: String, val icon: ImageVector) {
        val label: String
            get() = name.split('_').joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    val categories: List<String> = listOf("All") + categoriesByIcon.keys.toList()
    val registry: List<IconEntry> = categoriesByIcon.flatMap { (category, names) ->
        names.mapNotNull { name -> iconMap[name]?.let { IconEntry(name, category, it) } }
    }

    fun resolve(actionType: String, actionValue: String, title: String, subtitle: String = ""): String {
        val text = listOf(actionType, actionValue, title, subtitle).joinToString(" ").lowercase()
        val rules = listOf(
            listOf("volume up", "vol_up", "volume_up", "volume+") to "volume_up",
            listOf("volume down", "vol_down", "volume_down", "volume-") to "volume_down",
            listOf("mute", "volume off", "volume_off") to "volume_off",
            listOf("play pause", "play_pause") to "play",
            listOf("play") to "play",
            listOf("pause") to "pause",
            listOf("stop") to "stop",
            listOf("previous", "skip_previous") to "skip_previous",
            listOf("next", "skip_next") to "skip_next",
            listOf("brightness up", "brightness_high") to "brightness_high",
            listOf("brightness down", "brightness_low") to "brightness_low",
            listOf("wifi", "wi-fi", "wireless") to "wifi",
            listOf("bluetooth") to "bluetooth",
            listOf("monitor", "display", "screen") to "monitor",
            listOf("fullscreen", "full screen") to "fullscreen",
            listOf("screenshot", "screen shot", "capture") to "screenshot",
            listOf("setting", "preferences", "options") to "settings",
            listOf("browser", "website", "web", "url", "open_url") to "language",
            listOf("folder", "directory", "explorer") to "folder",
            listOf("copy") to "content_copy",
            listOf("paste") to "content_paste",
            listOf("cut") to "content_cut",
            listOf("search", "find") to "search",
            listOf("lock", "lockscreen") to "lock",
            listOf("shutdown", "power off", "power_settings") to "power_settings_new",
            listOf("restart", "reboot") to "restart",
            listOf("keyboard", "shortcut") to "keyboard",
            listOf("mouse", "click") to "mouse",
            listOf("microphone", "mic") to "mic",
            listOf("camera", "webcam") to "camera",
            listOf("record") to "record",
            listOf("timer", "stopwatch") to "timer",
            listOf("star", "favorite") to "favorite",
        )
        for ((keywords, icon) in rules) {
            if (keywords.any { text.contains(it) }) return icon
        }
        return "dashboard"
    }

    fun icon(name: String?): ImageVector =
        iconMap[name] ?: Icons.Filled.Dashboard

    val availableNames: List<String> = iconMap.keys.toList()
}
