import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import {
  ApiError,
  ApiErrorField,
  Task,
  TaskPayload,
  TaskStatus,
} from '../../features/tasks/models/task';

export class TaskApiFailure extends Error {
  constructor(
    readonly status: number,
    readonly codigo: string,
    message: string,
    readonly campos: ApiErrorField[] = [],
  ) {
    super(message);
    this.name = 'TaskApiFailure';
  }

  get naoEncontrada(): boolean {
    return this.status === 404;
  }

  mensagemDoCampo(campo: string): string | null {
    return this.campos.find((recusado) => recusado.field === campo)?.message ?? null;
  }
}

@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);
  private readonly rota = '/api/tasks';

  listar(): Promise<Task[]> {
    return this.pedir(this.http.get<Task[]>(this.rota));
  }

  criar(tarefa: TaskPayload): Promise<Task> {
    return this.pedir(this.http.post<Task>(this.rota, tarefa));
  }

  substituir(id: number, tarefa: TaskPayload): Promise<Task> {
    return this.pedir(this.http.put<Task>(`${this.rota}/${id}`, tarefa));
  }

  alterarStatus(id: number, status: TaskStatus): Promise<Task> {
    return this.pedir(this.http.patch<Task>(`${this.rota}/${id}/status`, { status }));
  }

  excluir(id: number): Promise<void> {
    return this.pedir(this.http.delete<void>(`${this.rota}/${id}`));
  }

  excluirVarias(ids: number[]): Promise<void> {
    return this.pedir(this.http.post<void>(`${this.rota}/batch-delete`, { ids }));
  }

  private async pedir<T>(chamada: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(chamada);
    } catch (erro) {
      throw this.traduzir(erro);
    }
  }

  private traduzir(erro: unknown): TaskApiFailure {
    if (!(erro instanceof HttpErrorResponse)) {
      return new TaskApiFailure(0, 'UNKNOWN', String(erro));
    }

    const envelope = erro.error as Partial<ApiError> | null;
    return new TaskApiFailure(
      erro.status,
      envelope?.error ?? 'NETWORK_ERROR',
      envelope?.message ?? erro.message,
      envelope?.fields ?? [],
    );
  }
}
