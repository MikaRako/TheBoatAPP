import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BoatService, Boat, BoatStatus, BoatType } from '../../shared/services/boat.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';

@Component({
  selector: 'app-boat-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="detail-page">

      <!-- Breadcrumb -->
      <div class="breadcrumb">
        <a routerLink="/boats" class="breadcrumb-link">
          <mat-icon>arrow_back</mat-icon>
          Back to Fleet
        </a>
      </div>

      <app-loading-spinner *ngIf="loading" message="Loading vessel details..." />

      <!-- Error -->
      <div *ngIf="error && !loading" class="state-card">
        <mat-icon class="state-icon error-icon">error_outline</mat-icon>
        <h3>{{ error }}</h3>
        <a class="btn-secondary" routerLink="/boats">Back to Fleet</a>
      </div>

      <div *ngIf="boat && !loading" class="detail-layout">

        <!-- Hero card -->
        <div class="hero-card" [ngClass]="'hero-' + (boat.id % 4)">
          <div class="hero-watermark">
            <mat-icon>{{ getTypeIcon(boat.type) }}</mat-icon>
          </div>
          <div class="hero-top">
            <div class="hero-badges">
              <span class="status-badge" [ngClass]="getStatusClass(boat.status)">
                {{ getStatusLabel(boat.status) }}
              </span>
              <span class="type-badge">{{ getTypeLabel(boat.type) }}</span>
            </div>
          </div>
          <div class="hero-bottom">
            <h1 class="hero-name">{{ boat.name }}</h1>
            <span class="hero-id">Vessel ID #{{ boat.id }}</span>
          </div>
        </div>

        <!-- Action bar -->
        <div class="action-bar">
          <div class="action-date">
            <mat-icon>schedule</mat-icon>
            Added {{ boat.createdAt | date:'MMM d, y' }}&nbsp;&middot;&nbsp;{{ boat.createdAt | date:'h:mm a' }}
          </div>
          <div class="action-buttons">
            <a class="btn-edit" [routerLink]="['/boats', boat.id, 'edit']">
              <mat-icon>edit</mat-icon>
              Edit Vessel
            </a>
            <button class="btn-delete" (click)="confirmDelete()">
              <mat-icon>delete_outline</mat-icon>
              Delete Vessel
            </button>
          </div>
        </div>

        <!-- Info cards -->
        <div class="info-grid">
          <div class="info-card info-card--full">
            <div class="info-label">
              <mat-icon>description</mat-icon>
              Description
            </div>
            <p class="info-value">{{ boat.description || 'No description provided.' }}</p>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .detail-page {
      max-width: 900px;
      margin: 0 auto;
      padding: 28px 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    /* Breadcrumb */
    .breadcrumb-link {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--color-text-secondary);
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 500;
      transition: color 0.15s;
    }
    .breadcrumb-link mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
    .breadcrumb-link:hover { color: var(--color-primary); }

    /* State card */
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
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--color-border);
    }
    .error-icon { color: var(--color-warn); }
    .state-card h3 { font-size: 1.1rem; color: var(--color-text); }
    .btn-secondary {
      display: inline-flex;
      align-items: center;
      height: 38px;
      padding: 0 18px;
      background: var(--color-primary);
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 600;
      margin-top: 8px;
    }

    /* Hero card */
    .hero-card {
      border-radius: var(--radius);
      padding: 20px 24px;
      min-height: 220px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      position: relative;
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }
    .hero-0 { background: linear-gradient(135deg, #1B3A6B 0%, #2A5298 100%); }
    .hero-1 { background: linear-gradient(135deg, #0D4F3C 0%, #16A34A 100%); }
    .hero-2 { background: linear-gradient(135deg, #44237A 0%, #7C3AED 100%); }
    .hero-3 { background: linear-gradient(135deg, #7A2D00 0%, #EA580C 100%); }

    .hero-watermark {
      position: absolute;
      right: -16px;
      bottom: -16px;
      opacity: 0.12;
      pointer-events: none;
    }
    .hero-watermark mat-icon {
      font-size: 180px;
      width: 180px;
      height: 180px;
      color: white;
    }

    .hero-top {
      display: flex;
      align-items: flex-start;
    }
    .hero-badges {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .hero-bottom {
      display: flex;
      flex-direction: column;
      gap: 2px;
      z-index: 1;
    }
    .hero-name {
      font-size: 1.7rem;
      font-weight: 700;
      color: white;
      line-height: 1.2;
      word-break: break-word;
      overflow-wrap: break-word;
      text-shadow: 0 1px 4px rgba(0,0,0,0.25);
    }
    .hero-id {
      font-size: 0.78rem;
      color: rgba(255,255,255,0.6);
      letter-spacing: 0.3px;
    }

    /* Type badge */
    .type-badge {
      display: inline-block;
      font-size: 0.62rem;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
      padding: 3px 10px;
      border-radius: 20px;
      background: rgba(255,255,255,0.2);
      color: white;
    }

    /* Status badge */
    .status-badge {
      display: inline-block;
      font-size: 0.62rem;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
      padding: 3px 10px;
      border-radius: 20px;
      width: fit-content;
    }
    .badge-active   { background: rgba(220,252,231,0.9); color: #16A34A; }
    .badge-maintenance { background: rgba(254,243,199,0.9); color: #D97706; }
    .badge-port     { background: rgba(219,234,254,0.9); color: #2563EB; }

    /* Action bar */
    .action-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }
    .action-date {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 0.8rem;
      color: var(--color-text-secondary);
      font-weight: 500;
    }
    .action-date mat-icon {
      font-size: 15px;
      width: 15px;
      height: 15px;
    }
    .action-buttons {
      display: flex;
      gap: 12px;
    }
    .btn-edit {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 20px;
      background: #16A34A;
      color: white;
      border-radius: 8px;
      text-decoration: none;
      font-size: 0.83rem;
      font-weight: 600;
      border: none;
      transition: background 0.15s;
    }
    .btn-edit mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .btn-edit:hover { background: #15803d; }

    .btn-delete {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 20px;
      background: var(--color-warn);
      color: white;
      border-radius: 8px;
      border: none;
      font-family: var(--font-main);
      font-size: 0.83rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.15s;
    }
    .btn-delete mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .btn-delete:hover { background: #dc2626; }

    /* Info grid */
    .detail-layout {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }
    .info-card {
      background: white;
      border-radius: var(--radius);
      padding: 20px 22px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .info-card--full {
      grid-column: 1 / -1;
    }
    .info-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: var(--color-text-secondary);
    }
    .info-label mat-icon {
      font-size: 15px;
      width: 15px;
      height: 15px;
    }
    .info-value {
      font-size: 0.95rem;
      color: var(--color-text);
      line-height: 1.6;
      word-break: break-word;
      overflow-wrap: break-word;
      white-space: pre-wrap;
    }
  `]
})
export class BoatDetailComponent implements OnInit, OnDestroy {
  boat: Boat | null = null;
  loading = false;
  error = '';

  private authListener = () => {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.loadBoat(id);
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private boatService: BoatService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadBoat(id);
    window.addEventListener('auth:token_received', this.authListener);
  }

  getStatusLabel(status: BoatStatus): string {
    switch (status) {
      case 'UNDERWAY':    return 'UNDERWAY';
      case 'IN_PORT':     return 'IN PORT';
      case 'MAINTENANCE': return 'MAINTENANCE';
    }
  }

  getStatusClass(status: BoatStatus): string {
    switch (status) {
      case 'UNDERWAY':    return 'badge-active';
      case 'IN_PORT':     return 'badge-port';
      case 'MAINTENANCE': return 'badge-maintenance';
    }
  }

  getTypeLabel(type: BoatType): string {
    switch (type) {
      case 'SAILBOAT':  return 'Sailboat';
      case 'TRAWLER':   return 'Trawler';
      case 'CARGO_SHIP': return 'Cargo Ship';
      case 'YACHT':     return 'Yacht';
      case 'FERRY':     return 'Ferry';
    }
  }

  getTypeIcon(type: BoatType): string {
    switch (type) {
      case 'SAILBOAT':  return 'sailing';
      case 'TRAWLER':   return 'phishing';
      case 'CARGO_SHIP': return 'local_shipping';
      case 'YACHT':     return 'directions_boat';
      case 'FERRY':     return 'directions_ferry';
    }
  }

  private loadBoat(id: number): void {
    this.loading = true;
    try { this.cdr.detectChanges(); } catch {}
    this.boatService.getBoatById(id).subscribe({
      next: (boat) => {
        this.boat = boat;
        this.loading = false;
        try { this.cdr.detectChanges(); } catch {}
      },
      error: (err) => {
        this.error = err.status === 404 ? 'Vessel not found.' : 'Failed to load vessel details.';
        this.loading = false;
        try { this.cdr.detectChanges(); } catch {}
      }
    });
  }

  confirmDelete(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Vessel',
        message: `Are you sure you want to delete "${this.boat?.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        dangerous: true
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && this.boat) {
        this.boatService.deleteBoat(this.boat.id).subscribe({
          next: () => {
            this.snackBar.open('Vessel deleted successfully', 'Close', {
              duration: 3000,
              panelClass: 'success-snackbar'
            });
            this.router.navigate(['/boats']);
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
    window.removeEventListener('auth:token_received', this.authListener);
  }
}
