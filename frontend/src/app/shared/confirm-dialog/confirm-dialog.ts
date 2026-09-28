import { A11yModule } from '@angular/cdk/a11y';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  imports: [A11yModule],
  templateUrl: './confirm-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly titulo = input.required<string>();
  readonly mensagem = input.required<string>();
  readonly acao = input.required<string>();

  readonly confirmar = output<void>();
  readonly cancelar = output<void>();
}
