import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BoatService, Boat } from '../../shared/services/boat.service';
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
          <div class="hero-icon">
            <mat-icon>directions_boat</mat-icon>
          </div>
          <div class="hero-info">
            <span class="status-badge" [ngClass]="getStatusClass(boat.id)">
              {{ getStatusLabel(boat.id) }}
            </span>
            <h1 class="hero-name">{{ boat.name }}</h1>
            <span class="hero-id">Vessel ID #{{ boat.id }}</span>
          </div>
        </div>

        <!-- Action bar -->
        <div class="action-bar">
          <a class="btn-edit" [routerLink]="['/boats', boat.id, 'edit']">
            <mat-icon>edit</mat-icon>
            Edit Vessel
          </a>
          <button class="btn-delete" (click)="confirmDelete()">
            <mat-icon>delete_outline</mat-icon>
            Delete Vessel
          </button>
        </div>

        <!-- Info cards -->
        <div class="info-grid">
          <div class="info-card">
            <div class="info-label">
              <mat-icon>description</mat-icon>
              Description
            </div>
            <p class="info-value">{{ boat.description || 'No description provided.' }}</p>
          </div>

          <div class="info-card">
            <div class="info-label">
              <mat-icon>calendar_today</mat-icon>
              Date Added
            </div>
            <p class="info-value">{{ boat.createdAt | date:'MMMM d, y' }}</p>
            <p class="info-sub">{{ boat.createdAt | date:'h:mm a' }}</p>
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
      padding: 28px 28px 28px 24px;
      display: flex;
      align-items: center;
      gap: 24px;
      box-shadow: var(--shadow-sm);
      flex-wrap: wrap;
    }
    .hero-0 { background: linear-gradient(135deg, #1B3A6B 0%, #2A5298 100%); }
    .hero-1 { background: linear-gradient(135deg, #0D4F3C 0%, #16A34A 100%); }
    .hero-2 { background: linear-gradient(135deg, #44237A 0%, #7C3AED 100%); }
    .hero-3 { background: linear-gradient(135deg, #7A2D00 0%, #EA580C 100%); }

    .hero-icon {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: rgba(255,255,255,0.15);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .hero-icon mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: white;
    }

    .hero-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;
      min-width: 0;
    }
    .hero-name {
      font-size: 1.6rem;
      font-weight: 700;
      color: white;
      line-height: 1.2;
      word-break: break-word;
      overflow-wrap: break-word;
    }
    .hero-id {
      font-size: 0.8rem;
      color: rgba(255,255,255,0.65);
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
      gap: 12px;
      justify-content: flex-end;
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
    .info-sub {
      font-size: 0.8rem;
      color: var(--color-text-muted);
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
