import { randomUUID } from 'node:crypto';
import { APIRequestContext, expect, test } from '@playwright/test';

const criadas: number[] = [];

test.afterEach(async ({ request }) => {
  for (const id of criadas.splice(0)) {
    await request.delete(`/api/tasks/${id}`);
  }
});

async function criar(
  request: APIRequestContext,
  title: string,
  status: 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA' = 'PENDENTE',
): Promise<number> {
  const resposta = await request.post('/api/tasks', {
    data: { title, description: null, status, priority: 'MEDIA', dueDate: null },
  });
  expect(resposta.status()).toBe(201);

  const id = (await resposta.json()).id as number;
  criadas.push(id);
  return id;
}

function prefixo(): string {
  return randomUUID().slice(0, 8);
}

test('@spec:AC-065 o modo seleção marca cards em vez de abri-los', async ({ page, request }) => {
  const marca = prefixo();
  const primeira = await criar(request, `${marca} primeira`);
  const segunda = await criar(request, `${marca} segunda`);

  await page.goto('/quadro');
  await expect(page.getByTestId('marcar-card')).toHaveCount(0);

  await page.getByTestId('selecionar').click();

  await expect(page.getByRole('checkbox', { name: `Selecionar ${marca} primeira` })).toBeVisible();
  await expect(page.getByRole('checkbox', { name: `Selecionar ${marca} segunda` })).toBeVisible();

  await page.getByTestId(`card-${primeira}`).getByText(`${marca} primeira`).click();
  await page.getByRole('checkbox', { name: `Selecionar ${marca} segunda` }).check();

  await expect(page.getByTestId(`card-${primeira}`)).toHaveAttribute('data-selecionada', 'true');
  await expect(page.getByTestId(`card-${segunda}`)).toHaveAttribute('data-selecionada', 'true');
  await expect(page.getByTestId('painel')).toHaveCount(0);
  await expect(page.getByTestId('selecionadas')).toHaveText('2 selecionadas');

  await page.getByTestId('cancelar-selecao').click();

  await expect(page.getByTestId('barra-selecao')).toHaveCount(0);
  await expect(page.getByTestId('marcar-card')).toHaveCount(0);

  await page.getByTestId('selecionar').click();
  await expect(page.getByTestId('selecionadas')).toHaveText('0 selecionadas');
  await expect(page.getByTestId(`card-${primeira}`)).toHaveAttribute('data-selecionada', 'false');
});

test('@spec:AC-066 as selecionadas saem juntas, depois de uma confirmação', async ({
  page,
  request,
}) => {
  const marca = prefixo();
  const primeira = await criar(request, `${marca} primeira`);
  const segunda = await criar(request, `${marca} segunda`);
  const mantida = await criar(request, `${marca} mantida`);

  const nativos: string[] = [];
  page.on('dialog', (dialogo) => {
    nativos.push(dialogo.message());
    void dialogo.dismiss();
  });

  await page.goto('/quadro');
  await page.getByTestId('selecionar').click();
  await page.getByRole('checkbox', { name: `Selecionar ${marca} primeira` }).check();
  await page.getByRole('checkbox', { name: `Selecionar ${marca} segunda` }).check();

  await page.getByTestId('excluir-selecionadas').click();

  const confirmacao = page.getByRole('alertdialog');
  await expect(confirmacao).toBeVisible();
  await expect(confirmacao).toContainText('2 tarefas');
  await expect(confirmacao.getByTestId('confirmacao-cancelar')).toBeFocused();

  await page.keyboard.press('Escape');

  await expect(confirmacao).toHaveCount(0);
  await expect(page.getByTestId('selecionadas')).toHaveText('2 selecionadas');
  expect((await request.get(`/api/tasks/${primeira}`)).status()).toBe(200);

  await page.getByTestId('excluir-selecionadas').click();
  await page.getByRole('alertdialog').getByTestId('confirmacao-aceitar').click();

  await expect(page.getByTestId(`card-${primeira}`)).toHaveCount(0);
  await expect(page.getByTestId(`card-${segunda}`)).toHaveCount(0);
  await expect(page.getByTestId(`card-${mantida}`)).toBeVisible();
  await expect(page.getByTestId('barra-selecao')).toHaveCount(0);
  expect((await request.get(`/api/tasks/${primeira}`)).status()).toBe(404);
  expect((await request.get(`/api/tasks/${segunda}`)).status()).toBe(404);
  expect((await request.get(`/api/tasks/${mantida}`)).status()).toBe(200);
  expect(nativos).toEqual([]);
});

test('@spec:AC-067 marcar todas de uma coluna', async ({ page, request }) => {
  const marca = prefixo();
  const pendente = await criar(request, `${marca} pendente`, 'PENDENTE');
  const concluidaA = await criar(request, `${marca} concluída A`, 'CONCLUIDA');
  const concluidaB = await criar(request, `${marca} concluída B`, 'CONCLUIDA');

  await page.goto('/quadro');
  await page.getByTestId('selecionar').click();

  const colunaConcluida = page.getByTestId('coluna-CONCLUIDA');
  const marcarColuna = colunaConcluida.getByTestId('marcar-coluna');

  await expect(marcarColuna).toHaveText('Marcar todas');
  await marcarColuna.click();

  await expect(page.getByTestId(`card-${concluidaA}`)).toHaveAttribute('data-selecionada', 'true');
  await expect(page.getByTestId(`card-${concluidaB}`)).toHaveAttribute('data-selecionada', 'true');
  await expect(page.getByTestId(`card-${pendente}`)).toHaveAttribute('data-selecionada', 'false');
  await expect(colunaConcluida.locator('[data-selecionada="false"]')).toHaveCount(0);
  await expect(marcarColuna).toHaveText('Desmarcar todas');

  await marcarColuna.click();

  await expect(page.getByTestId(`card-${concluidaA}`)).toHaveAttribute('data-selecionada', 'false');
  await expect(page.getByTestId(`card-${concluidaB}`)).toHaveAttribute('data-selecionada', 'false');
  await expect(marcarColuna).toHaveText('Marcar todas');
});
