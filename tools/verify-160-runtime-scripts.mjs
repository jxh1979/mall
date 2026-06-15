import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();

function readRequired(relativePath) {
  const absolutePath = path.join(root, relativePath);
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`${relativePath} is missing`);
  }
  return fs.readFileSync(absolutePath, 'utf8');
}

function requireIncludes(content, needle, label) {
  if (!content.includes(needle)) {
    throw new Error(`${label} missing required marker: ${needle}`);
  }
}

const runtime = readRequired('ops/160/complex-runtime.sh');
const verifyP0 = readRequired('ops/160/complex-verify-p0.sh');

for (const marker of [
  'start_api()',
  'start_web()',
  'stop_api()',
  'stop_web()',
  'status)',
  'up)',
  'down)',
  'api=running',
  'web=running',
]) {
  requireIncludes(runtime, marker, 'complex-runtime.sh');
}

for (const marker of [
  'AUTO_START_RUNTIME',
  'complex-runtime.sh',
  'runtime up',
  'runtime status',
]) {
  requireIncludes(verifyP0, marker, 'complex-verify-p0.sh');
}

if (/pkill\s+-f/.test(runtime)) {
  throw new Error('complex-runtime.sh must not use broad pkill -f process matching');
}

console.log('160 runtime script verification passed.');
