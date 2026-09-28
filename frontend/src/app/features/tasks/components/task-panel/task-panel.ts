import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ConfirmDialog } from '../../../../shared/confirm-dialog/confirm-dialog';
import { ApiErrorField, Task, TaskPayload, TaskPriority, TaskStatus } from '../../models/task';

@Component({
  selector: 'app-task-panel',
  imports: [ReactiveFormsModule, ConfirmDialog],
  templateUrl: './task-panel.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskPanel {
  private readonly fb = inject(FormBuilder);

  readonly tarefa = input<Task | null>(null);
  readonly situacaoInicial = input<TaskStatus>('PENDENTE');
  readonly salvando = input(false);
  readonly recusados = input<ApiErrorField[]>([]);

  readonly salvar = output<TaskPayload>();
  readonly excluir = output<Task>();
  readonly fechar = output<void>();

  readonly formulario = this.fb.nonNullable.group({
    title: '',
    description: '',
    status: 'PENDENTE' as TaskStatus,
    priority: 'MEDIA' as TaskPriority,
    dueDate: '',
  });

  readonly edicao = computed(() => this.tarefa() !== null);
  readonly confirmandoExclusao = signal(false);
  readonly avisoDeExclusao = computed(
    () => `"${this.tarefa()?.title ?? ''}" será removida do quadro. Não há como desfazer.`,
  );

  constructor() {
    effect(() => {
      const tarefa = this.tarefa();
      this.formulario.reset({
        title: tarefa?.title ?? '',
        description: tarefa?.description ?? '',
        status: tarefa?.status ?? this.situacaoInicial(),
        priority: tarefa?.priority ?? 'MEDIA',
        dueDate: tarefa?.dueDate ?? '',
      });
    });
  }

  @HostListener('document:keydown.escape')
  aoApertarEsc(): void {
    if (this.confirmandoExclusao()) {
      this.confirmandoExclusao.set(false);
      return;
    }

    this.fechar.emit();
  }

  enviar(): void {
    const valores = this.formulario.getRawValue();

    this.salvar.emit({
      title: valores.title,
      description: valores.description.trim() || null,
      status: valores.status,
      priority: valores.priority,
      dueDate: valores.dueDate || null,
    });
  }

  pedirExclusao(): void {
    this.confirmandoExclusao.set(true);
  }

  cancelarExclusao(): void {
    this.confirmandoExclusao.set(false);
  }

  confirmarExclusao(): void {
    const tarefa = this.tarefa();
    this.confirmandoExclusao.set(false);
    if (tarefa) this.excluir.emit(tarefa);
  }

  erroDe(campo: string): string | null {
    return this.recusados().find((recusado) => recusado.field === campo)?.message ?? null;
  }
}
