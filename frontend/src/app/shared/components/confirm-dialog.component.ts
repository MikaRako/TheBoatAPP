import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
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
  imports: [CommonModule, MatDialogModule, MatIconModule],
  template: `
    <div class="dialog-container"
         role="alertdialog"
         aria-modal="true"
         aria-labelledby="dialog-title"
         aria-describedby="dialog-message">
      <div class="dialog-icon" [class.danger]="data.dangerous" aria-hidden="true">
        <mat-icon>{{ data.dangerous ? 'warning' : 'help_outline' }}</mat-icon>
      </div>

      <h2 id="dialog-title" class="dialog-title">{{ data.title }}</h2>
      <p  id="dialog-message" class="dialog-message">{{ data.message }}</p>

      <div class="dialog-actions">
        <button class="btn-confirm" [class.btn-danger]="data.dangerous" (click)="dialogRef.close(true)">
          {{ data.confirmText || 'Confirm' }}
        </button>
        <button class="btn-cancel" (click)="dialogRef.close(false)">
          {{ data.cancelText || 'Cancel' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    ::ng-deep .mat-mdc-dialog-surface {
      background: #192744 !important;
      border-radius: 16px !important;
      box-shadow: 0 24px 64px rgba(0,0,0,0.55) !important;
      border: 1px solid rgba(99,140,200,0.14) !important;
    }

    .dialog-container {
      padding: 36px 28px 28px;
      min-width: 340px;
      max-width: 460px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    /* Icon */
    .dialog-icon {
      width: 64px;
      height: 64px;
      border-radius: 14px;
      background: rgba(42,82,152,0.18);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
    }
    .dialog-icon mat-icon {
      font-size: 30px;
      width: 30px;
      height: 30px;
      color: #93c5fd;
    }
    .dialog-icon.danger {
      background: rgba(232,110,90,0.15);
    }
    .dialog-icon.danger mat-icon {
      color: #e8836a;
    }

    /* Text */
    .dialog-title {
      font-size: 1.2rem;
      font-weight: 700;
      color: #e2e8f4;              /* on #192744 ≈ 10.4:1 ✓ */
      text-align: center;
      margin-bottom: 10px;
      font-family: var(--font-main);
    }
    .dialog-message {
      font-size: 0.875rem;
      color: #94A3B8;              /* on #192744 ≈ 5.4:1 ✓ */
      text-align: center;
      line-height: 1.65;
      margin-bottom: 28px;
    }

    /* Buttons */
    .dialog-actions {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .btn-confirm {
      width: 100%;
      height: 46px;
      border: none;
      border-radius: 8px;
      font-family: var(--font-main);
      font-size: 0.78rem;
      font-weight: 700;
      letter-spacing: 1.2px;
      text-transform: uppercase;
      cursor: pointer;
      background: linear-gradient(135deg, #1a3a6b 0%, #2a5298 100%);
      color: white;
      box-shadow: 0 4px 16px rgba(42,82,152,0.3);
      transition: opacity 0.15s, box-shadow 0.15s;
    }
    .btn-confirm.btn-danger {
      background: linear-gradient(135deg, #b94030 0%, #e8836a 100%);
      box-shadow: 0 4px 16px rgba(232,110,90,0.3);
    }
    .btn-confirm:hover {
      opacity: 0.88;
    }

    .btn-cancel {
      width: 100%;
      height: 42px;
      border: none;
      border-radius: 8px;
      background: transparent;
      font-family: var(--font-main);
      font-size: 0.875rem;
      font-weight: 500;
      color: #94A3B8;              /* on #192744 ≈ 5.4:1 ✓ */
      cursor: pointer;
      transition: color 0.15s;
    }
    .btn-cancel:hover {
      color: #e2e8f4;
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}
}
