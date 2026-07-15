package artboard.host

import artboard.canvas.ScreenDeviceSize

/** A named device viewport offered in the board's device dropdown. */
data class ArtboardDevice(
    val label: String,
    val size: ScreenDeviceSize,
)

/**
 * Common device viewports in logical dp/pt. "Declared" (no override) is
 * represented by a null [ScreenDeviceSize], not an entry here.
 */
val artboardDevicePresets: List<ArtboardDevice> = listOf(
    ArtboardDevice("iPhone 17", ScreenDeviceSize(402, 874)),
    ArtboardDevice("iPhone 17 Pro Max", ScreenDeviceSize(440, 956)),
    ArtboardDevice("iPhone SE", ScreenDeviceSize(375, 667)),
    ArtboardDevice("Pixel 9", ScreenDeviceSize(412, 923)),
    ArtboardDevice("Galaxy S25 Ultra", ScreenDeviceSize(384, 832)),
    ArtboardDevice("iPad Pro 11″", ScreenDeviceSize(834, 1194)),
)
