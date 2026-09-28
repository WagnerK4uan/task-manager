import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TaskApi, TaskApiFailure } from '../../../../core/services/task-api';
import { TaskCard } from '../../components/task-card/task-card';
import { TaskPanel } from '../../components/task-panel/task-panel';
import { ApiErrorField, Task, TaskPayload, TaskStatus } from '../../models/task';

interface Coluna {
  status: TaskStatus;
  titulo: string;
  moldura: string;
  cabecalho: string;
  contador: string;
}

const COLUNAS: Coluna[] = [
  {
    status: 'PENDENTE',
    titulo: 'Pendente',
    moldura: 'border-pendente/30 bg-pendente-wash',
    cabecalho: 'text-pendente',
    contador: 'bg-pendente text-surface',
  },
  {
    status: 'EM_ANDAMENTO',
    titulo: 'Em andamento',
    moldura: 'border-andamento/30 bg-andamento-wash',
    cabecalho: 'text-andamento',
    contador: 'bg-andamento text-surface',
  },
  {
    status: 'CONCLUIDA',
    titulo: 'Concluída',
    moldura: 'border-concluida/30 bg-concluida-wash',
    cabecalho: 'text-concluida',
    contador: 'bg-concluida text-surface',
  },
];

@Component({
  selector: 'app-task-board',
  imports: [DragDropModule, TaskCard, TaskPanel],
  templateUrl: './task-board.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskBoard {
  private readonly api = inject(TaskApi);

  readonly colunas = COLUNAS;

  readonly tarefas = signal<Task[]>([]);
  readonly carregando = signal(false);
  readonly erro = signal(false);
  readonly aviso = signal<string | null>(null);
  readonly recemMexida = signal<number | null>(null);

  readonly painelAberto = signal(false);
  readonly tarefaDoPainel = signal<Task | null>(null);
  readonly situacaoDoPainel = signal<TaskStatus>('PENDENTE');
  readonly salvando = signal(false);
  readonly recusados = signal<ApiErrorField[]>([]);

  constructor() {
    void this.carregar();
  }

  resumo(): string {
    const total = this.tarefas().length;
    return total === 1 ? '1 tarefa no quadro' : `${total} tarefas no quadro`;
  }

  tarefasDa(status: TaskStatus): Task[] {
    return this.tarefas().filter((tarefa) => tarefa.status === status);
  }

  tituloDaColuna(status: TaskStatus): string {
    return COLUNAS.find((coluna) => coluna.status === status)?.titulo ?? '';
  }

  destinoSeguinte(status: TaskStatus): string | null {
    return this.vizinha(status, 1)?.titulo ?? null;
  }

  destinoAnterior(status: TaskStatus): string | null {
    return this.vizinha(status, -1)?.titulo ?? null;
  }

  async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set(false);

    try {
      this.tarefas.set(await this.api.listar());
    } catch {
      this.tarefas.set([]);
      this.erro.set(true);
    } finally {
      this.carregando.set(false);
    }
  }

  async soltar(evento: CdkDragDrop<TaskStatus>): Promise<void> {
    const destino = evento.container.data;
    const tarefa = evento.item.data as Task;

    if (destino === tarefa.status) return;

    await this.trocarSituacao(tarefa, destino);
  }

  async mover(tarefa: Task, passo: 1 | -1): Promise<void> {
    const destino = this.vizinha(tarefa.status, passo);
    if (!destino) return;

    await this.trocarSituacao(tarefa, destino.status);
  }

  abrirCard(tarefa: Task): void {
    this.recusados.set([]);
    this.tarefaDoPainel.set(tarefa);
    this.situacaoDoPainel.set(tarefa.status);
    this.painelAberto.set(true);
  }

  abrirCriacao(status: TaskStatus): void {
    this.recusados.set([]);
    this.tarefaDoPainel.set(null);
    this.situacaoDoPainel.set(status);
    this.painelAberto.set(true);
  }

  fecharPainel(): void {
    this.painelAberto.set(false);
    this.tarefaDoPainel.set(null);
    this.recusados.set([]);
  }

  async salvarPainel(payload: TaskPayload): Promise<void> {
    const tarefa = this.tarefaDoPainel();
    this.salvando.set(true);
    this.recusados.set([]);
    this.aviso.set(null);

    try {
      const gravada = tarefa
        ? await this.api.substituir(tarefa.id, payload)
        : await this.api.criar(payload);

      this.recemMexida.set(gravada.id);
      this.fecharPainel();
    } catch (erro) {
      if (erro instanceof TaskApiFailure && erro.campos.length > 0) {
        this.recusados.set(erro.campos);
      } else if (erro instanceof TaskApiFailure && erro.naoEncontrada) {
        this.aviso.set('Essa tarefa já não existe. O quadro foi atualizado.');
        this.fecharPainel();
      } else {
        this.aviso.set('Não foi possível salvar essa tarefa. Tente de novo.');
      }
    } finally {
      this.salvando.set(false);
      await this.carregar();
    }
  }

  async excluirTarefa(tarefa: Task): Promise<void> {
    this.aviso.set(null);

    try {
      await this.api.excluir(tarefa.id);
    } catch (erro) {
      if (!(erro instanceof TaskApiFailure && erro.naoEncontrada)) {
        this.aviso.set('Não foi possível excluir essa tarefa. O quadro foi atualizado.');
      }
    }

    this.fecharPainel();
    await this.carregar();
  }

  private async trocarSituacao(tarefa: Task, destino: TaskStatus): Promise<void> {
    this.aviso.set(null);

    try {
      await this.api.alterarStatus(tarefa.id, destino);
      this.recemMexida.set(tarefa.id);
    } catch {
      this.aviso.set(`Não foi possível mover "${tarefa.title}". O quadro foi atualizado.`);
    }

    await this.carregar();
  }

  private vizinha(status: TaskStatus, passo: 1 | -1): Coluna | null {
    const posicao = COLUNAS.findIndex((coluna) => coluna.status === status);
    return COLUNAS[posicao + passo] ?? null;
  }
}
