import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../shared/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-hero">
          <mat-icon class="hero-icon">directions_boat</mat-icon>
          <h1>BoatManager</h1>
          <p class="subtitle">Your fleet management platform</p>
        </div>
        <button mat-raised-button class="login-btn" (click)="authService.login()">
          <mat-icon>login</mat-icon>
          Sign in with Keycloak
        </button>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, var(--color-primary) 0%, #023e8a 100%);
    }
    .login-card {
      background: white;
      border-radius: 20px;
      padding: 48px 40px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
      max-width: 380px;
      width: 90%;
    }
    .hero-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: var(--color-primary);
      margin-bottom: 16px;
    }
    h1 {
      font-family: var(--font-mono);
      font-size: 2rem;
      color: var(--color-primary);
      margin-bottom: 8px;
    }
    .subtitle {
      color: var(--color-text-secondary);
      margin-bottom: 32px;
    }
    .login-btn {
      background: var(--color-primary) !important;
      color: white !important;
      width: 100%;
      height: 48px;
      font-size: 1rem;
      border-radius: 8px !important;
      gap: 8px;
    }
  `]
})
export class LoginComponent {
  constructor(public authService: AuthService) {}
}
