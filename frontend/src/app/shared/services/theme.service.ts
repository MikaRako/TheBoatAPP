import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'boat-theme';
  isDark = signal(false);

  constructor() {
    const stored =
      typeof localStorage !== 'undefined'
        ? localStorage.getItem(this.STORAGE_KEY)
        : null;
    const prefersDark =
      typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.isDark.set(stored !== null ? stored === 'dark' : prefersDark);
    this.apply();
  }

  toggle(): void {
    this.isDark.update(v => !v);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, this.isDark() ? 'dark' : 'light');
    }
    this.apply();
  }

  private apply(): void {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute(
        'data-theme',
        this.isDark() ? 'dark' : 'light'
      );
    }
  }
}
