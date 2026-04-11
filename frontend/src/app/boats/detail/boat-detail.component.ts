import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
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
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule,
    MatSnackBarModule,
    LoadingSpinnerComponent
  ],
  template: `
    <div class="page-container">
      <div class="breadcrumb">
        <a routerLink="/boats">Fleet</a>
        <mat-icon>chevron_right</mat-icon>
        <span>{{ boat?.name || 'Loading...' }}</span>
      </div>

      <app-loading-spinner *ngIf="loading" message="Loading boat details..." />

      <div *ngIf="error && !loading" class="card error-card">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error }}</p>
        <a mat-raised-button routerLink="/boats">Back to Fleet</a>
      </div>

      <div *ngIf="boat && !loading" class="detail-layout">
        <mat-card class="detail-card">
          <div class="detail-header">
            <div class="boat-icon-wrapper">
              <mat-icon>directions_boat</mat-icon>
            </div>
            <div class="detail-title">
              <h1>{{ boat.name }}</h1>
              <span class="boat-id">ID #{{ boat.id }}</span>
            </div>
          </div>

          <mat-divider />

          <div class="detail-body">
            <div class="detail-field">
              <span class="field-label">Description</span>
              <p class="field-value">{{ boat.description || 'No description provided.' }}</p>
            </div>

            <div class="detail-field">
              <span class="field-label">Created</span>
              <p class="field-value">{{ boat.createdAt | date:'MMMM d, y, h:mm a' }}</p>
            </div>
          </div>

          <mat-divider />

          <div class="detail-actions">
            <a mat-stroked-button routerLink="/boats">
              <mat-icon>arrow_back</mat-icon>
              Back
            </a>
            <a mat-raised-button color="primary" [routerLink]="['/boats', boat.id, 'edit']">
              <mat-icon>edit</mat-icon>
              Edit
            </a>
            <button mat-raised-button color="warn" (click)="confirmDelete()">
              <mat-icon>delete_outline</mat-icon>
              Delete
            </button>
          </div>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .breadcrumb {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-bottom: 24px;
      color: var(--color-text-secondary);
      font-size: 0.9rem;
      a { color: var(--color-primary); text-decoration: none; }
      a:hover { text-decoration: underline; }
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .detail-card { padding: 32px; max-width: 700px; }
    .detail-header {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 24px;
    }
    .boat-icon-wrapper {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
      display: flex;
      align-items: center;
      justify-content: center;
      mat-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
        color: white;
      }
    }
    .detail-title h1 {
      font-family: var(--font-mono);
      font-size: 1.6rem;
      color: var(--color-primary);
    }
    .boat-id {
      color: var(--color-text-secondary);
      font-size: 0.85rem;
    }
    .detail-body {
      padding: 24px 0;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .detail-field {}
    .field-label {
      display: block;
      font-size: 0.78rem;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: var(--color-text-secondary);
      font-weight: 600;
      margin-bottom: 6px;
    }
    .field-value {
      font-size: 1rem;
      color: var(--color-text);
      line-height: 1.6;
    }
    .detail-actions {
      display: flex;
      gap: 12px;
      padding-top: 24px;
      flex-wrap: wrap;
    }
    .error-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      padding: 48px;
      text-align: center;
      mat-icon { font-size: 48px; width: 48px; height: 48px; color: var(--color-warn); }
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
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private boatService: BoatService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
    ,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadBoat(id);
    window.addEventListener('auth:token_received', this.authListener);
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
        this.error = err.status === 404
          ? 'Boat not found.'
          : 'Failed to load boat details.';
        this.loading = false;
        try { this.cdr.detectChanges(); } catch {}
      }
    });
  }

  confirmDelete(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Boat',
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
            this.snackBar.open('Boat deleted successfully', 'Close', {
              duration: 3000,
              panelClass: 'success-snackbar'
            });
            this.router.navigate(['/boats']);
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
    window.removeEventListener('auth:token_received', this.authListener);
  }
}
