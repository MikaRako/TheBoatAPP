import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="spinner-overlay" [class.fullpage]="fullPage" role="status" [attr.aria-label]="message || 'Loading'">
      <mat-spinner [diameter]="diameter" color="primary" aria-hidden="true" />
      <p *ngIf="message" class="spinner-message" aria-live="polite">{{ message }}</p>
    </div>
  `,
  styles: [`
    .spinner-overlay {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      gap: 16px;
    }
    .spinner-overlay.fullpage {
      position: fixed;
      inset: 0;
      background: color-mix(in srgb, var(--color-surface) 85%, transparent);
      z-index: 9999;
    }
    .spinner-message {
      color: var(--color-text-secondary);
      font-size: 0.95rem;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() diameter = 48;
  @Input() message = '';
  @Input() fullPage = false;
}
