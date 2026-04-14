import { Component, OnInit, OnDestroy, HostListener, ChangeDetectorRef } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, BehaviorSubject, EMPTY, switchMap, takeUntil, debounceTime, distinctUntilChanged, catchError } from 'rxjs';
import { BoatService, Boat, BoatPage, BoatStatus, BoatType } from '../../shared/services/boat.service';
import { AuthService } from '../../shared/services/auth.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';
import { StatusLabelPipe } from '../../shared/pipes/status-label.pipe';
import { StatusClassPipe } from '../../shared/pipes/status-class.pipe';

type StatusFilter = 'all' | BoatStatus;
type TypeFilter   = 'all' | BoatType;

interface ListState {
  search:  string;
  status:  StatusFilter;
  type:    TypeFilter;
  page:    number;
  size:    number;
  sortBy:  string;
  sortDir: string;
}

@Component({
  selector: 'app-boat-list',
  standalone: true,
  imports: [
    NgClass,
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatSortModule,
    MatProgressBarModule,
    StatusLabelPipe,
    StatusClassPipe
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
            <mat-icon class="search-icon" aria-hidden="true">search</mat-icon>
            <input
              class="search-input"
              [formControl]="searchControl"
              placeholder="Search by name or description..."
              aria-label="Search vessels by name or description"
              type="search" />
            @if (searchControl.value) {
              <button type="button" class="search-clear" (click)="searchControl.setValue('')" aria-label="Clear search">
                <mat-icon aria-hidden="true">close</mat-icon>
              </button>
            }
          </div>
          <a class="btn-add" routerLink="/boats/new" aria-label="Add new boat">
            <mat-icon aria-hidden="true">add</mat-icon>
            ADD NEW BOAT
          </a>
        </div>
      </div>

      <!-- Stats + filters row -->
      <div class="stats-filter-row">
        <div class="stat-block">
          <span class="fleet-ready-label">FLEET:</span>
          <span class="fleet-total-chip">
            <mat-icon aria-hidden="true">directions_boat</mat-icon>
            {{ totalElements }} Vessels
          </span>
        </div>

        <div class="filter-group">
          <div class="vessel-type-select">
            <label for="type-filter" class="sr-only">Filter by vessel type</label>
            <select id="type-filter" class="type-select" (change)="onTypeChange($event)" aria-label="Filter by vessel type">
              <option value="all"       [selected]="typeFilter === 'all'">All Vessel Types</option>
              <option value="SAILBOAT"  [selected]="typeFilter === 'SAILBOAT'">Sailboat</option>
              <option value="TRAWLER"   [selected]="typeFilter === 'TRAWLER'">Trawler</option>
              <option value="CARGO_SHIP"[selected]="typeFilter === 'CARGO_SHIP'">Cargo Ship</option>
              <option value="YACHT"     [selected]="typeFilter === 'YACHT'">Yacht</option>
              <option value="FERRY"     [selected]="typeFilter === 'FERRY'">Ferry</option>
            </select>
            <mat-icon class="select-arrow" aria-hidden="true">expand_more</mat-icon>
          </div>

          <div class="status-tabs" role="group" aria-label="Filter by vessel status">
            <button type="button" class="status-tab" [class.active]="statusFilter === 'all'" (click)="setStatusFilter('all')" [attr.aria-pressed]="statusFilter === 'all'">All</button>
            <button type="button" class="status-tab active-tab" [class.active]="statusFilter === 'UNDERWAY'" (click)="setStatusFilter('UNDERWAY')" [attr.aria-pressed]="statusFilter === 'UNDERWAY'">Underway</button>
            <button type="button" class="status-tab port-tab" [class.active]="statusFilter === 'IN_PORT'" (click)="setStatusFilter('IN_PORT')" [attr.aria-pressed]="statusFilter === 'IN_PORT'">In Port</button>
            <button type="button" class="status-tab maint-tab" [class.active]="statusFilter === 'MAINTENANCE'" (click)="setStatusFilter('MAINTENANCE')" [attr.aria-pressed]="statusFilter === 'MAINTENANCE'">Maintenance</button>
          </div>
        </div>
      </div>

      <!-- Loading bar -->
      @if (loading) {
        <mat-progress-bar mode="indeterminate" class="top-loader" />
      }

      <!-- Error state -->
      @if (error && !loading) {
        <div class="state-card" role="alert">
          <mat-icon class="state-icon error-icon" aria-hidden="true">error_outline</mat-icon>
          <h3>Failed to load vessels</h3>
          <p>{{ error }}</p>
          <button type="button" class="btn-retry" (click)="refresh()">Try Again</button>
        </div>
      }

      <!-- Empty state -->
      @if (!loading && !error && boats.length === 0) {
        <div class="state-card">
          <mat-icon class="state-icon" aria-hidden="true">sailing</mat-icon>
          <h3>No vessels found</h3>
          @if (searchControl.value) {
            <p>No results for "{{ searchControl.value }}"</p>
          } @else {
            <p>Your fleet is empty. Add your first vessel!</p>
          }
          <a class="btn-add-inline" routerLink="/boats/new">Add First Vessel</a>
        </div>
      }

      <!-- Card grid -->
      @if (!error && boats.length > 0) {
        <div class="vessel-grid"
             [style.gridTemplateColumns]="gridCols"
             [class.vessel-grid--list]="isListView"
             [class.vessel-grid--compact]="isCompactView">
          @for (boat of boats; track boat.id) {
            <div class="vessel-card"
                 [routerLink]="['/boats', boat.id]"
                 [class.vessel-card--list]="isListView"
                 role="link"
                 tabindex="0"
                 [attr.aria-label]="boat.name + ' — view vessel details'"
                 (keydown.enter)="navigateToBoat(boat.id)"
                 (keydown.space)="$event.preventDefault(); navigateToBoat(boat.id)">

              <!-- ── Card view (4 / 8 items per page) ── -->
              @if (!isListView) {
                <div class="card-image">
                  <div class="image-placeholder" [ngClass]="'img-' + (boat.id % 4)">
                    <mat-icon aria-hidden="true">directions_boat</mat-icon>
                  </div>
                </div>
                <div class="card-body">
                  <div class="card-title-row">
                    <h3 class="vessel-name">{{ boat.name }}</h3>
                    <span class="status-badge" [ngClass]="boat.status | statusClass">
                      {{ boat.status | statusLabel }}
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
                      <a class="card-icon-btn card-icon-btn-view" [routerLink]="['/boats', boat.id]" matTooltip="View vessel" [attr.aria-label]="'View ' + boat.name">
                        <mat-icon aria-hidden="true">visibility</mat-icon>
                      </a>
                      <a class="card-icon-btn" [routerLink]="['/boats', boat.id, 'edit']" matTooltip="Edit vessel" [attr.aria-label]="'Edit ' + boat.name">
                        <mat-icon aria-hidden="true">edit</mat-icon>
                      </a>
                      <button type="button" class="card-icon-btn card-icon-btn-danger" (click)="confirmDelete(boat)" matTooltip="Delete vessel" [attr.aria-label]="'Delete ' + boat.name">
                        <mat-icon aria-hidden="true">delete_outline</mat-icon>
                      </button>
                    </div>
                  </div>
                </div>
              }

              <!-- ── List view (16 / 32 items per page) ── -->
              @if (isListView) {
                <div class="list-strip" [ngClass]="'img-' + (boat.id % 4)">
                  <mat-icon aria-hidden="true">directions_boat</mat-icon>
                </div>
                <div class="list-info">
                  <h3 class="vessel-name">{{ boat.name }}</h3>
                  <p class="vessel-desc">{{ boat.description || 'No description provided.' }}</p>
                </div>
                <span class="status-badge list-status" [ngClass]="boat.status | statusClass">
                  {{ boat.status | statusLabel }}
                </span>
                <span class="list-date">
                  <mat-icon aria-hidden="true">calendar_today</mat-icon>
                  {{ boat.createdAt | date:'MMM d, y' }}
                </span>
                <div class="list-actions" (click)="$event.stopPropagation()">
                  <a class="card-icon-btn card-icon-btn-view" [routerLink]="['/boats', boat.id]" matTooltip="View vessel" [attr.aria-label]="'View ' + boat.name">
                    <mat-icon aria-hidden="true">visibility</mat-icon>
                  </a>
                  <a class="card-icon-btn" [routerLink]="['/boats', boat.id, 'edit']" matTooltip="Edit vessel" [attr.aria-label]="'Edit ' + boat.name">
                    <mat-icon aria-hidden="true">edit</mat-icon>
                  </a>
                  <button type="button" class="card-icon-btn card-icon-btn-danger" (click)="confirmDelete(boat)" matTooltip="Delete vessel" [attr.aria-label]="'Delete ' + boat.name">
                    <mat-icon aria-hidden="true">delete_outline</mat-icon>
                  </button>
                </div>
              }

            </div>
          }
        </div>
      }

      <!-- Pagination -->
      @if (!error && totalElements > 0) {
        <div class="pagination-bar">
          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageSizeOptions]="[4, 8, 16, 32]"
            [pageIndex]="currentPage"
            (page)="onPageChange($event)"
            showFirstLastButtons>
          </mat-paginator>
        </div>
      }

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
      background: var(--color-surface);
      color: var(--color-text);
      outline: none;
      transition: border-color 0.15s;
    }
    .search-input:focus {
      border-color: var(--color-primary);
      outline: 3px solid var(--color-focus-ring);
      outline-offset: 2px;
    }
    .search-input::placeholder { color: var(--color-text-muted); }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0,0,0,0);
      white-space: nowrap;
      border: 0;
    }
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
      gap: 8px;
    }
    .fleet-ready-label {
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 1.5px;
      color: var(--color-text-secondary);
      text-transform: uppercase;
    }

    .fleet-total-chip {
      display: flex;
      align-items: center;
      gap: 4px;
      background: var(--color-status-port-bg);
      color: var(--color-status-port);
      border-radius: 20px;
      padding: 3px 10px 3px 6px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .fleet-total-chip mat-icon { font-size: 14px; width: 14px; height: 14px; }

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
      background: var(--color-surface);
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
      background: var(--color-surface);
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
    .status-tab:hover { background: var(--color-surface-hover); }
    .status-tab.active {
      background: var(--color-primary);
      color: white;
    }
    .status-tab.active-tab.active { background: var(--color-btn-green); }
    .status-tab.port-tab.active   { background: var(--color-btn-blue); }
    .status-tab.maint-tab.active  { background: #B45309; }

    /* ── Loading bar ── */
    .top-loader {
      border-radius: 4px;
      height: 3px;
    }

    /* ── State cards ── */
    .state-card {
      background: var(--color-surface);
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
      background: var(--color-surface);
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
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
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
      background: var(--color-btn-green);
      color: white;
      cursor: pointer;
      text-decoration: none;
      transition: background 0.15s, transform 0.1s;
      flex-shrink: 0;
    }
    .card-icon-btn mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .card-icon-btn:hover { background: var(--color-btn-green-hover); transform: translateY(-1px); }

    .card-icon-btn-danger {
      background: var(--color-btn-red);
    }
    .card-icon-btn-danger:hover { background: var(--color-btn-red-hover); transform: translateY(-1px); }

    .card-icon-btn-view {
      background: var(--color-btn-blue);
    }
    .card-icon-btn-view:hover { background: var(--color-btn-blue-hover); transform: translateY(-1px); }

    /* ── Compact view (4 × 2 grid, page size 8) ── */
    .vessel-grid--compact {
      gap: 14px;
    }

    .vessel-grid--compact .card-image {
      height: 120px;
    }
    .vessel-grid--compact .image-placeholder mat-icon {
      font-size: 44px;
      width: 44px;
      height: 44px;
    }
    .vessel-grid--compact .card-body {
      padding: 12px 14px 14px;
    }
    .vessel-grid--compact .vessel-name {
      font-size: 0.88rem;
    }
    .vessel-grid--compact .vessel-desc {
      font-size: 0.74rem;
      margin-bottom: 10px;
    }
    .vessel-grid--compact .meta-label {
      font-size: 0.58rem;
    }
    .vessel-grid--compact .meta-value {
      font-size: 0.76rem;
    }
    .vessel-grid--compact .meta-grid {
      margin-bottom: 10px;
    }
    .vessel-grid--compact .card-icon-btn {
      width: 28px;
      height: 28px;
    }
    .vessel-grid--compact .card-icon-btn mat-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }
    .vessel-grid--compact .status-badge {
      font-size: 0.58rem;
      padding: 2px 7px;
    }

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
      background: var(--color-surface);
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
    }
  `]
})
export class BoatListComponent implements OnInit, OnDestroy {
  boats: Boat[] = [];
  loading = true;
  error = '';
  totalElements = 0;
  maxCols = 2;

  searchControl = new FormControl('');

  private destroy$ = new Subject<void>();
  private state$!: BehaviorSubject<ListState>;

  constructor(
    private boatService: BoatService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  navigateToBoat(id: number): void {
    this.router.navigate(['/boats', id]);
  }

  @HostListener('window:resize')
  onResize(): void {
    this.maxCols = window.innerWidth < 768 ? 1 : 2;
  }

  ngOnInit(): void {
    this.onResize();

    this.state$ = new BehaviorSubject<ListState>({
      search:  '',
      status:  'all',
      type:    'all',
      page:    0,
      size:    Number(localStorage.getItem('fleet_pageSize') ?? '8'),
      sortBy:  'createdAt',
      sortDir: 'desc'
    });

    // Debounce search and reset page to 0 on new query
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(search => {
      this.state$.next({ ...this.state$.value, search: search ?? '', page: 0 });
    });

    // Single pipeline — reacts to every state change
    this.state$.pipe(
      takeUntil(this.destroy$),
      switchMap(state => {
        this.loading = true;
        this.error   = '';
        return this.boatService.getBoats(
          state.search, state.page, state.size, state.sortBy, state.sortDir,
          state.status === 'all' ? '' : state.status,
          state.type   === 'all' ? '' : state.type
        ).pipe(catchError(err => { this.handleError(err); return EMPTY; }));
      })
    ).subscribe({ next: page => this.handlePage(page) });

    // Refresh data on token renewal without duplicating the load logic
    this.authService.onTokenReceived$.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.refresh());
  }

  // ── Getters for template ──────────────────────────────────────────────────

  get statusFilter(): StatusFilter { return this.state$.value.status; }
  get typeFilter():   TypeFilter   { return this.state$.value.type; }
  get currentPage():  number       { return this.state$.value.page; }
  get pageSize():     number       { return this.state$.value.size; }

  get isListView(): boolean {
    return this.state$.value.size >= 16;
  }

  /** 4-column compact grid — only on desktop when page size is 8 */
  get isCompactView(): boolean {
    return this.state$.value.size === 8 && this.maxCols > 1;
  }

  get gridCols(): string {
    if (this.maxCols === 1 || this.isListView) return 'repeat(1, 1fr)';
    if (this.isCompactView) return 'repeat(4, 1fr)';
    return 'repeat(2, 1fr)'; // size = 4 → 2×2
  }

  // ── State mutators ────────────────────────────────────────────────────────

  refresh(): void {
    this.state$.next({ ...this.state$.value }); // new object reference re-triggers switchMap
  }

  setStatusFilter(filter: StatusFilter): void {
    this.state$.next({ ...this.state$.value, status: filter, page: 0 });
  }

  onTypeChange(event: Event): void {
    const type = (event.target as HTMLSelectElement).value as TypeFilter;
    this.state$.next({ ...this.state$.value, type, page: 0 });
  }

  onPageChange(event: PageEvent): void {
    localStorage.setItem('fleet_pageSize', String(event.pageSize));
    this.state$.next({ ...this.state$.value, page: event.pageIndex, size: event.pageSize });
  }

  onSort(sort: Sort): void {
    this.state$.next({
      ...this.state$.value,
      sortBy:  sort.active    || 'createdAt',
      sortDir: sort.direction || 'desc',
      page: 0
    });
  }

  confirmDelete(boat: Boat): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title:       'Delete Vessel',
        message:     `Are you sure you want to delete "${boat.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText:  'Cancel',
        dangerous:   true
      }
    });

    dialogRef.afterClosed().pipe(takeUntil(this.destroy$)).subscribe(confirmed => {
      if (!confirmed) return;
      this.boatService.deleteBoat(boat.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.snackBar.open(`"${boat.name}" deleted successfully`, 'Close', {
            duration: 3000, panelClass: 'success-snackbar'
          });
          this.refresh();
        },
        error: () => {
          this.snackBar.open('Failed to delete vessel', 'Close', {
            duration: 3000, panelClass: 'error-snackbar'
          });
        }
      });
    });
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private handlePage(page: BoatPage): void {
    this.boats         = page.content;
    this.totalElements = page.totalElements;
    this.loading       = false;
    this.cdr.detectChanges();
  }

  private handleError(err: any): void {
    this.error   = err?.error?.detail || 'An error occurred while loading vessels.';
    this.loading = false;
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
