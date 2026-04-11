import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, switchMap, startWith, combineLatest, BehaviorSubject, EMPTY, catchError } from 'rxjs';
import { BoatService, Boat, BoatPage } from '../../shared/services/boat.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';


@Component({
  selector: 'app-boat-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    MatSortModule,
    MatChipsModule,
    MatProgressBarModule
  ],
  template: `
    <div class="page-container">
      <!-- Header -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">
            <mat-icon>directions_boat</mat-icon>
            Fleet
          </h1>
          <span class="boat-count" *ngIf="totalElements > 0">{{ totalElements }} vessels</span>
        </div>
        <a mat-raised-button color="primary" routerLink="/boats/new" class="add-btn">
          <mat-icon>add</mat-icon>
          Add Boat
        </a>
      </div>

      <!-- Search — always visible -->
      <div class="search-bar card">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search by name or description</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput [formControl]="searchControl" placeholder="e.g. Sea Explorer..." />
          <button mat-icon-button matSuffix *ngIf="searchControl.value" (click)="searchControl.setValue('')">
            <mat-icon>close</mat-icon>
          </button>
        </mat-form-field>
      </div>

      <!-- Initial loading state (before first data arrives, no table to show progress bar in) -->
      <mat-progress-bar *ngIf="loading && boats.length === 0" mode="indeterminate" />

      <!-- Error state -->
      <div *ngIf="error && !loading" class="state-card error-state card">
        <mat-icon>error_outline</mat-icon>
        <h3>Failed to load boats</h3>
        <p>{{ error }}</p>
        <button mat-raised-button color="primary" (click)="refresh()">Try Again</button>
      </div>

      <!-- Empty state (only when not loading and no error) -->
      <div *ngIf="!loading && !error && boats.length === 0" class="state-card empty-state card">
        <mat-icon>sailing</mat-icon>
        <h3>No boats found</h3>
        <p *ngIf="searchControl.value">No results for "{{ searchControl.value }}"</p>
        <p *ngIf="!searchControl.value">Your fleet is empty. Add your first boat!</p>
        <a mat-raised-button color="primary" routerLink="/boats/new">Add First Boat</a>
      </div>

      <!-- Table — shown as soon as we have data; progress bar replaces blocking spinner -->
      <div class="table-card card" *ngIf="!error && boats.length > 0">
        <mat-progress-bar *ngIf="loading" mode="indeterminate" class="table-progress" />
        <table mat-table [dataSource]="boats" matSort (matSortChange)="onSort($event)" class="boats-table">

          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
            <td mat-cell *matCellDef="let boat">
              <span class="boat-name">{{ boat.name }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="description">
            <th mat-header-cell *matHeaderCellDef>Description</th>
            <td mat-cell *matCellDef="let boat">
              <span class="description-text">{{ boat.description || '—' }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Created</th>
            <td mat-cell *matCellDef="let boat">
              {{ boat.createdAt | date:'mediumDate' }}
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let boat" (click)="$event.stopPropagation()">
              <button mat-icon-button [routerLink]="['/boats', boat.id]" matTooltip="View details" color="primary">
                <mat-icon>visibility</mat-icon>
              </button>
              <button mat-icon-button [routerLink]="['/boats', boat.id, 'edit']" matTooltip="Edit" color="accent">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button (click)="confirmDelete(boat)" matTooltip="Delete" color="warn">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"
              [routerLink]="['/boats', row.id]"
              class="clickable-row"></tr>
        </table>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[5, 10, 25, 50]"
          [pageIndex]="currentPage"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 12px;
    }
    .header-left {
      display: flex;
      align-items: baseline;
      gap: 12px;
    }
    .page-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: var(--font-mono);
      font-size: 1.8rem;
      color: var(--color-primary);
    }
    .boat-count {
      color: var(--color-text-secondary);
      font-size: 0.95rem;
      background: var(--color-border);
      padding: 2px 10px;
      border-radius: 20px;
    }
    .add-btn {
      background: var(--color-primary) !important;
      color: white !important;
      border-radius: 8px !important;
    }
    .search-bar {
      margin-bottom: 20px;
      padding: 16px 20px;
    }
    .search-field { width: 100%; }
    .table-card { padding: 0; overflow: hidden; }
    .boats-table { width: 100%; }
    .boat-name {
      font-weight: 600;
      color: var(--color-primary);
    }
    .description-text {
      color: var(--color-text-secondary);
      font-size: 0.9rem;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      max-width: 320px;
    }
    .clickable-row { cursor: pointer; }
    .state-card {
      text-align: center;
      padding: 64px 32px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: var(--color-border);
      }
      h3 { font-size: 1.3rem; color: var(--color-text); }
      p { color: var(--color-text-secondary); }
    }
    .error-state mat-icon { color: var(--color-warn); }
    .table-progress { position: absolute; top: 0; left: 0; right: 0; }
    .table-card { position: relative; }
  `]
})
export class BoatListComponent implements OnInit, OnDestroy {
  displayedColumns = ['name', 'description', 'createdAt', 'actions'];
  boats: Boat[] = [];
  loading = true; // true from the start so the spinner shows immediately
  error = '';
  totalElements = 0;
  currentPage = 0;
  pageSize = 10;
  sortBy = 'createdAt';
  sortDir = 'desc';

  searchControl = new FormControl('');
  private destroy$ = new Subject<void>();
  private refresh$ = new BehaviorSubject<void>(undefined);
  private authListener = () => this.loadPage();

  constructor(
    private boatService: BoatService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
    ,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
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
        // catchError INSIDE switchMap keeps the outer chain alive after errors.
        // Without this, the first HTTP failure kills the whole observable and
        // no further search/refresh events ever trigger a new request.
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

    // Ensure an initial load in case the combineLatest chain doesn't emit immediately
    this.loadPage();

    // Reload when auth token becomes available (token exchange completed elsewhere)
    window.addEventListener('auth:token_received', this.authListener);
  }

  refresh(): void {
    this.refresh$.next();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadPage();
  }

  onSort(sort: Sort): void {
    this.sortBy = sort.active || 'createdAt';
    this.sortDir = sort.direction || 'desc';
    this.loadPage();
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
    // assure the table updates immediately
    try { this.cdr.detectChanges(); } catch {}
  }

  private handleError(err: any): void {
    this.error = err?.error?.detail || 'An error occurred while loading boats.';
    this.loading = false;
    try { this.cdr.detectChanges(); } catch {}
  }

  confirmDelete(boat: Boat): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Boat',
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
            this.snackBar.open('Failed to delete boat', 'Close', {
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
