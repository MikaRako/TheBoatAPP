import { Component, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from './shared/services/auth.service';
import { ThemeService } from './shared/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatIconModule, MatTooltipModule],
  template: `
    <!-- Skip to main content (WCAG 2.4.1) -->
    <a class="skip-link" href="#main-content">Skip to main content</a>

    @if (authService.isAuthenticated()) {
      <nav class="top-nav" aria-label="Main navigation">
        <a class="nav-brand" routerLink="/boats" aria-label="Boat Management — go to fleet">
          <div class="nav-logo" aria-hidden="true">
            <mat-icon>sailing</mat-icon>
          </div>
          <span class="nav-title">Boat Management</span>
        </a>

        <div class="nav-links"></div>

        <div class="nav-actions">
          <!-- Dark / light mode toggle -->
          <button
            type="button"
            class="theme-toggle"
            (click)="themeService.toggle()"
            [matTooltip]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'"
            matTooltipPosition="below"
            [attr.aria-label]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'"
            [attr.aria-pressed]="themeService.isDark()">
            <mat-icon aria-hidden="true">{{ themeService.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
            <span class="theme-toggle-label">
              {{ themeService.isDark() ? 'Light mode' : 'Dark mode' }}
            </span>
          </button>

          <!-- User pill -->
          <div class="nav-user">
            <button
              type="button"
              class="user-pill"
              (click)="toggleMenu($event)"
              [class.open]="menuOpen"
              [attr.aria-expanded]="menuOpen"
              aria-haspopup="menu"
              [attr.aria-label]="'User menu for ' + authService.userName()">
              <span class="pill-avatar" aria-hidden="true">{{ userInitial }}</span>
              <span class="pill-name">{{ authService.userName() }}</span>
              <mat-icon class="pill-chevron" aria-hidden="true">expand_more</mat-icon>
            </button>

            @if (menuOpen) {
            <div class="user-dropdown" role="menu" aria-label="User options">
              <div class="dropdown-header" aria-hidden="true">
                <div class="dropdown-avatar">{{ userInitial }}</div>
                <div class="dropdown-info">
                  <span class="dropdown-name">{{ authService.userName() }}</span>
                  <span class="dropdown-role">User</span>
                </div>
              </div>
              <div class="dropdown-divider" role="separator"></div>
              <button
                type="button"
                class="dropdown-item dropdown-item-danger"
                role="menuitem"
                (click)="authService.logout()">
                <mat-icon aria-hidden="true">logout</mat-icon>
                Sign out
              </button>
            </div>
            }
          </div>
        </div>
      </nav>

      <main class="main-content" id="main-content" tabindex="-1">
        <router-outlet />
      </main>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .top-nav {
      height: 60px;
      background: var(--color-surface);
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
      color: var(--color-heading);
      letter-spacing: 0.3px;
    }

    .nav-links {
      flex: 1;
    }

    /* ── Right-side actions (theme toggle + user) ── */
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    /* Theme toggle */
    .theme-toggle {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 38px;
      padding: 0 12px;
      border-radius: 8px;
      border: 1.5px solid var(--color-border);
      background: var(--color-background);
      color: var(--color-text);
      cursor: pointer;
      font-family: var(--font-main);
      font-size: 0.8rem;
      font-weight: 500;
      white-space: nowrap;
      transition: background 0.15s, border-color 0.15s;
    }
    .theme-toggle:hover {
      background: var(--color-surface-hover);
      border-color: var(--color-primary);
    }
    .theme-toggle mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      flex-shrink: 0;
    }
    .theme-toggle-label {
      /* hidden on mobile, visible on desktop */
    }
    @media (max-width: 767px) {
      .theme-toggle {
        width: 38px;
        padding: 0;
        justify-content: center;
      }
      .theme-toggle-label { display: none; }
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
      background: var(--color-background);
      color: var(--color-text);
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s;
    }
    .user-pill:hover,
    .user-pill.open {
      background: var(--color-surface-hover);
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
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: 10px;
      box-shadow: var(--shadow-lg);
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
    .dropdown-item:hover { background: var(--color-surface-hover); }
    .dropdown-item-danger { color: var(--color-warn); }
    .dropdown-item-danger:hover { background: rgba(185, 28, 28, 0.08); }

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
    /* Remove default outline on programmatic focus of main (skip link target) */
    .main-content:focus { outline: none; }
  `]
})
export class AppComponent {
  menuOpen = false;

  constructor(
    public authService: AuthService,
    public themeService: ThemeService
  ) {}

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
