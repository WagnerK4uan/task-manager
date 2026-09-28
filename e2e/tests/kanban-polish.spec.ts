import { randomUUID } from 'node:crypto';
import { APIRequestContext, Page, expect, test } from '@playwright/test';

const criadas: number[] = [];

test.afterEach(async ({ request }) => {
  for (const id of criadas.splice(0)) {
    await request.delete(`/api/tasks/${id}`);
  }
});

async function criar(request: APIRequestContext, title: string): Promise<number> {
  const resposta = await request.post('/api/tasks', {
    data: { title, description: null, status: 'PENDENTE', priority: 'MEDIA', dueDate: null },
  });
  expect(resposta.status()).toBe(201);

  const id = (await resposta.json()).id as number;
  criadas.push(id);
  return id;
}

async function abrirCard(page: Page, titulo: string): Promise<void> {
  await page.goto('/quadro');
  await page.getByRole('button', { name: `Abrir ${titulo}` }).click();
}

function animacaoDoPainel(page: Page): Promise<string> {
  return page
    .getByRole('dialog', { name: 'Editar tarefa' })
    .evaluate((painel) => getComputedStyle(painel).animationName);
}

test('@spec:AC-058 a exclusão pergunta pela própria interface', async ({ page, request }) => {
  const titulo = `${randomUUID().slice(0, 8)} confirmar`;
  const id = await criar(request, titulo);

  const nativos: string[] = [];
  page.on('dialog', (dialogo) => {
    nativos.push(dialogo.message());
    void dialogo.dismiss();
  });

  await abrirCard(page, titulo);
  await page.getByTestId('excluir').click();

  const confirmacao = page.getByRole('alertdialog');
  await expect(confirmacao).toBeVisible();
  await expect(confirmacao).toContainText(titulo);
  await expect(confirmacao.getByTestId('confirmacao-cancelar')).toBeFocused();

  await page.keyboard.press('Escape');

  await expect(confirmacao).toHaveCount(0);
  await expect(page.getByTestId('painel')).toBeVisible();
  expect((await request.get(`/api/tasks/${id}`)).status()).toBe(200);

  await page.getByTestId('excluir').click();
  await page.getByRole('alertdialog').getByTestId('confirmacao-aceitar').click();

  await expect(page.getByTestId('painel')).toHaveCount(0);
  await expect(page.getByTestId(`card-${id}`)).toHaveCount(0);
  expect((await request.get(`/api/tasks/${id}`)).status()).toBe(404);
  expect(nativos).toEqual([]);
});

test('@spec:AC-059 a aba usa o ícone do projeto', async ({ page, request }) => {
  await page.goto('/quadro');

  const icone = page.locator('link[rel="icon"]');
  await expect(icone).toHaveAttribute('href', 'favicon.svg');

  const endereco = await icone.evaluate((link) => (link as HTMLLinkElement).href);
  const resposta = await request.get(endereco);
  expect(resposta.status()).toBe(200);
  expect(resposta.headers()['content-type']).toContain('image/svg+xml');
});

test('@spec:AC-060 o painel entra com animação e respeita movimento reduzido', async ({
  page,
  request,
}) => {
  const titulo = `${randomUUID().slice(0, 8)} animar`;
  await criar(request, titulo);

  await abrirCard(page, titulo);
  expect(await animacaoDoPainel(page)).not.toBe('none');

  await page.emulateMedia({ reducedMotion: 'reduce' });
  await abrirCard(page, titulo);
  expect(await animacaoDoPainel(page)).toBe('none');
});
