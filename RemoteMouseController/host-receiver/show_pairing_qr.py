"""
Prints a QR code in the terminal (and saves pairing_qr.png) encoding this
machine's IP and UDP port, so the app's "Scan QR code" button can pair with
a single tap instead of typing the IP by hand. Bluetooth pairing doesn't
need this — pair via Android's Bluetooth settings instead.

Run on the host PC, alongside receiver.py:
    python show_pairing_qr.py
"""

import json
import socket

import qrcode


def local_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()


def main(port: int = 5005) -> None:
    payload = json.dumps({"host": local_ip(), "port": port})
    print(f"Pairing payload: {payload}")

    img = qrcode.make(payload)
    img.save("pairing_qr.png")

    qr = qrcode.QRCode()
    qr.add_data(payload)
    qr.print_ascii(invert=True)
    print("Saved pairing_qr.png — scan it from the app's 'Scan QR code' button.")


if __name__ == "__main__":
    main()
