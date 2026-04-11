import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink } from '@angular/router';
import { AuthService } from './shared/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    RouterLink
  ],
  template: `
    <mat-toolbar class="app-toolbar" *ngIf="authService.isAuthenticated()">
      <a routerLink="/boats" class="toolbar-brand">
        <mat-icon>directions_boat</mat-icon>
        <span>BoatManager</span>
      </a>
      <span class="spacer"></span>
      <button mat-button [matMenuTriggerFor]="userMenu" class="user-btn">
        <mat-icon>account_circle</mat-icon>
        <span class="user-name">{{ authService.userName() }}</span>
        <mat-icon>arrow_drop_down</mat-icon>
      </button>
      <mat-menu #userMenu="matMenu">
        <button mat-menu-item (click)="authService.logout()">
          <mat-icon>logout</mat-icon>
          <span>Logout</span>
        </button>
      </mat-menu>
    </mat-toolbar>

    <main [class.with-toolbar]="authService.isAuthenticated()">
      <router-outlet />
    </main>
  `,
  styles: [`
    .app-toolbar {
      background: var(--color-primary);
      color: white;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 100;
      box-shadow: var(--shadow-md);
    }
    .toolbar-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      color: white;
      font-family: var(--font-mono);
      font-size: 1.1rem;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .spacer { flex: 1; }
    .user-btn {
      color: white !important;
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .user-name { font-family: var(--font-main); }
    main {
      min-height: 100vh;
    }
    main.with-toolbar {
      padding-top: 64px;
    }
  `]
})
export class AppComponent {
  constructor(public authService: AuthService) {}
}
