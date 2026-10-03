// Builds the TriVoKo master plan (.docx) from content/*.md (a small markdown-like format).
// Copied from the EventHub guide generator (eventhub/tools/showcase-src/book/gen.js).
//   # / ## / ###  headings (chapter numbers are written in the text)
//   - item / 1. item     lists          | a | b |   tables (first row = header)
//   ```lang ... ```      code block     > text       note box      >> text   real-life example box
//   ![caption](file)     image from Showcase/screenshots or diagrams
//   @code path :: start text [:: max lines]   a real block of source code (until braces close)
//   @lines path :: from :: to                 real source lines (1-based, inclusive)
//   @tables    every database table with its columns (from the Flyway migrations)
//   @endpoints every REST endpoint (from the controllers)
//   @commits   the git history
//   @pagebreak
const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')
const d = require('docx')

const ROOT = 'D:/Java_FullStack_Journey/03_TriVoKo_Main_Project/'
const SHOTS = path.join(__dirname, 'diagrams/')
const DIAGRAMS = path.join(__dirname, 'diagrams/')
const OUT = process.argv[2]
// optional: another content folder and title (used for the short notes booklet)
const CONTENT = process.argv[3] || 'content'
const DOC_TITLE = process.argv[4] || 'Master Plan'
const DOC_SUB = process.argv[5] || 'Goals, outcomes, architecture, core flows, the four standout features, database, API, pages, testing, the 13-phase plan, risks and launch - explained with diagrams'

const FONT = 'Calibri', MONO = 'Consolas'
const INK = '1F2937', NIGHT = '134E4A', BRAND = '0F766E', MUTED = '6B7280'
const CONTENT_W = 9026 // A4 width 11906 - 2 x 1440 margins

const border = { style: d.BorderStyle.SINGLE, size: 4, color: 'D1D5DB' }
const borders = { top: border, bottom: border, left: border, right: border }

// ---------- inline text: **bold**, `code`, *italic* ----------
function runs(text, base = {}) {
  const out = []
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*\s][^*]*\*)/g
  let last = 0, m
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(new d.TextRun({ text: text.slice(last, m.index), ...base }))
    const t = m[0]
    if (t.startsWith('**')) out.push(new d.TextRun({ text: t.slice(2, -2), bold: true, ...base }))
    else if (t.startsWith('`')) out.push(new d.TextRun({ text: t.slice(1, -1), font: MONO, size: 19, color: 'B4235A', ...base }))
    else out.push(new d.TextRun({ text: t.slice(1, -1), italics: true, ...base }))
    last = m.index + t.length
  }
  if (last < text.length) out.push(new d.TextRun({ text: text.slice(last), ...base }))
  return out
}

const para = (text, opts = {}) => new d.Paragraph({ children: runs(text), spacing: { after: 120, line: 288 }, ...opts })

// ---------- code ----------
function codeBlock(lines, label) {
  const out = []
  if (label) out.push(new d.Paragraph({ children: [new d.TextRun({ text: label, font: FONT, size: 17, color: MUTED, italics: true })], spacing: { before: 120, after: 40 }, keepNext: true }))
  const clean = lines.map((l) => l.replace(/\t/g, '    ').replace(/\s+$/, ''))
  // remove the common indent so excerpts from deep inside a class start at the left
  const indents = clean.filter((l) => l.trim()).map((l) => l.match(/^ */)[0].length)
  const cut = indents.length ? Math.min(...indents) : 0
  clean.forEach((l, i) => {
    out.push(new d.Paragraph({
      children: [new d.TextRun({ text: l.slice(cut) || ' ', font: MONO, size: 17, color: '111827' })],
      shading: { type: d.ShadingType.CLEAR, color: 'auto', fill: 'F3F4F6' },
      spacing: { before: i === 0 ? 60 : 0, after: i === clean.length - 1 ? 160 : 0, line: 240 },
      indent: { left: 120, right: 120 },
    }))
  })
  return out
}

function readSource(rel) {
  const p = ROOT + rel
  if (!fs.existsSync(p)) throw new Error('missing source ' + rel)
  return fs.readFileSync(p, 'utf8').replace(/\r/g, '').split('\n')
}

function codeFrom(rel, start, max = 70) {
  const lines = readSource(rel)
  let i = lines.findIndex((l) => l.includes(start))
  if (i < 0) throw new Error(`"${start}" not found in ${rel}`)
  // include the javadoc / annotations right above
  let s = i
  while (s > 0 && /^\s*(\*|\/\*\*|\/\/|@)/.test(lines[s - 1])) s--
  const out = []
  let depth = 0, opened = false
  for (let k = s; k < lines.length && out.length < max; k++) {
    out.push(lines[k])
    if (k >= i) {
      for (const ch of lines[k].replace(/"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/g, '')) {
        if (ch === '{') { depth++; opened = true }
        if (ch === '}') depth--
      }
      if (opened && depth <= 0) break
      if (!opened && /;\s*$/.test(lines[k]) && k > i) break
    }
  }
  return codeBlock(out, `${rel.split('/').pop()}  (${rel})`)
}

// ---------- tables ----------
function table(rows, widths) {
  const cols = rows[0].length
  rows = rows.map((r) => (r.length > cols ? [...r.slice(0, cols - 1), r.slice(cols - 1).join(' | ')] : r.concat(Array(cols - r.length).fill(''))))
  const w = widths || Array(cols).fill(Math.floor(CONTENT_W / cols))
  const total = w.reduce((a, b) => a + b, 0)
  return new d.Table({
    width: { size: total, type: d.WidthType.DXA }, columnWidths: w,
    rows: rows.map((r, ri) => new d.TableRow({
      tableHeader: ri === 0, cantSplit: true,
      children: r.map((c, ci) => new d.TableCell({
        width: { size: w[ci], type: d.WidthType.DXA }, borders,
        shading: { type: d.ShadingType.CLEAR, color: 'auto', fill: ri === 0 ? NIGHT : (ri % 2 ? 'FFFFFF' : 'F0FDFA') },
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new d.Paragraph({ children: runs(c, ri === 0 ? { bold: true, color: 'FFFFFF', size: 19 } : { size: 19 }), spacing: { after: 0 } })],
      })),
    })),
  })
}

function widthsFor(rows) {
  // share of each column = its longest text, with limits
  const cols = rows[0].length
  const len = Array(cols).fill(0).map((_, c) => Math.min(60, Math.max(6, ...rows.map((r) => (r[c] || '').length))))
  const sum = len.reduce((a, b) => a + b, 0)
  const w = len.map((l) => Math.floor((CONTENT_W * l) / sum))
  w[w.length - 1] += CONTENT_W - w.reduce((a, b) => a + b, 0)
  return w
}

// ---------- boxes ----------
function box(text, fill, label, labelColor) {
  const children = []
  if (label) children.push(new d.TextRun({ text: label + '  ', bold: true, color: labelColor }))
  children.push(...runs(text))
  return new d.Paragraph({
    children, shading: { type: d.ShadingType.CLEAR, color: 'auto', fill },
    border: { left: { style: d.BorderStyle.SINGLE, size: 18, color: labelColor, space: 8 } },
    spacing: { before: 120, after: 160, line: 288 }, indent: { left: 200, right: 120 },
  })
}

// ---------- images ----------
function image(file, caption, widthIn = 6.2) {
  const p = fs.existsSync(SHOTS + file) ? SHOTS + file : DIAGRAMS + file
  const buf = fs.readFileSync(p)
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20) // PNG header
  const wpx = widthIn * 96, hpx = Math.round((wpx * h) / w)
  return [
    new d.Paragraph({ children: [new d.ImageRun({ type: 'png', data: buf, transformation: { width: wpx, height: hpx }, altText: { title: caption, description: caption, name: file } })], alignment: d.AlignmentType.CENTER, spacing: { before: 120, after: 60 }, keepNext: true }),
    new d.Paragraph({ children: [new d.TextRun({ text: caption, italics: true, size: 18, color: MUTED })], alignment: d.AlignmentType.CENTER, spacing: { after: 200 } }),
  ]
}

// ---------- generated sections ----------
function dbTables() {
  const out = []
  const dir = ROOT + 'backend/src/main/resources/db/migration/'
  const files = fs.readdirSync(dir).filter((f) => f.endsWith('.sql')).sort((a, b) => parseInt(a.slice(1)) - parseInt(b.slice(1)))
  const alters = {}
  for (const f of files) {
    const sql = fs.readFileSync(dir + f, 'utf8').replace(/\r/g, '')
    for (const m of sql.matchAll(/ALTER TABLE (\w+)([\s\S]*?);/g)) {
      for (const c of m[2].matchAll(/ADD COLUMN (\w+)\s+([A-Z]+(?:\(\d+(?:,\d+)?\))?)[^,\n]*(?:,)?\s*(?:--\s*(.*))?/g)) {
        ;(alters[m[1]] ||= []).push([c[1], c[2], (c[3] || '').trim() + ` (added in ${f.split('__')[0]})`])
      }
    }
  }
  for (const f of files) {
    const sql = fs.readFileSync(dir + f, 'utf8').replace(/\r/g, '')
    for (const m of sql.matchAll(/CREATE TABLE (\w+) \(([\s\S]*?)\n\);/g)) {
      const rows = [['Column', 'Type', 'Meaning / rule']]
      for (const line of m[2].split('\n')) {
        const t = line.trim()
        if (!t || /^(CONSTRAINT|INDEX|PRIMARY|UNIQUE|KEY|--)/i.test(t)) continue
        const cm = t.match(/^(\w+)\s+([A-Z]+(?:\(\d+(?:,\s*\d+)?\))?)(.*?)(?:--\s*(.*))?$/)
        if (!cm) continue
        const extra = []
        if (/PRIMARY KEY/i.test(cm[3])) extra.push('primary key')
        if (/NOT NULL/i.test(cm[3]) && !/PRIMARY/i.test(cm[3])) extra.push('required')
        if (/UNIQUE/i.test(cm[3])) extra.push('unique')
        const def = cm[3].match(/DEFAULT ([^,\s]+(?:\(\d\))?)/i)
        if (def) extra.push('default ' + def[1])
        rows.push([cm[1], cm[2], [cm[4], extra.join(', ')].filter(Boolean).join(' - ')])
      }
      for (const a of alters[m[1]] || []) rows.push(a)
      const fks = [...m[2].matchAll(/FOREIGN KEY\s*\((\w+)\)\s+REFERENCES\s+(\w+)/g)].map((x) => `${x[1]} → ${x[2]}`)
      out.push(new d.Paragraph({ text: `Table ${m[1]}  (${f.split('__')[0]})`, heading: d.HeadingLevel.HEADING_3, keepNext: true }))
      out.push(table(rows, [2300, 2000, CONTENT_W - 4300]))
      if (fks.length) out.push(para(`Links to other tables: ${fks.map((x) => '`' + x + '`').join(', ')}.`, { spacing: { before: 80, after: 200 } }))
      else out.push(new d.Paragraph({ spacing: { after: 160 } }))
    }
  }
  return out
}

function endpoints() {
  const base = ROOT + 'backend/src/main/java/com/eventhub/'
  const rows = [['Method', 'URL', 'Who may call it', 'Controller']]
  const walk = (dir) => fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(dir + e.name + '/') : e.name.endsWith('Controller.java') ? [dir + e.name] : []))
  for (const f of walk(base).sort()) {
    const src = fs.readFileSync(f, 'utf8').replace(/\r/g, '')
    const prefix = (src.match(/@RequestMapping\("([^"]+)"\)/) || [])[1] || ''
    const lines = src.split('\n')
    lines.forEach((l, i) => {
      const m = l.match(/@(Get|Post|Put|Delete|Patch)Mapping(?:\((?:value = )?"?([^",)]*)"?[^)]*\))?/)
      if (!m) return
      const url = prefix + (m[2] || '')
      let who = ''
      for (let k = i - 3; k <= i + 3; k++) { const pa = (lines[k] || '').match(/@PreAuthorize\("(.+)"\)/); if (pa) who = pa[1] }
      if (!who) {
        if (url.startsWith('/api/admin')) who = 'ADMIN'
        else if (/^\/api\/(health|auth\/(register|login|logout|me))$/.test(url) || (m[1] === 'Get' && /^\/api\/(events|clubs|tags)/.test(url)) || url.startsWith('/api/certificates/verify') || url === '/api/payments/stripe/webhook') who = 'anyone'
        else if (/\/(approve|reject)$/.test(url)) who = 'ADMIN'
        else who = 'logged in'
      }
      who = who.replace('@clubAccess.', 'club rule: ').replace(/hasRole\('(\w+)'\)/, '$1')
      rows.push([m[1].toUpperCase(), '`' + url + '`', who, f.split('/').pop().replace('.java', '')])
    })
  }
  return [table(rows, [900, 3700, 2300, 2126]), para(`${rows.length - 1} endpoints in total. "logged in" = any logged-in user; club rules are checked by the ClubAccess bean on every call.`, { spacing: { before: 100, after: 200 } })]
}

function commits() {
  const log = require('child_process').execFileSync('git', ['log', '--reverse', '--format=%h|%ad|%s', '--date=format:%d %b %H:%M'], { cwd: ROOT, encoding: 'utf8' }).trim().split('\n')
  const rows = [['#', 'Commit', 'When', 'What changed']]
  log.forEach((l, i) => { const [h, w, ...s] = l.split('|'); rows.push([String(i + 1), h, w, s.join('|').slice(0, 170)]) })
  return [table(rows, [500, 1000, 1400, CONTENT_W - 2900])]
}

// ---------- parser ----------
// @widths a b c ...  sets the column widths (DXA, sum 9026) of the next table
let nextWidths = null
function parse(text, out) {
  const lines = text.replace(/\r/g, '').split('\n')
  let i = 0
  let listNo = 0
  while (i < lines.length) {
    const l = lines[i]
    if (!l.trim()) { i++; continue }
    if (l.startsWith('```')) {
      const block = []
      i++
      while (i < lines.length && !lines[i].startsWith('```')) block.push(lines[i++])
      i++
      out.push(...codeBlock(block))
      continue
    }
    if (l.startsWith('@widths ')) { nextWidths = l.slice(8).trim().split(/\s+/).map(Number); i++; continue }
    if (l.startsWith('@pagebreak')) { out.push(new d.Paragraph({ children: [new d.PageBreak()] })); i++; continue }
    if (l.startsWith('@code ')) { const [p, s, m] = l.slice(6).split(' :: '); out.push(...codeFrom(p.trim(), s.trim(), m ? +m : 70)); i++; continue }
    if (l.startsWith('@lines ')) {
      const [p, a, b] = l.slice(7).split(' :: ')
      out.push(...codeBlock(readSource(p.trim()).slice(+a - 1, +b), `${p.trim().split('/').pop()}  (${p.trim()}, lines ${a}-${b})`)); i++; continue
    }
    if (l.startsWith('@tables')) { out.push(...dbTables()); i++; continue }
    if (l.startsWith('@endpoints')) { out.push(...endpoints()); i++; continue }
    if (l.startsWith('@commits')) { out.push(...commits()); i++; continue }
    const img = l.match(/^!\[(.*)\]\((.*?)(?:\s+(\d(?:\.\d+)?))?\)$/)
    if (img) { out.push(...image(img[2], img[1], img[3] ? +img[3] : 6.2)); i++; continue }
    const h = l.match(/^(#{1,3}) (.*)/)
    if (h) {
      const level = h[1].length
      if (level === 1) out.push(new d.Paragraph({ children: [new d.PageBreak()] }))
      out.push(new d.Paragraph({ text: h[2], heading: [d.HeadingLevel.HEADING_1, d.HeadingLevel.HEADING_2, d.HeadingLevel.HEADING_3][level - 1], keepNext: true }))
      i++; continue
    }
    if (l.startsWith('|')) {
      const rows = []
      while (i < lines.length && lines[i].startsWith('|')) {
        if (!/^\|[\s:-]+\|/.test(lines[i]) || /[a-z0-9]/i.test(lines[i])) rows.push(lines[i].trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim()))
        i++
      }
      out.push(table(rows, nextWidths || widthsFor(rows)), new d.Paragraph({ spacing: { after: 120 } }))
      nextWidths = null
      continue
    }
    if (l.startsWith('>> ')) { out.push(box(l.slice(3), 'FFF7ED', 'Real-life example:', 'C2410C')); i++; continue }
    if (l.startsWith('> ')) { out.push(box(l.slice(2), 'F0FDFA', 'Note:', BRAND)); i++; continue }
    if (/^- /.test(l)) {
      while (i < lines.length && /^ {0,2}- /.test(lines[i])) {
        const lvl = lines[i].startsWith('  ') ? 1 : 0
        out.push(new d.Paragraph({ children: runs(lines[i].replace(/^ *- /, '')), numbering: { reference: 'bullets', level: lvl }, spacing: { after: 60, line: 276 } }))
        i++
      }
      out.push(new d.Paragraph({ spacing: { after: 60 } }))
      continue
    }
    if (/^\d+\. /.test(l)) {
      listNo++
      while (i < lines.length && /^\d+\. /.test(lines[i])) {
        // the number written in the text is kept, so a list interrupted by a picture continues 4., 5. ...
        const num = lines[i].match(/^(\d+)\. /)[1]
        out.push(new d.Paragraph({ children: [new d.TextRun({ text: num + '.	' }), ...runs(lines[i].replace(/^\d+\. /, ''))], tabStops: [{ type: d.TabStopType.LEFT, position: 540 }], indent: { left: 540, hanging: 300 }, spacing: { after: 60, line: 276 } }))
        i++
      }
      out.push(new d.Paragraph({ spacing: { after: 60 } }))
      continue
    }
    // normal paragraph: join following plain lines
    const buf = [l]
    i++
    while (i < lines.length && lines[i].trim() && !/^(#|```|@|>|\||- |\d+\. |!\[)/.test(lines[i])) buf.push(lines[i++])
    out.push(para(buf.join(' ')))
  }
}

// ---------- document ----------
function titlePage(meta) {
  const big = (t, size, color, bold = true, after = 200) => new d.Paragraph({ children: [new d.TextRun({ text: t, size, color, bold, font: 'Arial' })], alignment: d.AlignmentType.LEFT, spacing: { after } })
  const home = fs.readFileSync(SHOTS + (process.env.DOC_COVER || 'roles.png'))
  return [
    new d.Paragraph({ spacing: { before: 1200 } }),
    big('TriVoKo', 88, NIGHT, true, 120),
    big('Multi-seller E-commerce Marketplace', 36, 'EA580C', false, 360),
    big(DOC_TITLE, 30, INK, true, 120),
    big(DOC_SUB, 22, MUTED, false, 480),
    new d.Paragraph({ children: [new d.ImageRun({ type: 'png', data: home, transformation: { width: 600, height: Math.round(600 * home.readUInt32BE(20) / home.readUInt32BE(16)) }, altText: { title: 'TriVoKo roles', description: 'Who uses TriVoKo', name: 'roles' } })], spacing: { after: 480 } }),
    big(meta.author, 24, INK, true, 60),
    big(meta.line2, 22, MUTED, false, 60),
    big(meta.line3, 22, MUTED, false, 60),
  ]
}

async function main() {
  const files = fs.readdirSync(path.join(__dirname, CONTENT)).filter((f) => f.endsWith('.md')).sort()
  const body = []
  for (const f of files) parse(fs.readFileSync(path.join(__dirname, CONTENT, f), 'utf8'), body)

  const doc = new d.Document({
    creator: 'Madhavan Suresh', title: 'TriVoKo - ' + DOC_TITLE, description: 'TriVoKo project plan',
    styles: {
      default: { document: { run: { font: FONT, size: 22, color: INK } } },
      paragraphStyles: [
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 36, bold: true, font: 'Arial', color: NIGHT }, paragraph: { spacing: { before: 120, after: 240 }, outlineLevel: 0 } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 28, bold: true, font: 'Arial', color: BRAND }, paragraph: { spacing: { before: 320, after: 140 }, outlineLevel: 1 } },
        { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 23, bold: true, font: 'Arial', color: NIGHT }, paragraph: { spacing: { before: 240, after: 100 }, outlineLevel: 2 } },
      ],
    },
    numbering: {
      config: [
        { reference: 'bullets', levels: [
          { level: 0, format: d.LevelFormat.BULLET, text: '•', alignment: d.AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } },
          { level: 1, format: d.LevelFormat.BULLET, text: '◦', alignment: d.AlignmentType.LEFT, style: { paragraph: { indent: { left: 1080, hanging: 270 } } } },
        ] },
        { reference: 'numbers', levels: [{ level: 0, format: d.LevelFormat.DECIMAL, text: '%1.', alignment: d.AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 300 } } } }] },
      ],
    },
    sections: [
      { properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
        children: titlePage({ author: 'Madhavan Suresh', line2: 'Java Full Stack main portfolio project  ·  React + Spring Boot + MySQL + Stripe', line3: process.env.DOC_LINE3 || 'Version 1.0  ·  1 October 2026  ·  planning document (before Phase 0)' }) },
      {
        properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1300, left: 1440 }, pageNumbers: { start: 1 } } },
        headers: { default: new d.Header({ children: [new d.Paragraph({ children: [new d.TextRun({ text: 'TriVoKo - ' + DOC_TITLE, size: 17, color: MUTED })], alignment: d.AlignmentType.RIGHT })] }) },
        footers: { default: new d.Footer({ children: [new d.Paragraph({ children: [new d.TextRun({ children: ['Page ', d.PageNumber.CURRENT, ' of ', d.PageNumber.TOTAL_PAGES], size: 17, color: MUTED })], alignment: d.AlignmentType.CENTER })] }) },
        children: [
          new d.Paragraph({ children: [new d.TextRun({ text: 'Contents', bold: true, size: 36, font: 'Arial', color: NIGHT })], spacing: { after: 240 } }),
          new d.TableOfContents('Contents', { hyperlink: true, headingStyleRange: '1-2' }),
          ...body,
        ],
      },
    ],
  })
  fs.writeFileSync(OUT, await d.Packer.toBuffer(doc))
  console.log('written', OUT, body.length, 'blocks')
}
main().catch((e) => { console.error(e); process.exit(1) })
