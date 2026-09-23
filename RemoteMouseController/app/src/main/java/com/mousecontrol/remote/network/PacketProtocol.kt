package com.mousecontrol.remote.network

/**
 * Plain-text, newline-delimited wire protocol shared with the host receiver
 * script. Kept deliberately simple so any receiver (Python, or anything
 * else) can parse a line with one split() call instead of a binary codec.
 *
 *   MOVE:dx,dy      relative cursor move
 *   SCROLL:dy       vertical scroll
 *   CLICK:LEFT|RIGHT
 *   DCLICK:LEFT|RIGHT
 *   DOWN:LEFT|RIGHT mouse button down (drag-lock start)
 *   UP:LEFT|RIGHT   mouse button up (drag-lock end)
 *   VOL:0-100       set system volume
 *   BRI:0-100       set screen brightness
 *   HELLO:...       handshake sent right after a transport connects
 */
object PacketProtocol {
    fun move(dx: Float, dy: Float) = "MOVE:${"%.2f".format(dx)},${"%.2f".format(dy)}\n"
    fun scroll(dy: Float) = "SCROLL:${"%.2f".format(dy)}\n"
    fun click(button: MouseButton) = "CLICK:${button.wire}\n"
    fun doubleClick(button: MouseButton) = "DCLICK:${button.wire}\n"
    fun buttonDown(button: MouseButton) = "DOWN:${button.wire}\n"
    fun buttonUp(button: MouseButton) = "UP:${button.wire}\n"
    fun volume(level: Int) = "VOL:${level.coerceIn(0, 100)}\n"
    fun brightness(level: Int) = "BRI:${level.coerceIn(0, 100)}\n"
    fun hello() = "HELLO:remote-mouse-android\n"
}

enum class MouseButton(val wire: String) { LEFT("LEFT"), RIGHT("RIGHT") }
