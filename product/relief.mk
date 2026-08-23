# Relief product additions/removals.
# Included at the end of device/google/tegu/aosp_tegu.mk by scripts/apply-relief.sh.

PRODUCT_PACKAGES += \
    ReliefSetup

# Application-layer removals only. Do not remove framework/Mainline/telephony/
# CellBroadcast/location/WebView/GmsCompat infrastructure here.
PRODUCT_PACKAGES -= \
    ExactCalculator \
    LogViewer \
    PdfViewer \
    Seedvault \
    ThemePicker \
    WallpaperPicker2 \
    Traceur

# Keep DeskClock, Camera, Files/DocumentsUI and Vanadium/WebView for now.
# DeskClock has no meaningful idle cost and alarms are basic phone functionality.
# Camera is retained for QR codes and messaging/navigation camera intents.
# Vanadium may later be split so the browser UI is omitted while WebView remains.

PRODUCT_PRODUCT_PROPERTIES += \
    ro.relief.enabled=true \
    ro.relief.device=tegu \
    ro.relief.base=2026081300
