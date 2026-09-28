import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Task, TaskPriority } from '../../models/task';

const PRIORIDADE: Record<TaskPriority, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

const COR_DA_PRIORIDADE: Record<TaskPriority, string> = {
  BAIXA: 'bg-baixa-wash text-baixa',
  MEDIA: 'bg-media-wash text-media',
  ALTA: 'bg-alta-wash text-alta',
};

@Component({
  selector: 'app-task-card',
  templateUrl: './task-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCard {
  readonly tarefa = input.required<Task>();
  readonly destinoSeguinte = input<string | null>(null);
  readonly destinoAnterior = input<string | null>(null);
  readonly recemMexida = input(false);

  readonly abrir = output<Task>();
  readonly avancar = output<Task>();
  readonly voltar = output<Task>();

  readonly prioridade = computed(() => PRIORIDADE[this.tarefa().priority]);
  readonly corDaPrioridade = computed(() => COR_DA_PRIORIDADE[this.tarefa().priority]);

  readonly prazo = computed(() => {
    const iso = this.tarefa().dueDate;
    if (!iso) return 'sem prazo';

    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
  });
}
