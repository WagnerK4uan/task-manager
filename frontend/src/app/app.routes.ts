import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'quadro',
    title: 'Quadro de tarefas',
    loadComponent: () =>
      import('./features/tasks/pages/task-board/task-board').then((m) => m.TaskBoard),
  },
  { path: '', pathMatch: 'full', redirectTo: 'quadro' },
];
