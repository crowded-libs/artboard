package artboard.model

/** Broad visual category inferred for a discovered Compose preview. */
enum class PreviewKind {
    /** A full screen, route, page, or scaffold preview. */
    Screen,

    /** A reusable component or other partial-interface preview. */
    Component,
}
