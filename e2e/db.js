/**
 * Starts / stops the E2E test's own MySQL in Docker (port 3308), so the developer's data on 3307 is never touched.
 *   node db.js up    -> fresh, empty database (Flyway fills it when the backend starts), waits until it answers
 *   node db.js down  -> removes it (--rm: nothing is left behind)
 * CI does not use this: the workflow starts the same database as a "services:" container.
 */
import { execFileSync } from 'node:child_process'

import { DB } from './settings.js'

const NAME = 'trivoko-e2e-mysql'

const docker = (...args) => execFileSync('docker', args, { stdio: 'pipe' }).toString().trim()

function up() {
  try {
    docker('rm', '-f', NAME) // an old one from an interrupted run
  } catch {
    // none - fine
  }
  docker('run', '-d', '--rm', '--name', NAME, '-p', `${DB.port}:3306`,
    '-e', `MYSQL_DATABASE=${DB.name}`, '-e', `MYSQL_USER=${DB.user}`, '-e', `MYSQL_PASSWORD=${DB.password}`,
    '-e', 'MYSQL_ROOT_PASSWORD=e2e-root-only', '-e', 'TZ=Asia/Kolkata', 'mysql:8.4')
  // MySQL needs ~20 s on its first start; ping as the test user until it answers
  for (let i = 0; i < 90; i++) {
    try {
      docker('exec', NAME, 'mysql', '-h127.0.0.1', `-u${DB.user}`, `-p${DB.password}`, '-e', 'SELECT 1', DB.name)
      console.log(`E2E database ready on port ${DB.port}`)
      return
    } catch {
      execFileSync(process.execPath, ['-e', 'setTimeout(() => {}, 1000)'])
    }
  }
  throw new Error('The E2E database did not start in 90 seconds')
}

function down() {
  try {
    docker('rm', '-f', NAME)
  } catch {
    // already gone
  }
}

if (process.argv[2] === 'up') up()
else if (process.argv[2] === 'down') down()
else console.log('usage: node db.js up | down')
