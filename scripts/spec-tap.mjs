import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, rmSync, statSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const BACKEND = join(ROOT, 'backend');
const REPORTS = join(BACKEND, 'target', 'surefire-reports');
const E2E = join(ROOT, 'e2e');
const E2E_REPORTS = join(E2E, 'results');
const ENV_FILE = join(ROOT, '.env');
const MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-21';
const DOCKER_SOCKET = '/var/run/docker.sock';

const comentar = (linha) => console.log(`# ${linha}`);

function temJdk() {
  if (process.env.JAVA_HOME) {
    const javac = join(process.env.JAVA_HOME, 'bin', 'javac');
    if (existsSync(javac)) return true;
  }
  return spawnSync('javac', ['-version'], { stdio: 'ignore' }).status === 0;
}

function comandoLocal() {
  return {
    cmd: join(BACKEND, 'mvnw'),
    args: ['-B', 'test'],
    cwd: BACKEND,
    descricao: 'mvnw local (JDK encontrado)',
  };
}

function comandoContainer() {
  const args = ['run', '--rm', '--user', `${process.getuid()}:${process.getgid()}`];

  if (existsSync(DOCKER_SOCKET)) {
    args.push('--group-add', String(statSync(DOCKER_SOCKET).gid));
    args.push('-v', `${DOCKER_SOCKET}:${DOCKER_SOCKET}`);
    args.push('-e', `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=${DOCKER_SOCKET}`);
  }

  args.push(
    '--network', 'host',
    '-e', 'HOME=/tmp',
    '-v', `${BACKEND}:/app:Z`,
    '-v', `${join(homedir(), '.m2')}:/tmp/.m2:Z`,
    '-w', '/app',
    MAVEN_IMAGE,
    'mvn', '-B', '-Dmaven.repo.local=/tmp/.m2/repository', 'test'
  );

  return { cmd: 'docker', args, cwd: ROOT, descricao: `build em ${MAVEN_IMAGE} (sem JDK local)` };
}

const RE_TESTCASE = /<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g;
const RE_ATRIBUTO = /([\w:.-]+)\s*=\s*"([^"]*)"/g;

function desescapar(texto) {
  return texto
    .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(Number(d)))
    .replace(/&#x([0-9a-fA-F]+);/g, (_, h) => String.fromCodePoint(parseInt(h, 16)))
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

function atributos(bruto) {
  const mapa = {};
  for (const [, chave, valor] of bruto.matchAll(RE_ATRIBUTO)) mapa[chave] = desescapar(valor);
  return mapa;
}

function lerRelatorios(dir) {
  if (!existsSync(dir)) return [];
  const casos = [];

  for (const arquivo of readdirSync(dir).filter((f) => f.startsWith('TEST-') && f.endsWith('.xml'))) {
    const xml = readFileSync(join(dir, arquivo), 'utf-8');
    for (const [, bruto, corpo = ''] of xml.matchAll(RE_TESTCASE)) {
      const attr = atributos(bruto);
      const falhou = /<(failure|error)\b/.test(corpo);
      const pulado = /<skipped\b/.test(corpo);
      casos.push({
        classe: attr.classname || '?',
        titulo: attr.name || '(sem nome)',
        status: falhou ? 'fail' : pulado ? 'skip' : 'pass',
      });
    }
  }

  return casos.sort((a, b) => `${a.classe}${a.titulo}`.localeCompare(`${b.classe}${b.titulo}`));
}

function imprimirCauda(saida, linhas = 40) {
  for (const linha of saida.trimEnd().split(/\r?\n/).slice(-linhas)) comentar(linha);
}

function e2eDesligado() {
  const valor = (process.env.SPEC_TAP_E2E ?? '').trim().toLowerCase();
  return ['0', 'off', 'false', 'no', 'nao'].includes(valor);
}

function portaDoFrontend() {
  if (process.env.FRONTEND_PORT) return process.env.FRONTEND_PORT;
  if (!existsSync(ENV_FILE)) return '4200';
  const linha = readFileSync(ENV_FILE, 'utf-8')
    .split(/\r?\n/)
    .find((l) => /^\s*FRONTEND_PORT\s*=/.test(l));
  return linha?.split('=')[1]?.trim() || '4200';
}

function compose(...args) {
  return spawnSync('docker', ['compose', ...args], {
    cwd: ROOT,
    encoding: 'utf-8',
    maxBuffer: 64 * 1024 * 1024,
  });
}

function rodarE2e() {
  if (existsSync(E2E_REPORTS)) rmSync(E2E_REPORTS, { recursive: true, force: true });

  const inicio = Date.now();
  const subida = compose('up', '--build', '--detach', '--wait', '--wait-timeout', '300');
  if (subida.status !== 0) {
    comentar(`compose não subiu (código ${subida.status})`);
    imprimirCauda(`${subida.stdout || ''}\n${subida.stderr || ''}`);
    compose('down');
    return { casos: [], ok: false };
  }
  comentar(`compose de pé em ${Math.round((Date.now() - inicio) / 1000)}s`);

  const e2e = spawnSync('npx', ['playwright', 'test'], {
    cwd: E2E,
    encoding: 'utf-8',
    maxBuffer: 64 * 1024 * 1024,
    env: { ...process.env, FRONTEND_PORT: portaDoFrontend() },
  });
  comentar(`E2E terminou com código ${e2e.status}`);

  const casos = lerRelatorios(E2E_REPORTS);
  if (casos.length === 0 || e2e.status !== 0) {
    imprimirCauda(`${e2e.stdout || ''}\n${e2e.stderr || ''}`);
  }

  if (compose('down').status !== 0) {
    comentar('`docker compose down` falhou: containers podem ter ficado de pé');
  }

  return { casos, ok: e2e.status === 0 && casos.length > 0 };
}

const plano = temJdk() ? comandoLocal() : comandoContainer();
comentar(plano.descricao);

if (existsSync(REPORTS)) rmSync(REPORTS, { recursive: true, force: true });

const inicio = Date.now();
const build = spawnSync(plano.cmd, plano.args, {
  cwd: plano.cwd,
  encoding: 'utf-8',
  maxBuffer: 256 * 1024 * 1024,
});
const saida = `${build.stdout || ''}\n${build.stderr || ''}`;
comentar(`build terminou em ${Math.round((Date.now() - inicio) / 1000)}s com código ${build.status}`);

const casosBackend = lerRelatorios(REPORTS);

let casosE2e = [];
let e2eOk = true;
if (e2eDesligado()) {
  comentar('E2E desligado por SPEC_TAP_E2E: sem casos de tela no TAP, os critérios de tela ficam sem prova');
} else {
  ({ casos: casosE2e, ok: e2eOk } = rodarE2e());
}

const casos = [...casosBackend, ...casosE2e];

console.log('TAP version 13');
console.log(`1..${casos.length}`);

casos.forEach((caso, i) => {
  const numero = i + 1;
  if (caso.status === 'skip') {
    // Pulado não é prova: o motor trata "# SKIP" como ausência de veredito.
    console.log(`ok ${numero} - ${caso.titulo} # SKIP`);
  } else {
    console.log(`${caso.status === 'fail' ? 'not ok' : 'ok'} ${numero} - ${caso.titulo}`);
  }
  comentar(`  ${caso.classe}`);
});

const falhas = casos.filter((c) => c.status === 'fail').length;
const pulados = casos.filter((c) => c.status === 'skip').length;
comentar(`${casos.length} teste(s) · ${casos.length - falhas - pulados} ok · ${falhas} falha(s) · ${pulados} pulado(s)`);

const falhasBackend = casosBackend.filter((c) => c.status === 'fail').length;
if (casosBackend.length === 0) {
  comentar('nenhum relatório do Surefire: o build não chegou a rodar testes');
  imprimirCauda(saida);
} else if (falhasBackend > 0) {
  imprimirCauda(saida);
}

process.exit(casosBackend.length > 0 && falhas === 0 && build.status === 0 && e2eOk ? 0 : 1);
