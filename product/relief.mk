# Relief product additions/removals.
# Included at the end of device/google/tegu/aosp_tegu.mk by scripts/apply-relief.sh.

PRODUCT_PACKAGES += \
    ReliefSetup

# Application-layer removals only. These module names were verified against the
# current GrapheneOS Android 17 source/prebuilt definitions.
#
# Deliberately keep framework/Mainline/telephony/CellBroadcast/location/WebView/
# GmsCompat infrastructure. Also keep Seedvault, ThemePicker and WallpaperPicker
# in the first hardware-test build until setup/settings dependency tests pass.
PRODUCT_PACKAGES -= \
    Auditor \
    ExactCalculator \
    LogViewer \
    Messaging \
    PdfViewerGOS \
    Traceur

# Kept intentionally for V1:
# - DeskClock: alarms/timers are basic phone functionality and negligible at idle.
# - Camera: QR scanning and camera intents used by messaging/navigation.
# - DocumentsUI: Android's system file picker; third-party apps depend on it.
# - AppStore: installs sandboxed Google Play, which is required for Relief RCS.
# - Vanadium + WebView: WebView is mandatory app infrastructure and the browser
#   remains for authentication/custom-tab compatibility until tested otherwise.
# - CellBroadcastReceiver/EmergencyInfo: emergency functionality is non-negotiable.

PRODUCT_PRODUCT_PROPERTIES += \
    ro.relief.enabled=true \
    ro.relief.device=tegu \
    ro.relief.base=2026081300
