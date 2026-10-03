// Writes the TriVoKo Phase Plan and the 13 phase checklist documents from phases.js.
//   node phase-docs.js <stamp>      e.g. node phase-docs.js 2026-10-03_0940
// Output:
//   00_Project_Documents/TriVoKo_Phase_Plan_<stamp>.docx   (via gen.js, from content-phase/*.md written here)
//   Phase_N_Name/PhaseN_Name_<stamp>.docx                  (one checklist per phase)
// Then run finish.ps1 on each .docx for the table of contents, page numbers and the PDF.
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')
const d = require('docx')
const phases = require('./phases')

const ROOT = 'D:/Java_FullStack_Journey/03_TriVoKo_Main_Project/'
const STAMP = process.argv[2]
if (!STAMP) throw new Error('usage: node phase-docs.js <YYYY-MM-DD_HHMM>')
const DATE_TEXT = new Date(STAMP.slice(0, 10)).toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })

const WHO = { C: 'Claude', M: '★ Madhavan', Y: 'You', T: 'Together' }
const SHORT = ['Project ready to build', 'Products exist', 'Safe login for 3 roles', 'Customer can shop (no payment yet)', 'Real orders, paid once, split per seller', 'Sellers run their shop', 'Admin controls the marketplace', 'Standout #1 - never oversells', 'Standout #2 - exact partial refunds', 'Standout #3 - alerts without spam', 'Standout #4 - typo-tolerant search', 'Portfolio quality', 'Live and shown']
const GROUP = (n) => (n <= 8 ? 'Core' : n <= 10 ? 'Shine' : 'Always')

// ---------- 1. Phase Plan book (markdown for gen.js) ----------
function planMarkdown() {
  const files = {}
  const overview = phases.map((p) => `| **${p.n}** | ${p.name} | ${SHORT[p.n]} | ${p.time} | ${GROUP(p.n)} |`).join('\n')
  files['00-how.md'] = `# How to use this plan

This is the **step-by-step plan** for building TriVoKo. The **Master Plan** (1 October 2026) explains *what* we build and *why*. This document says *in which order* and *how you know a phase is finished*.

>> A building plan has two papers: the architect's drawing (what the house looks like) and the contractor's schedule (foundation first, then walls, then roof). The Master Plan is the drawing. This Phase Plan is the schedule.

![The 13 phases](roadmap.png)

## 1.1 The 13 phases

@widths 800 2500 3626 1100 1000
| Phase | Name | Goal | Time | Group |
${overview}

Total: about **15-16 weeks** at 2 hours per day. **Core** (Phases 0-8) is a complete marketplace with the two strongest standouts. **Shine** (9-10) can be made smaller if time is short. **Always** (11-12) is never skipped - a portfolio project that is not tested and not live is not finished.

## 1.2 Who does each step

Every step has a **Who** column:

@widths 1800 7226
| Who | Meaning |
| **Claude** | Claude builds it and explains it line by line. You read, run it, and ask for changes. |
| **★ Madhavan** | **You type the key logic.** Claude explains the idea first with a picture, gives you the method signature and the test, then reviews what you wrote. These are the parts interviewers will ask about. |
| **You** | Something only you can do: create an account (Stripe, Cloudinary, GitHub), approve a sketch, choose hosting. |
| **Together** | We do it side by side - for example the real Stripe test or the demo video. |

The three ★ parts are: the **order split** (Phase 4), the **flash-sale claim** (Phase 7) and the **return state machine** (Phase 8). For each one you also write at least one test.

## 1.3 The rules for every phase

1. Open the phase folder (\`Phase_N_Name\`) and its checklist document. Tick each step when it is done.
2. Do not start the next phase until **every "Done when" check passes** - shown working in the browser, not only in tests.
3. Every phase ends with the **finish routine**:

- All tests pass on the laptop and **CI is green** on GitHub
- Code committed and pushed with clear messages
- The "Done when" checks shown working
- **Explain it back**: you answer the questions at the end of the phase in your own words (spoken or written in "My notes")
- Session summary + SESSION_LOG entry written

> A phase may be split over many days. The checklist document is the place where you write the start date, the finish date and your own notes.

## 1.4 Where things live

@widths 3400 5626
| Folder | What is inside |
| \`00_Project_Documents\\\` | Master Plan, this Phase Plan, guides, session history |
| \`Phase_0_Setup\\\` ... \`Phase_12_Launch\\\` | One checklist document per phase (tick boxes + notes) |
| \`backend\\\` | Spring Boot code - one project that grows every phase |
| \`frontend\\\` | React code - one project that grows every phase |
| \`load-tests\\\`, \`e2e\\\` | k6 scripts (Phase 7), Playwright tests (Phase 11) |
| \`docs\\\`, \`tools\\\` | Sketches, decisions, README diagrams; document generators, demo scripts |
`
  for (const p of phases) {
    const steps = p.steps.map((s, i) => `| ${i + 1} | ${s[1].replace(/\|/g, '/')} | ${WHO[s[0]]} |`).join('\n')
    files[`${String(p.n + 1).padStart(2, '0')}-phase${p.n}.md`] = `# Phase ${p.n} - ${p.name}

**Time:** ${p.time}  ·  **Group:** ${GROUP(p.n)}  ·  **Folder:** \`${p.folder}\\\`

**Goal:** ${p.goal}

>> ${p.example}

## Steps

@widths 600 6926 1500
| # | Step | Who |
${steps}

## Output of this phase

${p.output.map((o) => '- ' + o).join('\n')}

## Done when

${p.done.map((o) => '- ' + o).join('\n')}

## Explain it back

${p.explain.map((o) => '- ' + o).join('\n')}

**Skills you practise:** ${p.skills}

**Where your work goes:** ${p.where}
`
  }
  return files
}

const dir = path.join(__dirname, 'content-phase')
fs.rmSync(dir, { recursive: true, force: true })
fs.mkdirSync(dir)
for (const [f, text] of Object.entries(planMarkdown())) fs.writeFileSync(path.join(dir, f), text)
const planOut = ROOT + `00_Project_Documents/TriVoKo_Phase_Plan_${STAMP}.docx`
execFileSync('node', [path.join(__dirname, 'gen.js'), planOut, 'content-phase', 'Phase Plan',
  'Step-by-step checklists for the 13 phases: who does each step, what you get, how you know it is done, and the questions to explain it back'],
  { stdio: 'inherit', env: { ...process.env, DOC_COVER: 'roadmap.png', DOC_LINE3: `Version 1.0  ·  ${DATE_TEXT}  ·  companion to the Master Plan` } })

// ---------- 2. One checklist document per phase ----------
const FONT = 'Calibri', INK = '1F2937', NIGHT = '134E4A', BRAND = '0F766E', MUTED = '6B7280', SAFFRON = 'C2410C'
const W = 9026
const border = { style: d.BorderStyle.SINGLE, size: 4, color: 'D1D5DB' }
const borders = { top: border, bottom: border, left: border, right: border }

function runs(text, base = {}) {
  const out = []
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g
  let last = 0, m
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(new d.TextRun({ text: text.slice(last, m.index), ...base }))
    const t = m[0]
    if (t.startsWith('**')) out.push(new d.TextRun({ text: t.slice(2, -2), bold: true, ...base }))
    else out.push(new d.TextRun({ text: t.slice(1, -1), font: 'Consolas', size: 19, color: 'B4235A', ...base }))
    last = m.index + t.length
  }
  if (last < text.length) out.push(new d.TextRun({ text: text.slice(last), ...base }))
  return out
}
const para = (text, opts = {}) => new d.Paragraph({ children: runs(text), spacing: { after: 120, line: 288 }, ...opts })
const heading = (text) => new d.Paragraph({ text, heading: d.HeadingLevel.HEADING_2, keepNext: true })

function table(rows, widths, center = []) {
  return new d.Table({
    width: { size: W, type: d.WidthType.DXA }, columnWidths: widths,
    rows: rows.map((r, ri) => new d.TableRow({
      tableHeader: ri === 0, cantSplit: true,
      children: r.map((c, ci) => new d.TableCell({
        width: { size: widths[ci], type: d.WidthType.DXA }, borders, verticalAlign: d.VerticalAlign.CENTER,
        shading: { type: d.ShadingType.CLEAR, color: 'auto', fill: ri === 0 ? NIGHT : (ri % 2 ? 'FFFFFF' : 'F0FDFA') },
        margins: { top: 70, bottom: 70, left: 100, right: 100 },
        children: [new d.Paragraph({
          children: runs(c, ri === 0 ? { bold: true, color: 'FFFFFF', size: 19 } : { size: c === '☐' ? 28 : 19, color: c.startsWith('★') ? SAFFRON : undefined, bold: c.startsWith('★') || undefined }),
          alignment: center.includes(ci) ? d.AlignmentType.CENTER : d.AlignmentType.LEFT, spacing: { after: 0 },
        })],
      })),
    })),
  })
}

function box(text) {
  return new d.Paragraph({
    children: [new d.TextRun({ text: 'Real-life example:  ', bold: true, color: SAFFRON }), ...runs(text)],
    shading: { type: d.ShadingType.CLEAR, color: 'auto', fill: 'FFF7ED' },
    border: { left: { style: d.BorderStyle.SINGLE, size: 18, color: SAFFRON, space: 8 } },
    spacing: { before: 120, after: 200, line: 288 }, indent: { left: 200, right: 120 },
  })
}

const bullets = (list) => list.map((t) => new d.Paragraph({ children: runs(t), numbering: { reference: 'bullets', level: 0 }, spacing: { after: 60, line: 276 } }))
const gap = () => new d.Paragraph({ spacing: { after: 120 } })

async function phaseDoc(p) {
  const title = `Phase ${p.n} - ${p.name}`
  const children = [
    new d.Paragraph({ children: [new d.TextRun({ text: 'TriVoKo', bold: true, size: 26, color: SAFFRON, font: 'Arial' })], spacing: { after: 40 } }),
    new d.Paragraph({ text: title, heading: d.HeadingLevel.HEADING_1 }),
    table([['Time', 'Start date', 'Finish date', 'Status'], [p.time, '', '', 'Not started']], [2256, 2256, 2256, 2258]),
    gap(),
    para(`**Goal:** ${p.goal}`),
    box(p.example),
    heading('Step checklist'),
    para('★ = you type the key logic (Claude explains first, then reviews). "You" = only you can do it.', { spacing: { after: 100 }, children: [new d.TextRun({ text: '★ = you type the key logic (Claude explains first, then reviews). "You" = only you can do it.', italics: true, size: 18, color: MUTED })] }),
    table([['#', 'Step', 'Who', 'Done'], ...p.steps.map((s, i) => [String(i + 1), s[1], WHO[s[0]], '☐'])], [500, 6026, 1600, 900], [0, 3]),
    heading('Output of this phase'),
    ...bullets(p.output),
    heading('Done when (check before moving on)'),
    table([['Check', 'Passed'], ...p.done.map((c) => [c, '☐'])], [8026, 1000], [1]),
    heading('Explain it back (in your own words)'),
    table([['Question', 'Done'], ...p.explain.map((c) => [c, '☐'])], [8026, 1000], [1]),
    heading('Finish routine'),
    table([['Check', 'Done'], ...['All tests pass and CI is green', 'Committed and pushed', 'Done-when checks shown working in the browser', 'Session summary + SESSION_LOG written'].map((c) => [c, '☐'])], [8026, 1000], [1]),
    para(`**Skills you practise:** ${p.skills}`, { spacing: { before: 240, after: 120 } }),
    para(`**Where your work goes:** ${p.where}`),
    heading('My notes'),
    ...Array.from({ length: 8 }, () => new d.Paragraph({ border: { bottom: { style: d.BorderStyle.SINGLE, size: 4, color: 'D1D5DB', space: 1 } }, spacing: { before: 280 } })),
  ]
  const doc = new d.Document({
    creator: 'Madhavan Suresh', title: `TriVoKo - ${title}`,
    styles: {
      default: { document: { run: { font: FONT, size: 22, color: INK } } },
      paragraphStyles: [
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', run: { size: 40, bold: true, font: 'Arial', color: NIGHT }, paragraph: { spacing: { after: 240 }, outlineLevel: 0 } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', run: { size: 26, bold: true, font: 'Arial', color: BRAND }, paragraph: { spacing: { before: 300, after: 120 }, outlineLevel: 1 } },
      ],
    },
    numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: d.LevelFormat.BULLET, text: '•', alignment: d.AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
    sections: [{
      properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1300, right: 1440, bottom: 1300, left: 1440 } } },
      headers: { default: new d.Header({ children: [new d.Paragraph({ children: [new d.TextRun({ text: `TriVoKo - ${title}  ·  ${DATE_TEXT}`, size: 17, color: MUTED })], alignment: d.AlignmentType.RIGHT })] }) },
      footers: { default: new d.Footer({ children: [new d.Paragraph({ children: [new d.TextRun({ children: ['Page ', d.PageNumber.CURRENT, ' of ', d.PageNumber.TOTAL_PAGES], size: 17, color: MUTED })], alignment: d.AlignmentType.CENTER })] }) },
      children,
    }],
  })
  const folder = ROOT + p.folder + '/'
  fs.mkdirSync(folder, { recursive: true })
  const file = folder + `Phase${p.n}_${p.folder.replace(/^Phase_\d+_/, '')}_${STAMP}.docx`
  fs.writeFileSync(file, await d.Packer.toBuffer(doc))
  console.log('written', file)
}

;(async () => { for (const p of phases) await phaseDoc(p) })()
