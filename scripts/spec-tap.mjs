import { spawnSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, rmSync, statSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const BACKEND = join(ROOT, 'backend');
const REPORTS = join(BACKEND, 'target', 'surefire-reports');
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

const casos = lerRelatorios(REPORTS);

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

if (casos.length === 0) {
  comentar('nenhum relatório do Surefire: o build não chegou a rodar testes');
  imprimirCauda(saida);
} else if (falhas > 0) {
  imprimirCauda(saida);
}

process.exit(casos.length > 0 && falhas === 0 && build.status === 0 ? 0 : 1);
