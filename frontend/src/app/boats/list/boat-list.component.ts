import { Component, OnInit, OnDestroy, ChangeDetectorRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, switchMap, startWith, combineLatest, BehaviorSubject, EMPTY, catchError } from 'rxjs';
import { BoatService, Boat, BoatPage } from '../../shared/services/boat.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';

type StatusFilter = 'all' | 'active' | 'maintenance' | 'drydock';

@Component({
  selector: 'app-boat-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatSortModule,
    MatProgressBarModule
  ],
  template: `
    <div class="fleet-page">

      <!-- Top bar -->
      <div class="top-bar">
        <div class="top-bar-left">
          <h1 class="page-title">Fleet Vessels</h1>
        </div>
        <div class="top-bar-right">
          <div class="search-wrapper">
            <mat-icon class="search-icon">search</mat-icon>
            <input
              class="search-input"
              [formControl]="searchControl"
              placeholder="Search by name or description..." />
            <button class="search-clear" *ngIf="searchControl.value" (click)="searchControl.setValue('')">
              <mat-icon>close</mat-icon>
            </button>
          </div>
          <a class="btn-add" routerLink="/boats/new">
            <mat-icon>add</mat-icon>
            ADD NEW BOAT
          </a>
        </div>
      </div>

      <!-- Stats + filters row -->
      <div class="stats-filter-row">
        <div class="stat-block">
          <div class="fleet-ready-label">FLEET READY</div>
          <div class="fleet-ready-value">
            <span class="fleet-pct">{{ fleetReadyPct }}%</span>
          </div>
          <div class="fleet-ready-sub">
            <span class="in-port-chip">
              <mat-icon>anchor</mat-icon>
              {{ totalElements }} Vessel{{ totalElements !== 1 ? 's' : '' }}
            </span>
          </div>
        </div>

        <div class="filter-group">
          <div class="vessel-type-select">
            <select class="type-select">
              <option>All Vessel Types</option>
              <option>Yacht</option>
              <option>Commercial</option>
              <option>Catamaran</option>
            </select>
            <mat-icon class="select-arrow">expand_more</mat-icon>
          </div>

          <div class="status-tabs">
            <button class="status-tab" [class.active]="statusFilter === 'all'" (click)="setStatusFilter('all')">All</button>
            <button class="status-tab active-tab" [class.active]="statusFilter === 'active'" (click)="setStatusFilter('active')">Active</button>
            <button class="status-tab maint-tab" [class.active]="statusFilter === 'maintenance'" (click)="setStatusFilter('maintenance')">Maintenance</button>
            <button class="status-tab dock-tab" [class.active]="statusFilter === 'drydock'" (click)="setStatusFilter('drydock')">Dry Dock</button>
          </div>
        </div>
      </div>

      <!-- Loading bar -->
      <mat-progress-bar *ngIf="loading" mode="indeterminate" class="top-loader" />

      <!-- Error state -->
      <div *ngIf="error && !loading" class="state-card">
        <mat-icon class="state-icon error-icon">error_outline</mat-icon>
        <h3>Failed to load vessels</h3>
        <p>{{ error }}</p>
        <button class="btn-retry" (click)="refresh()">Try Again</button>
      </div>

      <!-- Empty state -->
      <div *ngIf="!loading && !error && boats.length === 0" class="state-card">
        <mat-icon class="state-icon">sailing</mat-icon>
        <h3>No vessels found</h3>
        <p *ngIf="searchControl.value">No results for "{{ searchControl.value }}"</p>
        <p *ngIf="!searchControl.value">Your fleet is empty. Add your first vessel!</p>
        <a class="btn-add-inline" routerLink="/boats/new">Add First Vessel</a>
      </div>

      <!-- Card grid -->
      <div class="vessel-grid" *ngIf="!error && boats.length > 0"
           [style.gridTemplateColumns]="gridCols"
           [class.vessel-grid--list]="isListView">
        <div class="vessel-card" *ngFor="let boat of boats"
             [routerLink]="['/boats', boat.id]"
             [class.vessel-card--list]="isListView"
             style="cursor:pointer">

          <!-- ── Card view (4 / 8 items per page) ── -->
          <ng-container *ngIf="!isListView">
            <div class="card-image">
              <div class="image-placeholder" [ngClass]="'img-' + (boat.id % 4)">
                <mat-icon>directions_boat</mat-icon>
              </div>
            </div>
            <div class="card-body">
              <div class="card-title-row">
                <h3 class="vessel-name">{{ boat.name }}</h3>
                <span class="status-badge" [ngClass]="getStatusClass(boat.id)">
                  {{ getStatusLabel(boat.id) }}
                </span>
              </div>
              <p class="vessel-desc">{{ boat.description || 'No description provided.' }}</p>
              <div class="card-divider"></div>
              <div class="meta-grid">
                <div class="meta-col">
                  <span class="meta-label">CREATED</span>
                  <span class="meta-value">{{ boat.createdAt | date:'MMM d, y' }}</span>
                </div>
              </div>
              <div class="card-actions">
                <div class="card-btn-group" (click)="$event.stopPropagation()">
                  <a class="card-icon-btn card-icon-btn-view" [routerLink]="['/boats', boat.id]" matTooltip="View vessel">
                    <mat-icon>visibility</mat-icon>
                  </a>
                  <a class="card-icon-btn" [routerLink]="['/boats', boat.id, 'edit']" matTooltip="Edit vessel">
                    <mat-icon>edit</mat-icon>
                  </a>
                  <button class="card-icon-btn card-icon-btn-danger" (click)="confirmDelete(boat)" matTooltip="Delete vessel">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </div>
              </div>
            </div>
          </ng-container>

          <!-- ── List view (16 / 32 items per page) ── -->
          <ng-container *ngIf="isListView">
            <div class="list-strip" [ngClass]="'img-' + (boat.id % 4)">
              <mat-icon>directions_boat</mat-icon>
            </div>
            <div class="list-info">
              <h3 class="vessel-name">{{ boat.name }}</h3>
              <p class="vessel-desc">{{ boat.description || 'No description provided.' }}</p>
            </div>
            <span class="status-badge list-status" [ngClass]="getStatusClass(boat.id)">
              {{ getStatusLabel(boat.id) }}
            </span>
            <span class="list-date">
              <mat-icon>calendar_today</mat-icon>
              {{ boat.createdAt | date:'MMM d, y' }}
            </span>
            <div class="list-actions" (click)="$event.stopPropagation()">
              <a class="card-icon-btn card-icon-btn-view" [routerLink]="['/boats', boat.id]" matTooltip="View vessel">
                <mat-icon>visibility</mat-icon>
              </a>
              <a class="card-icon-btn" [routerLink]="['/boats', boat.id, 'edit']" matTooltip="Edit vessel">
                <mat-icon>edit</mat-icon>
              </a>
              <button class="card-icon-btn card-icon-btn-danger" (click)="confirmDelete(boat)" matTooltip="Delete vessel">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </div>
          </ng-container>

        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-bar" *ngIf="!error && totalElements > 0">
        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[4, 8, 16, 32]"
          [pageIndex]="currentPage"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      </div>

    </div>
  `,
  styles: [`
    .fleet-page {
      max-width: 1100px;
      margin: 0 auto;
      padding: 28px 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      overflow-x: hidden;
    }

    /* ── Top bar ── */
    .top-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--color-text);
    }
    .top-bar-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    /* Search */
    .search-wrapper {
      position: relative;
      display: flex;
      align-items: center;
      max-width: 100%;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--color-text-muted);
    }
    .search-input {
      height: 40px;
      width: 520px;
      padding: 0 36px 0 38px;
      border: 1.5px solid var(--color-border);
      border-radius: 8px;
      font-family: var(--font-main);
      font-size: 0.85rem;
      background: white;
      color: var(--color-text);
      outline: none;
      transition: border-color 0.15s;
    }
    .search-input:focus { border-color: var(--color-primary); }
    .search-input::placeholder { color: var(--color-text-muted); }
    .search-clear {
      position: absolute;
      right: 8px;
      background: none;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      color: var(--color-text-muted);
      padding: 0;
    }
    .search-clear mat-icon { font-size: 16px; width: 16px; height: 16px; }

    /* Notification btn */
    .btn-notification {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      border: 1.5px solid var(--color-border);
      background: white;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-secondary);
      transition: border-color 0.15s;
    }
    .btn-notification:hover { border-color: var(--color-primary); }
    .btn-notification mat-icon { font-size: 20px; width: 20px; height: 20px; }

    /* Add button */
    .btn-add {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 16px;
      background: var(--color-primary);
      color: white;
      border-radius: 8px;
      font-family: var(--font-main);
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.8px;
      text-decoration: none;
      transition: background 0.15s;
      white-space: nowrap;
    }
    .btn-add mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .btn-add:hover { background: var(--color-primary-light); }

    /* ── Stats + filter row ── */
    .stats-filter-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }

    .stat-block {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    .fleet-ready-label {
      font-size: 0.65rem;
      font-weight: 700;
      letter-spacing: 1.5px;
      color: var(--color-text-secondary);
      text-transform: uppercase;
    }
    .fleet-ready-value { display: flex; align-items: baseline; }
    .fleet-pct {
      font-size: 1.5rem;
      font-weight: 800;
      color: #16A34A;
      line-height: 1;
    }
    .fleet-ready-sub { display: flex; align-items: center; }
    .in-port-chip {
      display: flex;
      align-items: center;
      gap: 4px;
      background: #DCFCE7;
      color: #16A34A;
      border-radius: 20px;
      padding: 3px 10px 3px 6px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .in-port-chip mat-icon { font-size: 14px; width: 14px; height: 14px; }

    .filter-group {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    /* Vessel type select */
    .vessel-type-select {
      position: relative;
      display: flex;
      align-items: center;
    }
    .type-select {
      height: 36px;
      padding: 0 32px 0 12px;
      border: 1.5px solid var(--color-border);
      border-radius: 8px;
      font-family: var(--font-main);
      font-size: 0.83rem;
      color: var(--color-text);
      background: white;
      appearance: none;
      cursor: pointer;
      outline: none;
    }
    .select-arrow {
      position: absolute;
      right: 8px;
      pointer-events: none;
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--color-text-muted);
    }

    /* Status tabs */
    .status-tabs {
      display: flex;
      border: 1.5px solid var(--color-border);
      border-radius: 8px;
      overflow: hidden;
      background: white;
    }
    .status-tab {
      height: 36px;
      padding: 0 14px;
      border: none;
      background: none;
      font-family: var(--font-main);
      font-size: 0.8rem;
      font-weight: 500;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
      border-right: 1px solid var(--color-border);
    }
    .status-tab:last-child { border-right: none; }
    .status-tab:hover { background: #F8FAFC; }
    .status-tab.active {
      background: var(--color-primary);
      color: white;
    }
    .status-tab.active-tab.active { background: #16A34A; }
    .status-tab.maint-tab.active { background: #D97706; }
    .status-tab.dock-tab.active { background: #2563EB; }

    /* ── Loading bar ── */
    .top-loader {
      border-radius: 4px;
      height: 3px;
    }

    /* ── State cards ── */
    .state-card {
      background: white;
      border-radius: var(--radius);
      padding: 64px 32px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      box-shadow: var(--shadow-sm);
    }
    .state-icon {
      font-size: 56px;
      width: 56px;
      height: 56px;
      color: var(--color-border);
    }
    .error-icon { color: var(--color-warn); }
    .state-card h3 { font-size: 1.15rem; color: var(--color-text); }
    .state-card p { color: var(--color-text-secondary); font-size: 0.9rem; }
    .btn-retry {
      margin-top: 8px;
      height: 38px;
      padding: 0 20px;
      background: var(--color-primary);
      color: white;
      border: none;
      border-radius: 8px;
      font-family: var(--font-main);
      font-weight: 600;
      cursor: pointer;
    }
    .btn-add-inline {
      margin-top: 8px;
      display: inline-flex;
      height: 38px;
      padding: 0 20px;
      align-items: center;
      background: var(--color-primary);
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-weight: 600;
      font-size: 0.875rem;
    }

    /* ── Vessel grid ── */
    .vessel-grid {
      display: grid;
      gap: 20px;
    }

    /* ── Vessel card ── */
    .vessel-card {
      background: white;
      border-radius: var(--radius);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: box-shadow 0.2s, transform 0.2s;
    }
    .vessel-card:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-2px);
    }

    /* Card image */
    .card-image {
      height: 180px;
      overflow: hidden;
      flex-shrink: 0;
    }
    .image-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .image-placeholder mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      opacity: 0.5;
      color: white;
    }
    .img-0 { background: linear-gradient(135deg, #1B3A6B 0%, #2A5298 100%); }
    .img-1 { background: linear-gradient(135deg, #0D4F3C 0%, #16A34A 100%); }
    .img-2 { background: linear-gradient(135deg, #44237A 0%, #7C3AED 100%); }
    .img-3 { background: linear-gradient(135deg, #7A2D00 0%, #EA580C 100%); }

    /* Status badge */
    .status-badge {
      font-size: 0.62rem;
      font-weight: 700;
      letter-spacing: 0.8px;
      text-transform: uppercase;
      padding: 3px 9px;
      border-radius: 20px;
      white-space: nowrap;
      flex-shrink: 0;
    }
    .badge-active {
      background: var(--color-status-active-bg);
      color: var(--color-status-active);
    }
    .badge-maintenance {
      background: var(--color-status-maintenance-bg);
      color: var(--color-status-maintenance);
    }
    .badge-port {
      background: var(--color-status-port-bg);
      color: var(--color-status-port);
    }

    /* Card body */
    .card-body {
      padding: 16px 18px 18px;
      display: flex;
      flex-direction: column;
      gap: 0;
      flex: 1;
    }

    /* Title row: name + badge */
    .card-title-row {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 6px;
    }
    .vessel-name {
      font-size: 1.05rem;
      font-weight: 700;
      color: var(--color-text);
      line-height: 1.3;
    }

    /* Subtitle / description */
    .vessel-desc {
      font-size: 0.78rem;
      color: var(--color-text-secondary);
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
      margin-bottom: 14px;
    }

    /* Divider */
    .card-divider {
      height: 1px;
      background: var(--color-border);
      margin-bottom: 14px;
    }

    /* Meta block */
    .meta-grid {
      display: flex;
      gap: 24px;
      margin-bottom: 16px;
    }
    .meta-col {
      display: flex;
      flex-direction: column;
      gap: 3px;
    }
    .meta-label {
      font-size: 0.62rem;
      font-weight: 700;
      letter-spacing: 1.2px;
      text-transform: uppercase;
      color: var(--color-text-muted);
    }
    .meta-value {
      font-size: 0.82rem;
      font-weight: 600;
      color: var(--color-text-secondary);
    }

    /* Card actions */
    .card-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 6px;
      margin-top: auto;
    }

    .card-btn-group {
      display: flex;
      gap: 4px;
      flex-shrink: 0;
    }
    .card-icon-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      border-radius: 6px;
      border: none;
      background: #16A34A;
      color: white;
      cursor: pointer;
      text-decoration: none;
      transition: background 0.15s, transform 0.1s;
      flex-shrink: 0;
    }
    .card-icon-btn mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .card-icon-btn:hover { background: #15803d; transform: translateY(-1px); }

    .card-icon-btn-danger {
      background: var(--color-warn);
    }
    .card-icon-btn-danger:hover { background: #dc2626; transform: translateY(-1px); }

    .card-icon-btn-view {
      background: #2563EB;
    }
    .card-icon-btn-view:hover { background: #1d4ed8; transform: translateY(-1px); }

    /* ── List view ── */
    .vessel-grid--list {
      gap: 6px;
    }

    .vessel-card--list {
      flex-direction: row;
      align-items: center;
      min-height: 68px;
      border-radius: 8px;
    }

    /* Colored strip */
    .list-strip {
      width: 60px;
      flex-shrink: 0;
      align-self: stretch;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius) 0 0 var(--radius);
    }
    .list-strip mat-icon {
      font-size: 22px;
      width: 22px;
      height: 22px;
      opacity: 0.55;
      color: white;
    }

    /* Name + description column */
    .list-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 4px;
      padding: 12px 24px 12px 20px;
    }
    .list-info .vessel-name {
      font-size: 0.9rem;
      font-weight: 700;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      color: var(--color-text);
    }
    .list-info .vessel-desc {
      font-size: 0.76rem;
      color: var(--color-text-secondary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: block;
    }

    /* Status badge column */
    .list-status {
      flex-shrink: 0;
      width: 110px;
      text-align: center;
    }

    /* Date column */
    .list-date {
      flex-shrink: 0;
      width: 130px;
      margin-left: 24px;
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 0.76rem;
      color: var(--color-text-muted);
      white-space: nowrap;
    }
    .list-date mat-icon {
      font-size: 13px;
      width: 13px;
      height: 13px;
    }

    /* Actions column */
    .list-actions {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 0 20px 0 16px;
    }
    .list-actions .card-icon-btn {
      width: 30px;
      height: 30px;
    }

    /* ── Pagination ── */
    .pagination-bar {
      background: white;
      border-radius: var(--radius);
      padding: 0 8px;
      box-shadow: var(--shadow-sm);
    }

    /* ── Responsive ── */
    @media (max-width: 767px) {
      .fleet-page {
        padding: 16px 12px;
        gap: 14px;
      }
      .top-bar {
        flex-direction: column;
        align-items: stretch;
      }
      .top-bar-right {
        flex-direction: column;
        align-items: stretch;
      }
      .search-wrapper {
        width: 100%;
      }
      .search-input {
        width: 100%;
        box-sizing: border-box;
      }
      .btn-add {
        justify-content: center;
      }
      .stats-filter-row {
        flex-direction: column;
        align-items: stretch;
      }
      .filter-group {
        flex-direction: column;
        align-items: stretch;
      }
      .vessel-type-select,
      .type-select {
        width: 100%;
      }
      .status-tabs {
        width: 100%;
        justify-content: stretch;
      }
      .status-tab {
        flex: 1;
        padding: 0 6px;
        font-size: 0.75rem;
      }
      .vessel-grid {
        gap: 12px;
      }
      .card-body {
        padding: 10px 10px 12px;
        gap: 6px;
      }
      .card-btn {
        height: 28px;
        font-size: 0.62rem;
        gap: 3px;
        letter-spacing: 0.4px;
      }
      .card-btn mat-icon { font-size: 12px; width: 12px; height: 12px; }
      .card-icon-btn {
        width: 28px;
        height: 28px;
      }
      .card-icon-btn mat-icon { font-size: 14px; width: 14px; height: 14px; }
      .card-btn-group { gap: 3px; }
      .card-actions { gap: 4px; }
      .pagination-bar {
        padding: 0 4px;
      }
      ::ng-deep .pagination-bar .mat-mdc-paginator-page-size {
        display: none;
      }
      ::ng-deep .pagination-bar .mat-mdc-paginator-range-label {
        font-size: 0.75rem;
        margin: 0 4px;
      }
      ::ng-deep .pagination-bar .mat-mdc-icon-button {
        width: 32px;
        height: 32px;
        padding: 4px;
      }
    }
  `]
})
export class BoatListComponent implements OnInit, OnDestroy {
  boats: Boat[] = [];
  loading = true;
  error = '';
  totalElements = 0;
  currentPage = 0;
  pageSize = Number(localStorage.getItem('fleet_pageSize') ?? '8');
  sortBy = 'createdAt';
  sortDir = 'desc';
  statusFilter: StatusFilter = 'active';
  fleetReadyPct = 84;
  maxCols = 2;

  searchControl = new FormControl('');
  private destroy$ = new Subject<void>();
  private refresh$ = new BehaviorSubject<void>(undefined);
  private authListener = () => this.loadPage();

  constructor(
    private boatService: BoatService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  @HostListener('window:resize')
  onResize(): void {
    this.maxCols = window.innerWidth < 768 ? 1 : 2;
  }

  ngOnInit(): void {
    this.onResize();
    combineLatest([
      this.searchControl.valueChanges.pipe(
        startWith(''),
        debounceTime(300),
        distinctUntilChanged()
      ),
      this.refresh$
    ]).pipe(
      takeUntil(this.destroy$),
      switchMap(([search]) => {
        this.loading = true;
        this.error = '';
        this.currentPage = 0;
        try { this.cdr.detectChanges(); } catch {}
        return this.boatService.getBoats(search ?? '', this.currentPage, this.pageSize, this.sortBy, this.sortDir).pipe(
          catchError(err => {
            this.handleError(err);
            return EMPTY;
          })
        );
      })
    ).subscribe({
      next: (page) => this.handlePage(page)
    });

    this.loadPage();
    window.addEventListener('auth:token_received', this.authListener);
  }

  refresh(): void {
    this.refresh$.next();
  }

  setStatusFilter(filter: StatusFilter): void {
    this.statusFilter = filter;
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    localStorage.setItem('fleet_pageSize', String(this.pageSize));
    this.loadPage();
  }

  onSort(sort: Sort): void {
    this.sortBy = sort.active || 'createdAt';
    this.sortDir = sort.direction || 'desc';
    this.loadPage();
  }

  getStatusLabel(id: number): string {
    const idx = id % 3;
    if (idx === 0) return 'ACTIVE';
    if (idx === 1) return 'MAINTENANCE';
    return 'IN PORT';
  }

  getStatusClass(id: number): string {
    const idx = id % 3;
    if (idx === 0) return 'badge-active';
    if (idx === 1) return 'badge-maintenance';
    return 'badge-port';
  }

  get isListView(): boolean {
    return this.pageSize >= 16;
  }

  get gridCols(): string {
    if (this.maxCols === 1 || this.isListView) return 'repeat(1, 1fr)';
    return `repeat(${this.pageSize / 2}, 1fr)`;
  }

  private loadPage(): void {
    this.loading = true;
    try { this.cdr.detectChanges(); } catch {}
    this.boatService.getBoats(
      this.searchControl.value ?? '',
      this.currentPage,
      this.pageSize,
      this.sortBy,
      this.sortDir
    ).subscribe({
      next: (page) => this.handlePage(page),
      error: (err) => {
        this.handleError(err);
        this.loading = false;
      }
    });
  }

  private handlePage(page: BoatPage): void {
    this.boats = page.content;
    this.totalElements = page.totalElements;
    this.loading = false;
    try { this.cdr.detectChanges(); } catch {}
  }

  private handleError(err: any): void {
    this.error = err?.error?.detail || 'An error occurred while loading vessels.';
    this.loading = false;
    try { this.cdr.detectChanges(); } catch {}
  }

  confirmDelete(boat: Boat): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Vessel',
        message: `Are you sure you want to delete "${boat.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        dangerous: true
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.boatService.deleteBoat(boat.id).subscribe({
          next: () => {
            this.snackBar.open(`"${boat.name}" deleted successfully`, 'Close', {
              duration: 3000,
              panelClass: 'success-snackbar'
            });
            this.refresh();
          },
          error: () => {
            this.snackBar.open('Failed to delete vessel', 'Close', {
              duration: 3000,
              panelClass: 'error-snackbar'
            });
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener('auth:token_received', this.authListener);
  }
}
