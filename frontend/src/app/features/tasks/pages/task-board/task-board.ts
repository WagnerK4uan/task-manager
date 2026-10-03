import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { TaskApi, TaskApiFailure } from '../../../../core/services/task-api';
import { ConfirmDialog } from '../../../../shared/confirm-dialog/confirm-dialog';
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
  imports: [DragDropModule, TaskCard, TaskPanel, ConfirmDialog],
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

  readonly selecionando = signal(false);
  readonly selecionadas = signal<ReadonlySet<number>>(new Set());
  readonly confirmandoLote = signal(false);

  constructor() {
    void this.carregar();
  }

  resumo(): string {
    const total = this.tarefas().length;
    return total === 1 ? '1 tarefa no quadro' : `${total} tarefas no quadro`;
  }

  quantidadeSelecionada(): string {
    const total = this.selecionadas().size;
    return total === 1 ? '1 selecionada' : `${total} selecionadas`;
  }

  tituloDoLote(): string {
    const total = this.selecionadas().size;
    return total === 1 ? 'Excluir 1 tarefa?' : `Excluir ${total} tarefas?`;
  }

  avisoDoLote(): string {
    const total = this.selecionadas().size;
    const quais = total === 1 ? '1 tarefa será removida' : `${total} tarefas serão removidas`;
    return `${quais} do quadro. Não há como desfazer.`;
  }

  colunaMarcada(status: TaskStatus): boolean {
    const daColuna = this.tarefasDa(status);
    return daColuna.length > 0 && daColuna.every((tarefa) => this.selecionadas().has(tarefa.id));
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

  @HostListener('document:keydown.escape')
  aoApertarEsc(): void {
    this.confirmandoLote.set(false);
  }

  entrarNaSelecao(): void {
    this.selecionadas.set(new Set());
    this.selecionando.set(true);
  }

  sairDaSelecao(): void {
    this.confirmandoLote.set(false);
    this.selecionando.set(false);
    this.selecionadas.set(new Set());
  }

  alternarSelecao(tarefa: Task): void {
    const marcadas = new Set(this.selecionadas());
    if (!marcadas.delete(tarefa.id)) marcadas.add(tarefa.id);
    this.selecionadas.set(marcadas);
  }

  alternarColuna(status: TaskStatus): void {
    const marcar = !this.colunaMarcada(status);
    const marcadas = new Set(this.selecionadas());

    for (const tarefa of this.tarefasDa(status)) {
      if (marcar) marcadas.add(tarefa.id);
      else marcadas.delete(tarefa.id);
    }

    this.selecionadas.set(marcadas);
  }

  pedirExclusaoDoLote(): void {
    if (this.selecionadas().size > 0) this.confirmandoLote.set(true);
  }

  cancelarExclusaoDoLote(): void {
    this.confirmandoLote.set(false);
  }

  async excluirSelecionadas(): Promise<void> {
    const ids = [...this.selecionadas()];
    this.confirmandoLote.set(false);
    this.aviso.set(null);

    try {
      await this.api.excluirVarias(ids);
      this.sairDaSelecao();
    } catch (erro) {
      this.aviso.set(
        erro instanceof TaskApiFailure && erro.naoEncontrada
          ? 'Alguma tarefa já não existia; nada foi excluído. O quadro foi atualizado.'
          : 'Não foi possível excluir as tarefas selecionadas. O quadro foi atualizado.',
      );
    }

    await this.carregar();
    this.podarSelecao();
  }

  private podarSelecao(): void {
    const existentes = new Set(this.tarefas().map((tarefa) => tarefa.id));
    this.selecionadas.set(new Set([...this.selecionadas()].filter((id) => existentes.has(id))));
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
