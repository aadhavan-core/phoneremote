#!/usr/bin/env python3
"""
Host receiver for the Android Remote Mouse Controller app.

Listens on:
  - UDP (default port 5005) for the Wi-Fi transport
  - A Bluetooth serial (SPP) port, if one is supplied with --bt-port,
    for the Bluetooth transport (the app's default connection method)

...and translates the app's plain-text protocol into real OS mouse,
volume, and brightness changes.

Setup, per OS, for the Bluetooth path:
  Windows: Settings > Bluetooth & devices > Devices > More Bluetooth
           settings > COM Ports tab > Add > Incoming > pick your phone.
           Windows assigns a COM port (e.g. COM5) once you pair and
           connect from the app — pass that with --bt-port COM5.
  Linux:   sudo rfcomm listen /dev/rfcomm0 1
           then run this script with --bt-port /dev/rfcomm0
  macOS:   Classic (non-BLE) Bluetooth serial is deprecated on macOS;
           use the Wi-Fi transport instead.

Usage:
  python receiver.py --bt-port COM5
  python receiver.py                      # Wi-Fi (UDP) only
"""

import argparse
import platform
import socket
import threading

import pyautogui

pyautogui.FAILSAFE = False
pyautogui.PAUSE = 0

_lock = threading.Lock()


def set_volume(level: int) -> None:
    system = platform.system()
    try:
        if system == "Windows":
            from ctypes import cast, POINTER
            from comtypes import CLSCTX_ALL
            from pycaw.pycaw import AudioUtilities, IAudioEndpointVolume

            devices = AudioUtilities.GetSpeakers()
            interface = devices.Activate(IAudioEndpointVolume._iid_, CLSCTX_ALL, None)
            volume = cast(interface, POINTER(IAudioEndpointVolume))
            volume.SetMasterVolumeLevelScalar(level / 100, None)
        elif system == "Darwin":
            import os
            os.system(f"osascript -e 'set volume output volume {level}'")
        else:
            import os
            os.system(f"amixer -D pulse sset Master {level}% >/dev/null 2>&1")
    except Exception as exc:
        print(f"[volume] could not set volume: {exc}")


def set_brightness(level: int) -> None:
    try:
        import screen_brightness_control as sbc
        sbc.set_brightness(level)
    except Exception as exc:
        print(f"[brightness] could not set brightness: {exc}")


def handle_line(line: str) -> None:
    line = line.strip()
    if not line or ":" not in line:
        return
    kind, _, payload = line.partition(":")

    with _lock:
        try:
            if kind == "MOVE":
                dx, _, dy = payload.partition(",")
                pyautogui.moveRel(float(dx), float(dy), duration=0)
            elif kind == "SCROLL":
                pyautogui.scroll(int(float(payload)))
            elif kind == "CLICK":
                pyautogui.click(button=payload.lower())
            elif kind == "DCLICK":
                pyautogui.doubleClick(button=payload.lower())
            elif kind == "DOWN":
                pyautogui.mouseDown(button=payload.lower())
            elif kind == "UP":
                pyautogui.mouseUp(button=payload.lower())
            elif kind == "VOL":
                set_volume(int(payload))
            elif kind == "BRI":
                set_brightness(int(payload))
            elif kind == "HELLO":
                print(f"[handshake] {payload}")
        except Exception as exc:
            print(f"[warn] failed to handle '{line}': {exc}")


def run_udp_listener(port: int) -> None:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", port))
    print(f"[udp] listening on 0.0.0.0:{port}")
    while True:
        data, _addr = sock.recvfrom(1024)
        for line in data.decode("utf-8", errors="ignore").splitlines():
            handle_line(line)


def run_bluetooth_listener(com_port: str, baudrate: int = 115200) -> None:
    import serial

    print(f"[bluetooth] opening {com_port}")
    with serial.Serial(com_port, baudrate=baudrate, timeout=1) as ser:
        buffer = ""
        while True:
            chunk = ser.read(max(ser.in_waiting, 1)).decode("utf-8", errors="ignore")
            if not chunk:
                continue
            buffer += chunk
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                handle_line(line)


def main() -> None:
    parser = argparse.ArgumentParser(description="Remote Mouse Controller host receiver")
    parser.add_argument("--udp-port", type=int, default=5005)
    parser.add_argument(
        "--bt-port", type=str, default=None,
        help="Bluetooth serial port, e.g. COM5 (Windows) or /dev/rfcomm0 (Linux)"
    )
    args = parser.parse_args()

    threads = [threading.Thread(target=run_udp_listener, args=(args.udp_port,), daemon=True)]
    if args.bt_port:
        threads.append(threading.Thread(target=run_bluetooth_listener, args=(args.bt_port,), daemon=True))
    else:
        print("[bluetooth] no --bt-port given — Bluetooth transport disabled for this run")

    for t in threads:
        t.start()

    print("Receiver running. Press Ctrl+C to stop.")
    try:
        while True:
            threading.Event().wait(3600)
    except KeyboardInterrupt:
        print("\nStopping.")


if __name__ == "__main__":
    main()
