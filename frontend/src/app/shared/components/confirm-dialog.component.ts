import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  dangerous?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="dialog-container">
      <div class="dialog-icon" [class.danger]="data.dangerous">
        <mat-icon>{{ data.dangerous ? 'warning' : 'help_outline' }}</mat-icon>
      </div>
      <h2 mat-dialog-title>{{ data.title }}</h2>
      <mat-dialog-content>
        <p>{{ data.message }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-stroked-button [mat-dialog-close]="false">
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button
          mat-raised-button
          [color]="data.dangerous ? 'warn' : 'primary'"
          [mat-dialog-close]="true"
          cdkFocusInitial>
          {{ data.confirmText || 'Confirm' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      padding: 8px 8px 0;
      min-width: 320px;
      max-width: 480px;
    }
    .dialog-icon {
      display: flex;
      justify-content: center;
      margin-bottom: 8px;
      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: var(--color-primary);
      }
      &.danger mat-icon { color: var(--color-warn); }
    }
    h2 { text-align: center; font-family: var(--font-main); }
    p { color: var(--color-text-secondary); text-align: center; margin-top: 8px; }
    mat-dialog-actions { gap: 8px; padding: 16px 0; }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}
}
