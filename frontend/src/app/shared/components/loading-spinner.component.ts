import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="spinner-overlay" [class.fullpage]="fullPage">
      <mat-spinner [diameter]="diameter" color="primary" />
      <p *ngIf="message" class="spinner-message">{{ message }}</p>
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
      background: rgba(255,255,255,0.8);
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
