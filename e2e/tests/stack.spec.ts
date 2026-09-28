import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

test('@spec:AC-038 o nginx serve a aplicação Angular compilada', async ({ page }) => {
  const resposta = await page.goto('/');

  expect(resposta?.status()).toBe(200);
  expect(resposta?.headers()['content-type']).toContain('text/html');
  await expect(page.locator('app-root')).toBeAttached();

  await expect(page.locator('app-root')).toHaveAttribute('ng-version', /\d/);
});

test('@spec:AC-039 o /api/ do frontend alcança o backend', async ({ request }) => {
  const titulo = `e2e proxy ${randomUUID()}`;

  const criada = await request.post('/api/tasks', {
    data: {
      title: titulo,
      description: 'tarefa criada pelo teste de ponta a ponta',
      status: 'PENDENTE',
      priority: 'MEDIA',
    },
  });
  expect(criada.status()).toBe(201);

  const listagem = await request.get('/api/tasks');
  expect(listagem.status()).toBe(200);

  const tarefas = (await listagem.json()) as Array<{ title: string }>;
  expect(Array.isArray(tarefas)).toBe(true);
  expect(tarefas.map((tarefa) => tarefa.title)).toContain(titulo);
});
