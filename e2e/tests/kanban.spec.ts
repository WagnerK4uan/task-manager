import { randomUUID } from 'node:crypto';
import { APIRequestContext, Locator, Page, expect, test } from '@playwright/test';

type NovaTarefa = {
  title: string;
  description?: string | null;
  status?: 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA';
  priority?: 'BAIXA' | 'MEDIA' | 'ALTA';
  dueDate?: string | null;
};

const criadas: number[] = [];

test.afterEach(async ({ request }) => {
  for (const id of criadas.splice(0)) {
    await request.delete(`/api/tasks/${id}`);
  }
});

async function criar(request: APIRequestContext, tarefa: NovaTarefa): Promise<number> {
  const resposta = await request.post('/api/tasks', {
    data: {
      title: tarefa.title,
      description: tarefa.description ?? null,
      status: tarefa.status ?? 'PENDENTE',
      priority: tarefa.priority ?? 'MEDIA',
      dueDate: tarefa.dueDate ?? null,
    },
  });
  expect(resposta.status()).toBe(201);

  const id = (await resposta.json()).id as number;
  criadas.push(id);
  return id;
}

function coluna(page: Page, status: string): Locator {
  return page.getByTestId(`coluna-${status}`);
}

function card(page: Page, id: number): Locator {
  return page.getByTestId(`card-${id}`);
}

function abrirQuadro(page: Page) {
  return page.goto('/quadro');
}

test('@spec:AC-048 cada tarefa aparece na coluna da sua situação', async ({ page, request }) => {
  const marca = randomUUID().slice(0, 8);

  const idPendente = await criar(request, {
    title: `${marca} pendente`,
    priority: 'ALTA',
    dueDate: '2026-10-15',
  });
  const idAndamento = await criar(request, {
    title: `${marca} andamento`,
    status: 'EM_ANDAMENTO',
  });
  const idConcluida = await criar(request, {
    title: `${marca} concluída`,
    status: 'CONCLUIDA',
    priority: 'BAIXA',
  });

  await abrirQuadro(page);

  await expect(coluna(page, 'PENDENTE').getByTestId(`card-${idPendente}`)).toBeVisible();
  await expect(coluna(page, 'EM_ANDAMENTO').getByTestId(`card-${idAndamento}`)).toBeVisible();
  await expect(coluna(page, 'CONCLUIDA').getByTestId(`card-${idConcluida}`)).toBeVisible();

  const primeiro = card(page, idPendente);
  await expect(primeiro).toContainText(`${marca} pendente`);
  await expect(primeiro.getByTestId('prioridade')).toHaveText('Alta');
  await expect(primeiro.getByTestId('prazo')).toHaveText('15/10/2026');

  for (const situacao of ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA']) {
    const alvo = coluna(page, situacao);
    const cards = await alvo.locator('article').count();
    await expect(alvo.getByTestId('contador')).toHaveText(String(cards));
  }
});

test('@spec:AC-049 quadro sem tarefa nenhuma convida a criar', async ({ page }) => {
  await page.route('**/api/tasks', async (rota) => {
    if (rota.request().method() !== 'GET') return rota.continue();
    return rota.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });

  await abrirQuadro(page);

  for (const situacao of ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA']) {
    await expect(coluna(page, situacao).getByTestId('coluna-vazia')).toBeVisible();
    await expect(coluna(page, situacao).getByTestId('criar')).toBeVisible();
  }

  await expect(page.getByTestId('erro')).toHaveCount(0);
});

test('@spec:AC-050 falha da API aparece como mensagem, não como tela branca', async ({ page }) => {
  await page.route('**/api/tasks', async (rota) => {
    if (rota.request().method() !== 'GET') return rota.continue();
    return rota.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({
        timestamp: '2026-09-28T00:00:00Z',
        status: 500,
        error: 'INTERNAL_ERROR',
        message: 'Internal error',
        path: '/api/tasks',
      }),
    });
  });

  await abrirQuadro(page);

  const alerta = page.getByTestId('erro');
  await expect(alerta).toBeVisible();
  await expect(alerta).toContainText('Não foi possível carregar o quadro.');
  await expect(alerta).not.toContainText('500');
  await expect(alerta).not.toContainText('INTERNAL_ERROR');
  await expect(coluna(page, 'PENDENTE').getByTestId('criar')).toBeVisible();
});

test('@spec:AC-051 arrastar o card para outra coluna muda a situação no servidor', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} arrastar`;
  const id = await criar(request, { title: titulo });

  await abrirQuadro(page);
  await expect(coluna(page, 'PENDENTE').getByTestId(`card-${id}`)).toBeVisible();

  await card(page, id).scrollIntoViewIfNeeded();

  const origem = await card(page, id).boundingBox();
  const ancora = await coluna(page, 'EM_ANDAMENTO').getByTestId('criar').boundingBox();
  expect(origem).not.toBeNull();
  expect(ancora).not.toBeNull();

  const alvoX = ancora!.x + ancora!.width / 2;
  const alvoY = ancora!.y + ancora!.height + 40;

  await page.mouse.move(origem!.x + origem!.width / 2, origem!.y + 20);
  await page.mouse.down();
  await page.mouse.move(origem!.x + origem!.width / 2 + 15, origem!.y + 25, { steps: 5 });
  await page.mouse.move(alvoX, alvoY, { steps: 20 });
  await page.mouse.move(alvoX, alvoY + 5, { steps: 5 });
  await page.mouse.up();

  await expect(coluna(page, 'EM_ANDAMENTO').getByTestId(`card-${id}`)).toBeVisible();

  await page.reload();
  await expect(coluna(page, 'EM_ANDAMENTO').getByTestId(`card-${id}`)).toBeVisible();
});

test('@spec:AC-052 mover pelo botão do card faz o mesmo, sem mouse', async ({ page, request }) => {
  const titulo = `${randomUUID().slice(0, 8)} mover por botao`;
  const id = await criar(request, { title: titulo });

  await abrirQuadro(page);
  await expect(coluna(page, 'PENDENTE').getByTestId(`card-${id}`)).toBeVisible();

  const botao = page.getByRole('button', { name: `Mover ${titulo} para Em andamento` });
  await botao.focus();
  await page.keyboard.press('Enter');

  await expect(coluna(page, 'EM_ANDAMENTO').getByTestId(`card-${id}`)).toBeVisible();

  await page.reload();
  await expect(coluna(page, 'EM_ANDAMENTO').getByTestId(`card-${id}`)).toBeVisible();
});

test('@spec:AC-053 o card abre com a descrição inteira e os campos preenchidos', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} abrir card`;
  const descricao =
    'Primeira linha do contexto desta tarefa, longa o bastante para não caber no card do quadro e precisar do painel para ser lida inteira.';
  await criar(request, {
    title: titulo,
    description: descricao,
    status: 'EM_ANDAMENTO',
    priority: 'ALTA',
    dueDate: '2026-11-20',
  });

  await abrirQuadro(page);
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();

  const painel = page.getByTestId('painel');
  await expect(painel).toBeVisible();
  await expect(painel.getByTestId('campo-titulo')).toHaveValue(titulo);
  await expect(painel.getByTestId('campo-descricao')).toHaveValue(descricao);
  await expect(painel.getByTestId('campo-situacao')).toHaveValue('EM_ANDAMENTO');
  await expect(painel.getByTestId('campo-prioridade')).toHaveValue('ALTA');
  await expect(painel.getByTestId('campo-prazo')).toHaveValue('2026-11-20');
});

test('@spec:AC-054 editar e salvar reflete no quadro e sobrevive a recarregar', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} editar`;
  const id = await criar(request, { title: titulo, description: 'descrição curta' });
  const novaDescricao = 'Descrição reescrita pelo painel, com bem mais contexto do que a anterior.';

  await abrirQuadro(page);
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();

  await page.getByTestId('campo-descricao').fill(novaDescricao);
  await page.getByTestId('campo-prioridade').selectOption('ALTA');
  await page.getByTestId('salvar').click();

  await expect(page.getByTestId('painel')).toHaveCount(0);
  await expect(card(page, id).getByTestId('prioridade')).toHaveText('Alta');

  await page.reload();
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();
  await expect(page.getByTestId('campo-descricao')).toHaveValue(novaDescricao);
});

test('@spec:AC-055 salvar sem título é recusado com o motivo no campo', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} sem titulo`;
  const id = await criar(request, { title: titulo });

  await abrirQuadro(page);
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();

  await page.getByTestId('campo-titulo').fill('');
  await page.getByTestId('salvar').click();

  await expect(page.getByTestId('erro-titulo')).toBeVisible();
  await expect(page.getByTestId('erro-titulo')).toContainText('título');
  await expect(page.getByTestId('painel')).toBeVisible();

  const gravada = await request.get(`/api/tasks/${id}`);
  expect((await gravada.json()).title).toBe(titulo);
});

test('@spec:AC-056 excluir pelo painel pede confirmação antes de remover', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} excluir`;
  const id = await criar(request, { title: titulo });

  await abrirQuadro(page);
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();

  await page.getByTestId('excluir').click();
  await page.getByRole('alertdialog').getByTestId('confirmacao-cancelar').click();

  await expect(page.getByTestId('painel')).toBeVisible();
  expect((await request.get(`/api/tasks/${id}`)).status()).toBe(200);

  await page.getByTestId('excluir').click();
  await page.getByRole('alertdialog').getByTestId('confirmacao-aceitar').click();

  await expect(page.getByTestId('painel')).toHaveCount(0);
  await expect(card(page, id)).toHaveCount(0);
  expect((await request.get(`/api/tasks/${id}`)).status()).toBe(404);
});

test('@spec:AC-057 criar pela coluna nasce naquela situação', async ({ page, request }) => {
  const titulo = `${randomUUID().slice(0, 8)} criada pela coluna`;

  await abrirQuadro(page);
  await page.getByRole('button', { name: 'Nova tarefa em Em andamento' }).click();

  await page.getByTestId('campo-titulo').fill(titulo);
  await page.getByTestId('campo-prioridade').selectOption('ALTA');
  await page.getByTestId('salvar').click();

  await expect(page.getByTestId('painel')).toHaveCount(0);
  await expect(
    coluna(page, 'EM_ANDAMENTO').getByRole('button', { name: `Abrir ${titulo}` }),
  ).toBeVisible();

  const listagem = await request.get('/api/tasks');
  const tarefas = (await listagem.json()) as Array<{ id: number; title: string; status: string }>;
  const gravada = tarefas.find((tarefa) => tarefa.title === titulo);

  expect(gravada?.status).toBe('EM_ANDAMENTO');
  if (gravada) criadas.push(gravada.id);
});
