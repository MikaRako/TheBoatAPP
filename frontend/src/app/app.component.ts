import { Component, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './shared/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatIconModule],
  template: `
    @if (authService.isAuthenticated()) {
      <nav class="top-nav">
        <a class="nav-brand" routerLink="/boats">
          <div class="nav-logo">
            <mat-icon>sailing</mat-icon>
          </div>
          <span class="nav-title">Boat Management</span>
        </a>

        <div class="nav-links"></div>

        <div class="nav-user">
          <button class="user-pill" (click)="toggleMenu($event)" [class.open]="menuOpen">
            <span class="pill-avatar">{{ userInitial }}</span>
            <span class="pill-name">{{ authService.userName() }}</span>
            <mat-icon class="pill-chevron">expand_more</mat-icon>
          </button>

          @if (menuOpen) {
          <div class="user-dropdown" (click)="$event.stopPropagation()">
            <div class="dropdown-header">
              <div class="dropdown-avatar">{{ userInitial }}</div>
              <div class="dropdown-info">
                <span class="dropdown-name">{{ authService.userName() }}</span>
                <span class="dropdown-role">User</span>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item dropdown-item-danger" (click)="authService.logout()">
              <mat-icon>logout</mat-icon>
              Sign out
            </button>
          </div>
          }
        </div>
      </nav>

      <main class="main-content">
        <router-outlet />
      </main>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .top-nav {
      height: 60px;
      background: #ffffff;
      display: flex;
      align-items: center;
      padding: 0 24px;
      gap: 32px;
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 0 var(--color-border), 0 2px 12px rgba(0,0,0,0.06);
    }

    .nav-brand {
      display: flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      flex-shrink: 0;
      cursor: pointer;
    }
    .nav-logo {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: var(--color-primary);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .nav-logo mat-icon {
      color: white;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
    .nav-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--color-primary);
      letter-spacing: 0.3px;
    }

    .nav-links {
      flex: 1;
    }

    /* ── User avatar + dropdown ── */
    .nav-user {
      position: relative;
      flex-shrink: 0;
    }

    .user-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      height: 38px;
      padding: 0 10px 0 5px;
      border-radius: 999px;
      border: 1.5px solid var(--color-border);
      background: #F8FAFC;
      color: var(--color-primary);
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s;
    }
    .user-pill:hover,
    .user-pill.open {
      background: #EDF2F7;
      border-color: var(--color-primary);
    }

    .pill-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--color-primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.78rem;
      font-weight: 700;
      text-transform: uppercase;
      flex-shrink: 0;
    }
    .pill-name {
      font-size: 0.85rem;
      font-weight: 600;
      white-space: nowrap;
      max-width: 140px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .pill-chevron {
      font-size: 16px;
      width: 16px;
      height: 16px;
      opacity: 0.5;
      transition: transform 0.15s;
    }
    .user-pill.open .pill-chevron {
      transform: rotate(180deg);
    }

    .user-dropdown {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      min-width: 200px;
      background: white;
      border-radius: 10px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.14), 0 2px 8px rgba(0,0,0,0.08);
      overflow: hidden;
      animation: dropdown-in 0.12s ease;
      z-index: 200;
    }
    @keyframes dropdown-in {
      from { opacity: 0; transform: translateY(-6px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .dropdown-header {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 14px 16px;
    }
    .dropdown-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--color-primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      font-weight: 700;
      text-transform: uppercase;
      flex-shrink: 0;
    }
    .dropdown-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
    }
    .dropdown-name {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--color-text);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .dropdown-role {
      font-size: 0.72rem;
      color: var(--color-text-muted);
    }

    .dropdown-divider {
      height: 1px;
      background: var(--color-border);
      margin: 0;
    }

    .dropdown-item {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 11px 16px;
      border: none;
      background: none;
      font-family: var(--font-main);
      font-size: 0.875rem;
      color: var(--color-text);
      cursor: pointer;
      text-align: left;
      transition: background 0.12s;
    }
    .dropdown-item mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .dropdown-item:hover { background: #F8FAFC; }
    .dropdown-item-danger { color: var(--color-warn); }
    .dropdown-item-danger:hover { background: #FEF2F2; }

    @media (max-width: 767px) {
      .user-pill {
        width: 36px;
        height: 36px;
        padding: 0;
        border-radius: 50%;
        justify-content: center;
        gap: 0;
        background: var(--color-primary);
        border-color: transparent;
      }
      .pill-avatar {
        width: 26px;
        height: 26px;
        background: rgba(255,255,255,0.2);
      }
      .pill-name,
      .pill-chevron {
        display: none;
      }
    }

    .main-content {
      min-height: calc(100vh - 60px);
      background: transparent;
    }
  `]
})
export class AppComponent {
  menuOpen = false;

  constructor(public authService: AuthService) {}

  get userInitial(): string {
    const name = this.authService.userName();
    return name ? name.charAt(0) : '?';
  }

  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.menuOpen = !this.menuOpen;
  }

  @HostListener('document:click')
  closeMenu(): void {
    this.menuOpen = false;
  }
}
