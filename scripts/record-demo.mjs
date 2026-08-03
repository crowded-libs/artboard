#!/usr/bin/env node
/**
 * Record a README demo GIF of the Crowded Café Artboard gallery.
 *
 * Default: serve a local café export, drive chrome controls via accessibility
 * hit-targets (Compose Wasm paints to a canvas; pointer events go through
 * coordinate clicks), capture keyframe screenshots, and encode a palette GIF
 * with system ffmpeg. Locale is toggled via `#locale=ar` deep links (Compose
 * Popup menus are not reliably scriptable under Playwright).
 *
 * Prerequisites:
 *   - Node 20+
 *   - ffmpeg on PATH
 *   - Google Chrome (or Chromium) installed
 *   - npm install in scripts/ (once)
 *   - A café export (or pass --url / --build):
 *       ./gradlew -p showcase/cafe :shared:artboardExport
 *
 * Usage:
 *   node scripts/record-demo.mjs
 *   node scripts/record-demo.mjs --build
 *   node scripts/record-demo.mjs --url https://crowded-libs.github.io/artboard/
 *   node scripts/record-demo.mjs --out artboard_sample.gif --port 8765
 */

import { spawnSync } from 'node:child_process'
import { createServer } from 'node:http'
import {
  createReadStream,
  existsSync,
  mkdirSync,
  rmSync,
  statSync,
} from 'node:fs'
import { dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const __dirname = dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = resolve(__dirname, '..')

const DEFAULTS = {
  exportDir: join(REPO_ROOT, 'showcase/cafe/shared/build/artboard/export'),
  out: join(REPO_ROOT, 'artboard_sample.gif'),
  port: 8765,
  width: 1440,
  height: 900,
  gifWidth: 1280,
  fps: 10,
  headless: true,
}

function parseArgs(argv) {
  const opts = { ...DEFAULTS, url: null, build: false, keepFrames: false }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    const next = () => {
      const v = argv[++i]
      if (v == null) throw new Error(`Missing value for ${a}`)
      return v
    }
    switch (a) {
      case '--url':
        opts.url = next()
        break
      case '--export-dir':
        opts.exportDir = resolve(next())
        break
      case '--out':
        opts.out = resolve(next())
        break
      case '--port':
        opts.port = Number(next())
        break
      case '--width':
        opts.width = Number(next())
        break
      case '--height':
        opts.height = Number(next())
        break
      case '--gif-width':
        opts.gifWidth = Number(next())
        break
      case '--fps':
        opts.fps = Number(next())
        break
      case '--build':
        opts.build = true
        break
      case '--headed':
        opts.headless = false
        break
      case '--keep-frames':
        opts.keepFrames = true
        break
      case '--help':
      case '-h':
        printHelp()
        process.exit(0)
        break
      default:
        throw new Error(`Unknown argument: ${a}`)
    }
  }
  return opts
}

function printHelp() {
  console.log(`Usage: node scripts/record-demo.mjs [options]

Options:
  --build              Run :shared:artboardExport before recording
  --export-dir <path>  Local export to serve (default: showcase/cafe/shared/build/artboard/export)
  --url <url>          Record against an already-running gallery (skips local serve)
  --out <path>         Output GIF path (default: artboard_sample.gif)
  --port <n>           Local static server port (default: 8765)
  --width / --height   Capture viewport (default: 1440x900)
  --gif-width <n>      Scaled GIF width (default: 1280)
  --fps <n>            GIF frame rate (default: 12)
  --headed             Show the browser window
  --keep-frames        Leave raw screencast PNGs under scripts/.record-work/
  -h, --help           Show this help
`)
}

function log(msg) {
  console.log(`[record-demo] ${msg}`)
}

function requireBinary(name) {
  const result = spawnSync(name, ['-version'], { encoding: 'utf8' })
  // ffmpeg prints to stderr; -version still exits 0
  if (result.error || (result.status !== 0 && result.status !== null && name !== 'ffmpeg')) {
    // ffmpeg -version returns 0; accept any non-ENOENT
  }
  if (result.error?.code === 'ENOENT') {
    throw new Error(`Required binary not found on PATH: ${name}`)
  }
  if (name === 'ffmpeg') {
    const probe = spawnSync('ffmpeg', ['-version'], { encoding: 'utf8' })
    if (probe.error?.code === 'ENOENT') {
      throw new Error('ffmpeg not found on PATH (required to encode the GIF)')
    }
  }
}

function runBuild() {
  log('Building café export (:shared:artboardExport)…')
  const r = spawnSync(
    './gradlew',
    ['-p', 'showcase/cafe', ':shared:artboardExport', '--console=plain'],
    { cwd: REPO_ROOT, stdio: 'inherit' },
  )
  if (r.status !== 0) {
    throw new Error(`artboardExport failed with exit ${r.status}`)
  }
}

const CONTENT_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.wasm': 'application/wasm',
  '.json': 'application/json',
  '.map': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
}

function startStaticServer(rootDir, port) {
  return new Promise((resolveServer, reject) => {
    const server = createServer((req, res) => {
      try {
        const urlPath = decodeURIComponent((req.url || '/').split('?')[0])
        let rel = urlPath === '/' ? '/index.html' : urlPath
        rel = rel.replace(/^\/+/, '')
        const filePath = resolve(rootDir, rel)
        if (!filePath.startsWith(resolve(rootDir))) {
          res.writeHead(403).end('Forbidden')
          return
        }
        if (!existsSync(filePath) || !statSync(filePath).isFile()) {
          res.writeHead(404).end('Not found')
          return
        }
        const type = CONTENT_TYPES[extname(filePath).toLowerCase()] || 'application/octet-stream'
        res.writeHead(200, {
          'Content-Type': type,
          'Cache-Control': 'no-store',
        })
        createReadStream(filePath).pipe(res)
      } catch (err) {
        res.writeHead(500).end(String(err))
      }
    })
    server.once('error', reject)
    server.listen(port, '127.0.0.1', () => {
      resolveServer({
        url: `http://127.0.0.1:${port}/`,
        close: () =>
          new Promise((res, rej) => {
            server.close((err) => (err ? rej(err) : res()))
          }),
      })
    })
  })
}

async function sleep(ms) {
  await new Promise((r) => setTimeout(r, ms))
}

/**
 * Compose Wasm paints under a canvas; click a11y hit-targets by coordinates.
 * Use `first: true` when preview bodies expose the same label (e.g. kind "All"
 * vs category-chip "All" inside a frame).
 */
async function clickRole(page, role, name, { exact = true, timeout = 15_000, first = false } = {}) {
  let loc = page.getByRole(role, { name, exact })
  if (first) loc = loc.first()
  await loc.waitFor({ state: 'attached', timeout })
  const box = await loc.boundingBox()
  if (!box || box.width < 1 || box.height < 1) {
    throw new Error(`No bounding box for ${role} "${name}"`)
  }
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  return box
}

async function waitForGallery(page) {
  // Compose exposes chrome via the a11y tree long before DOM querySelector is useful.
  const deadline = Date.now() + 90_000
  while (Date.now() < deadline) {
    const fit = page.getByRole('button', { name: 'Fit', exact: true })
    if ((await fit.count()) > 0) {
      // Prefer a loaded frame count so previews have painted.
      const snap = await page.accessibility.snapshot({ interestingOnly: true })
      const labels = collectNames(snap)
      if (labels.some((n) => /\d+\s+frames?/.test(n))) {
        await sleep(1_200)
        return
      }
    }
    await sleep(400)
  }
  throw new Error('Gallery chrome did not become ready in time')
}

function collectNames(node, out = []) {
  if (!node) return out
  if (node.name) out.push(node.name)
  for (const child of node.children || []) collectNames(child, out)
  return out
}

async function clearSearch(page) {
  await clickRole(page, 'textbox', 'Search preview frames')
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
  await page.keyboard.press('Backspace')
}

async function typeSearch(page, query) {
  await clickRole(page, 'textbox', 'Search preview frames')
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
  await page.keyboard.type(query, { delay: 70 })
}

/** Café Settings screen — strongest en/es/ar + RTL coverage in the showcase. */
const SETTINGS_FRAME_ID =
  'com.crowdedlibs.cafe.ui.screens.SettingsScreenPreview::Settings'

/**
 * Update the gallery hash deep link. Uses `location.hash` (fires `hashchange`)
 * so the Wasm host can apply frame focus and/or locale without the popup menus.
 */
async function setGalleryHash(page, { frameId = null, localeTag = null } = {}) {
  const parts = []
  if (frameId) parts.push(`frame=${encodeURIComponent(frameId)}`)
  if (localeTag) parts.push(`locale=${encodeURIComponent(localeTag)}`)
  const hash = parts.join('&')
  await page.evaluate((h) => {
    window.location.hash = h
  }, hash)
}

async function selectFrameByName(page, names) {
  for (const name of names) {
    const loc = page.getByRole('button', { name, exact: true })
    if ((await loc.count()) === 0) continue
    const box = await loc.first().boundingBox()
    if (!box || box.width <= 8 || box.height <= 8) continue
    await page.mouse.click(box.x + box.width / 2, box.y + Math.min(box.height * 0.12, 18))
    log(`Selected ${name}`)
    return name
  }
  return null
}

async function runTour(page, opts, hold) {
  const midX = opts.width * 0.5
  const boardY = opts.height * 0.55

  // Let camera settle, then open on a clean overview.
  await sleep(600)
  log('Fit overview')
  await clickRole(page, 'button', 'Fit')
  await sleep(700)
  await hold(1.4)

  log('Kind filter: Screens')
  await clickRole(page, 'button', 'Screens', { first: true })
  await sleep(500)
  await hold(1.3)

  log('Kind filter: Components')
  await clickRole(page, 'button', 'Components', { first: true })
  await sleep(500)
  await hold(1.3)

  log('Kind filter: All')
  await clickRole(page, 'button', 'All', { first: true })
  await sleep(400)
  await hold(0.8)

  log('Search: cart')
  await typeSearch(page, 'cart')
  await sleep(700) // search debounce + fit
  await hold(1.5)

  log('Clear search, fit')
  await clearSearch(page)
  await sleep(300)
  await clickRole(page, 'button', 'Fit')
  await sleep(500)
  await hold(0.9)

  log('Layout grid overlay on')
  await clickRole(page, 'button', 'Grid')
  await sleep(400)
  await hold(0.8)

  log('Zoom into screens to show grid columns')
  for (let i = 0; i < 5; i++) {
    await clickRole(page, 'button', '+')
    await sleep(180)
  }
  await sleep(400)
  await hold(1.2)

  // Settings has full composeResources coverage (en / es / ar) including RTL.
  log('Select Settings screen (locale showcase)')
  const settingsSelected = await selectFrameByName(page, [
    'Settings, screen preview',
    'Checkout, screen preview',
    'Detail, screen preview',
    'Preparing, screen preview',
  ])
  if (!settingsSelected) log('No named screen frame hit-target found; continuing')
  await sleep(400)
  // Push in further so string changes read clearly.
  for (let i = 0; i < 3; i++) {
    await clickRole(page, 'button', '+')
    await sleep(160)
  }
  await sleep(400)
  await hold(1.3)

  log('Locale → Arabic (deep link; popup menus are not scriptable under Playwright)')
  // Compose Popup menus swallow pointer input after open, so the tour drives
  // locale via the hash deep link (`#frame=…&locale=ar`) instead of the menu.
  await setGalleryHash(page, {
    frameId: SETTINGS_FRAME_ID,
    localeTag: 'ar',
  })
  // Wait for resource locale + RTL recompose on the selected screen.
  await sleep(1_200)
  // Toolbar should now show "Arabic" as the locale anchor.
  await hold(1.8)

  log('Theme → dark (Arabic still applied)')
  await clickRole(page, 'button', 'Switch to dark theme')
  await sleep(500)
  await hold(1.4)

  log('Fit, then pan across the board')
  await clickRole(page, 'button', 'Fit')
  await sleep(500)
  await hold(0.8)

  await page.mouse.move(midX + 140, boardY)
  await page.mouse.down()
  const steps = 12
  for (let i = 1; i <= steps; i++) {
    const t = i / steps
    await page.mouse.move(midX + 140 + (-380) * t, boardY + 50 * t)
    if (i % 2 === 0) await hold(0.1)
    else await sleep(20)
  }
  await page.mouse.up()
  await hold(0.7)

  log('Components zone')
  await clickRole(page, 'button', 'Components', { first: true })
  await sleep(450)
  await hold(1.3)

  log('Back to All + Fit')
  await clickRole(page, 'button', 'All', { first: true })
  await sleep(300)
  await clickRole(page, 'button', 'Fit')
  await sleep(450)
  await hold(1.0)

  // Toggle grid off so the final beat reads as a clean overview.
  log('Grid off')
  await clickRole(page, 'button', 'Grid')
  await sleep(350)
  await hold(0.7)

  const light = page.getByRole('button', { name: 'Switch to light theme', exact: true })
  if ((await light.count()) > 0) {
    log('Theme → light (closing beat)')
    await clickRole(page, 'button', 'Switch to light theme')
    await sleep(450)
    await hold(1.2)
  }

  log('Hold final frame')
  await hold(1.0)
}

/**
 * Step-based capture: the tour calls `hold(seconds)` after each action.
 * Continuous screenshot timers starve Compose Wasm; keyframe holds do not.
 */
function createFrameRecorder(page, frameDir, { fps }) {
  rmSync(frameDir, { recursive: true, force: true })
  mkdirSync(frameDir, { recursive: true })
  let frameIndex = 0

  async function snapOnce() {
    const index = ++frameIndex
    const path = join(frameDir, `frame_${String(index).padStart(5, '0')}.jpg`)
    await page.screenshot({ path, type: 'jpeg', quality: 72 })
  }

  /** Hold the current viewport for ~[seconds] of GIF time. */
  async function hold(seconds) {
    const frames = Math.max(1, Math.round(seconds * fps))
    for (let i = 0; i < frames; i++) {
      await snapOnce()
      // Tiny yield so Compose can process input between captures.
      if (i + 1 < frames) await sleep(16)
    }
  }

  return {
    hold,
    frameCount: () => frameIndex,
  }
}

function framesToGif(frameDir, gifPath, { fps, gifWidth }) {
  log(`Encoding GIF → ${gifPath}`)
  const pattern = join(frameDir, 'frame_%05d.jpg')
  // Input frames are already timed at `fps`; only scale + palette for the GIF.
  const vf =
    `scale=${gifWidth}:-1:flags=lanczos,` +
    `split[s0][s1];[s0]palettegen=max_colors=160:stats_mode=diff[p];` +
    `[s1][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle`

  const r = spawnSync(
    'ffmpeg',
    [
      '-y',
      '-framerate',
      String(fps),
      '-i',
      pattern,
      '-vf',
      vf,
      '-loop',
      '0',
      gifPath,
    ],
    { encoding: 'utf8' },
  )
  if (r.status !== 0) {
    console.error(r.stderr || r.stdout)
    throw new Error(`ffmpeg failed with exit ${r.status}`)
  }
  const sizeMb = (statSync(gifPath).size / (1024 * 1024)).toFixed(2)
  log(`GIF ready (${sizeMb} MB)`)
}

async function main() {
  const opts = parseArgs(process.argv.slice(2))
  requireBinary('ffmpeg')

  if (opts.build) {
    runBuild()
  }

  let server = null
  let galleryUrl = opts.url
  if (!galleryUrl) {
    if (!existsSync(join(opts.exportDir, 'index.html'))) {
      throw new Error(
        `No export at ${opts.exportDir}\n` +
          `Run with --build, or: ./gradlew -p showcase/cafe :shared:artboardExport`,
      )
    }
    log(`Serving ${opts.exportDir} on port ${opts.port}`)
    server = await startStaticServer(opts.exportDir, opts.port)
    galleryUrl = server.url
  }
  log(`Gallery URL: ${galleryUrl}`)

  const workDir = join(REPO_ROOT, 'scripts', '.record-work')
  mkdirSync(workDir, { recursive: true })
  const frameDir = join(workDir, 'frames')

  // Prefer system Chrome; fall back to Playwright Chromium if installed.
  let browser
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: opts.headless })
  } catch {
    log('System Chrome unavailable; trying Playwright Chromium')
    browser = await chromium.launch({ headless: opts.headless })
  }

  const context = await browser.newContext({
    viewport: { width: opts.width, height: opts.height },
    deviceScaleFactor: 1,
  })

  // Fresh prefs so the tour always starts from defaults (light, no grid, etc.).
  await context.addInitScript(() => {
    try {
      localStorage.clear()
      sessionStorage.clear()
    } catch {
      /* ignore */
    }
  })

  const page = await context.newPage()
  page.setDefaultTimeout(20_000)

  try {
    log('Navigating…')
    await page.goto(galleryUrl, { waitUntil: 'domcontentloaded', timeout: 60_000 })
    // Let Wasm/Skia settle before screencast starts so the first frames are useful.
    await waitForGallery(page)
    const recorder = createFrameRecorder(page, frameDir, { fps: opts.fps })
    await runTour(page, opts, recorder.hold)
    log(`Captured ${recorder.frameCount()} frames`)
  } finally {
    await page.close().catch(() => {})
    await context.close().catch(() => {})
    await browser.close().catch(() => {})
    if (server) await server.close().catch(() => {})
  }

  mkdirSync(dirname(opts.out), { recursive: true })
  framesToGif(frameDir, opts.out, { fps: opts.fps, gifWidth: opts.gifWidth })

  if (!opts.keepFrames) {
    rmSync(frameDir, { recursive: true, force: true })
  } else {
    log(`Kept frames: ${frameDir}`)
  }

  log('Done.')
}

main().catch((err) => {
  console.error(`[record-demo] ${err.stack || err}`)
  process.exit(1)
})
